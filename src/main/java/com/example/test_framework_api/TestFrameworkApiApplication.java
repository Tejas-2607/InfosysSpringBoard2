package com.example.test_framework_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestFrameworkApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestFrameworkApiApplication.class, args);
    }

    // Remove duplicate @Bean definitions for queue, exchange, etc. - keep in RabbitMQConfig
}