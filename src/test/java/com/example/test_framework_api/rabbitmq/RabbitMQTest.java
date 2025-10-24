package com.example.test_framework_api.rabbitmq;

import com.example.test_framework_api.model.TestRunRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.example.test_framework_api.config.RabbitMQConfig.EXCHANGE;
import static com.example.test_framework_api.config.RabbitMQConfig.ROUTING_KEY;
// import static org.mockito.Mockito.mock;

@SpringBootTest
public class RabbitMQTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // Mock RabbitMQ for local testing (requires testcontainer or manual setup)
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> 5672);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Test
    public void testSendMessage() {
        TestRunRequest request = new TestRunRequest(1L, "TestRun1");
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        System.out.println("Sent TestRunRequest: " + request);
        // Add assertion or wait for consumer (requires mock or test listener)
    }
}