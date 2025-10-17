package com.example.test_framework_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleMessageListenerContainer listenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        // Configure retries
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);

        container.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .maxAttempts(3)
                        .backOffPolicy(backOffPolicy)
                        .recoverer(new RejectAndDontRequeueRecoverer()) // Send to DLQ after retries
                        .build()
        );
        return container;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // New: Fanout exchange example for broadcast (missing from Sprint 2 Team 2)
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("fanoutExchange"); // Broadcast to all bound queues
    }

    // New: Binding for fanout (example for pub-sub pattern)
    @Bean
    public Binding fanoutBinding(Queue queue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }
}