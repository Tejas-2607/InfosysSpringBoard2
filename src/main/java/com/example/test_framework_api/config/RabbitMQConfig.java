package com.example.test_framework_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EXCHANGE      = "testRunExchange";
    public static final String ROUTING_KEY   = "testRunKey";
    public static final String DLQ_ROUTING   = "dlq.testRunKey";

    /* ---------- MESSAGE CONVERTER ---------- */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }

    /* ---------- LISTENER CONTAINER WITH RETRY + DLQ ---------- */
    @Bean
    public SimpleMessageListenerContainer listenerContainer(ConnectionFactory cf,
                                                            RabbitTemplate template) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000);

        // Republish to DLQ when all retries are exhausted
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(template,
                EXCHANGE, DLQ_ROUTING);

        container.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)               // 3 tries (original + 2 retries)
                        .backOffPolicy(backOff)
                        .recoverer(recoverer)         // <-- THIS SENDS TO DLQ
                        .build());

        return container;
    }

    /* ---------- QUEUES ---------- */
    @Bean
    public Queue queue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING);
        // durable = true, exclusive = false, autoDelete = false
        return new Queue("testRunQueue", true, false, false, args);
    }

    @Bean
    public Queue dlq() {
        return new Queue("testRunDLQ", true);
    }

    /* ---------- EXCHANGE ---------- */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /* ---------- BINDINGS ---------- */
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlq, TopicExchange exchange) {
        return BindingBuilder.bind(dlq).to(exchange).with(DLQ_ROUTING);
    }
}