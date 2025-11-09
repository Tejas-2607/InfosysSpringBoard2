// package com.example.test_framework_api.config;

// import org.springframework.amqp.core.*;
// import org.springframework.amqp.rabbit.connection.ConnectionFactory;
// import org.springframework.amqp.rabbit.core.RabbitAdmin;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Lazy;
// import org.springframework.retry.annotation.EnableRetry;

// @Configuration
// @EnableRetry
// public class RabbitMQConfig {
//     public static final String EXCHANGE = "testRunExchange";
//     public static final String ROUTING_KEY = "test.run";
//     public static final String QUEUE = "testRunQueue";
//     public static final String DLQ = "testRunDLQ"; // DLQ

//     @Bean
//     public Queue testRunQueue() {
//         return QueueBuilder.durable(QUEUE).withArgument("x-dead-letter-exchange", EXCHANGE)
//                 .withArgument("x-dead-letter-routing-key", "dlq").build();
//     }

//     @Bean
//     public Queue deadLetterQueue() {
//         return new Queue(DLQ, true); // Durable DLQ
//     }

//     @Bean
//     public DirectExchange exchange() {
//         return new DirectExchange(EXCHANGE);
//     }

//     @Bean
//     public Binding binding() {
//         return BindingBuilder.bind(testRunQueue()).to(exchange()).with(ROUTING_KEY);
//     }

//     @Bean
//     public Binding dlqBinding() {
//         return BindingBuilder.bind(deadLetterQueue()).to(exchange()).with("dlq");
//     }

//     @Bean
//     public Jackson2JsonMessageConverter jsonMessageConverter() {
//         return new Jackson2JsonMessageConverter();
//     }

//     @Bean
//     public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//         return new RabbitAdmin(connectionFactory);
//     }

//     @Autowired
//     public void configureRabbitTemplate(@Lazy RabbitTemplate rabbitTemplate, ConnectionFactory connectionFactory) {
//         rabbitTemplate.setConnectionFactory(connectionFactory);
//         rabbitTemplate.setMessageConverter(jsonMessageConverter());
//     }
// }

// package com.example.test_framework_api.config;

// import org.springframework.amqp.core.*;
// import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
// import org.springframework.amqp.rabbit.connection.ConnectionFactory;
// import org.springframework.amqp.rabbit.core.RabbitAdmin;
// import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
// // import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.retry.annotation.EnableRetry;
// import org.springframework.retry.backoff.ExponentialBackOffPolicy;
// import org.springframework.retry.policy.SimpleRetryPolicy;
// import org.springframework.retry.support.RetryTemplate;

// @Configuration
// @EnableRetry
// public class RabbitMQConfig {
//     public static final String EXCHANGE = "testRunExchange";
//     public static final String ROUTING_KEY = "testRunKey";
//     public static final String QUEUE = "testRunQueue";
//     public static final String DLQ = "dlq.testRunKey"; // DLQ

//     @Bean
//     public Queue testRunQueue() {
//         return QueueBuilder.durable(QUEUE).withArgument("x-dead-letter-exchange", EXCHANGE)
//                 .withArgument("x-dead-letter-routing-key", "dlq.testRunKey").build();
//     }

//     @Bean
//     public Queue deadLetterQueue() {
//         return new Queue(DLQ, true); // Durable DLQ
//     }

//     @Bean
//     public TopicExchange exchange() {
//         return new TopicExchange(EXCHANGE, true, false);
//     }

//     @Bean
//     public Binding binding() {
//         return BindingBuilder.bind(testRunQueue()).to(exchange()).with(ROUTING_KEY);
//     }

//     @Bean
//     public Binding dlqBinding() {
//         return BindingBuilder.bind(deadLetterQueue()).to(exchange()).with(DLQ);
//     }

//     @Bean
//     public Jackson2JsonMessageConverter jsonMessageConverter() {
//         return new Jackson2JsonMessageConverter();
//     }

//     @Bean
//     public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//         return new RabbitAdmin(connectionFactory);
//     }

//     @Bean
//     public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
//             ConnectionFactory connectionFactory) {
//         SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
//         factory.setConnectionFactory(connectionFactory);
//         factory.setDefaultRequeueRejected(false); // REJECT + NO REQUEUE
//         return factory;
//     }

//     // ...existing code...
//     // Removed autowired configureRabbitTemplate(...) to avoid circular dependency.
//     // Use RabbitTemplateCustomizer so the auto-configured RabbitTemplate is
//     // customized lazily by Spring Boot.
//     @Bean
//     public RetryTemplate retryTemplate() {
//         RetryTemplate retryTemplate = new RetryTemplate();

//         // Exponential backoff
//         ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
//         backOffPolicy.setInitialInterval(1000);
//         backOffPolicy.setMultiplier(2.0);
//         backOffPolicy.setMaxInterval(10000L);
//         retryTemplate.setBackOffPolicy(backOffPolicy);

//         // Max 3 attempts
//         SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
//         retryPolicy.setMaxAttempts(3);
//         retryTemplate.setRetryPolicy(retryPolicy);

//         return retryTemplate;
//     }
//     // ...existing code...
// }

package com.example.test_framework_api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "testRunExchange";
    public static final String ROUTING_KEY = "testRunKey";
    public static final String QUEUE = "testRunQueue";
    public static final String DLQ = "dlq.testRunKey";
    public static final String TEST_SUITE_QUEUE = "testSuiteQueue"; // NEW FEATURE: Constant
    public static final String TEST_SUITE_KEY = "testSuiteKey";

    /* ---------- Queues ---------- */
    @Bean
    public Queue testRunQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() { // FIXED: Define missing bean
        return new TopicExchange("dlx.exchange");
    }

    @Bean
    public TopicExchange testExchange() { // FIXED: Define missing bean (reuse or new)
        return new TopicExchange("test.exchange"); // Or reuse existing EXCHANGE
    }

    /* ---------- Exchange ---------- */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue elementTestQueue() {
        return QueueBuilder.durable("elementTestQueue").build();
    }

    @Bean
    public Binding elementTestBinding() {
        return BindingBuilder.bind(elementTestQueue()).to(exchange()).with("elementTestKey");
    }

    /* ---------- Bindings ---------- */
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(testRunQueue()).to(exchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(exchange()).with(DLQ);
    }

    /* ---------- Message converter ---------- */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory cf) {
        return new RabbitAdmin(cf);
    }

    /* ---------- Listener container (no requeue) ---------- */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, Jackson2JsonMessageConverter converter) {

        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(converter);
        f.setDefaultRequeueRejected(false); // <-- crucial
        return f;
    }

    /* ---------- RetryTemplate (shared) ---------- */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate rt = new RetryTemplate();

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        rt.setBackOffPolicy(backOff);

        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(3);
        rt.setRetryPolicy(policy);

        return rt;
    }

    @Bean
    public Queue testSuiteQueue() { // NEW FEATURE: Queue for suite execution
        return QueueBuilder.durable("testSuiteQueue")
                .withArgument("x-dead-letter-exchange", deadLetterExchange().getName()).build();
    }

    @Bean
    public Binding testSuiteBinding() { // NEW FEATURE: Binding for suite requests
        return BindingBuilder.bind(testSuiteQueue()).to(testExchange()).with("testSuiteKey");
    }
}