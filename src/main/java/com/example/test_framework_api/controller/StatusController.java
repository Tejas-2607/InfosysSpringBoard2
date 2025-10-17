package com.example.test_framework_api.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        Map<String, String> status = new HashMap<>();
        try {
            // Basic health check: Test RabbitMQ connection
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive("testRunQueue"); // Check if queue exists
                return null;
            });
            status.put("app", "UP");
            status.put("rabbitmq", "Connected");
        } catch (Exception e) {
            status.put("app", "UP");
            status.put("rabbitmq", "Disconnected: " + e.getMessage());
        }
        return ResponseEntity.ok(status);
    }
}