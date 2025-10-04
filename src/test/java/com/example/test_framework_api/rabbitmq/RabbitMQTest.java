package com.example.test_framework_api.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.example.test_framework_api.TestFrameworkApiApplication.EXCHANGE;
import static com.example.test_framework_api.TestFrameworkApiApplication.ROUTING_KEY;

@SpringBootTest
public class RabbitMQTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage() {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, "Test Message");
    }
}