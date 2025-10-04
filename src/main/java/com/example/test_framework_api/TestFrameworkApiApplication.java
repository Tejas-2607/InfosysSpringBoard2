package com.example.test_framework_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootApplication
@EnableRabbit
public class TestFrameworkApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestFrameworkApiApplication.class, args);
	}

	// RabbitMQ Beans (merged from RabbitmqApplication)
	public static final String QUEUE = "testRunQueue";
	public static final String DLQ = "testRunDLQ";
	public static final String EXCHANGE = "testRunExchange";
	public static final String ROUTING_KEY = "testRunKey";

	@Bean
	Queue queue() {
		return QueueBuilder.durable(QUEUE) // Durable queue
				.withArgument("x-dead-letter-exchange", EXCHANGE)
				.withArgument("x-dead-letter-routing-key", "dlq." + ROUTING_KEY)
				.build();
	}

	@Bean
	Queue dlq() {
		return QueueBuilder.durable(DLQ).build();
	}

	@Bean
	TopicExchange exchange() {
		return new TopicExchange(EXCHANGE); // Use topic for flexibility
	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
	}

	@Bean
	Binding dlqBinding(Queue dlq, TopicExchange exchange) {
		return BindingBuilder.bind(dlq).to(exchange).with("dlq." + ROUTING_KEY);
	}
}