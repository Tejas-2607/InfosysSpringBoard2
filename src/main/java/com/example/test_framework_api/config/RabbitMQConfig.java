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

package com.example.test_framework_api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RabbitMQConfig {
    public static final String EXCHANGE = "testRunExchange";
    public static final String ROUTING_KEY = "test.run";
    public static final String QUEUE = "testRunQueue";
    public static final String DLQ = "testRunDLQ"; // DLQ

    @Bean
    public Queue testRunQueue() {
        return QueueBuilder.durable(QUEUE).withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq").build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ, true); // Durable DLQ
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(testRunQueue()).to(exchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(exchange()).with("dlq");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // ...existing code...
    // Removed autowired configureRabbitTemplate(...) to avoid circular dependency.
    // Use RabbitTemplateCustomizer so the auto-configured RabbitTemplate is customized lazily by Spring Boot.
    @Bean
    public RabbitTemplateCustomizer rabbitTemplateCustomizer(Jackson2JsonMessageConverter jsonMessageConverter) {
        return rabbitTemplate -> {
            rabbitTemplate.setMessageConverter(jsonMessageConverter);
            // Do not set connectionFactory here; Spring Boot will set it on the auto-configured RabbitTemplate.
        };
    }
    // ...existing code...
}