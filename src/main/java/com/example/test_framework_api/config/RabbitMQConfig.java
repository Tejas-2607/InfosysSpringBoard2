package com.example.test_framework_api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "testRunQueue";
    public static final String DLQ = "testRunDLQ";
    public static final String EXCHANGE = "testRunExchange";
    public static final String DLX = "testRunDLX";
    public static final String ROUTING_KEY = "testRunKey";

    // === QUEUES & EXCHANGES ===
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange dlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlq, DirectExchange dlx) {
        return BindingBuilder.bind(dlq).to(dlx).with(ROUTING_KEY);
    }

    // === MESSAGE CONVERTER ===
    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    // === RETRY TEMPLATE ===
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10000L);
        retryTemplate.setBackOffPolicy(backoff);
        return retryTemplate;
    }

    // === MANUAL MESSAGE LISTENER WITH RETRY SUPPORT ===
    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(
            ConnectionFactory connectionFactory,
            Queue queue,
            Jackson2JsonMessageConverter converter,
            RetryTemplate retryTemplate) {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queue);
        container.setDefaultRequeueRejected(false);
        container.setShutdownTimeout(10_000L);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(3);

        // ✅ Manual message listener with retry and JSON conversion
        container.setMessageListener(message -> {
            retryTemplate.execute(context -> {
                try {
                    // Convert JSON manually
                    String body = new String(message.getBody());
                    Object payload = converter.fromMessage(message);
                    System.out.println("Received message: " + payload);

                    // Simulate processing
                    if (body.contains("fail")) {
                        throw new RuntimeException("Simulated failure");
                    }

                    return null; // success
                } catch (Exception e) {
                    System.err.println("Retry attempt " + context.getRetryCount() + " failed: " + e.getMessage());
                    throw e;
                }
            });
        });

        return container;
    }
}
