package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.example.test_framework_api.TestFrameworkApiApplication.*;

@Service
public class TestRunService {

    @Autowired
    private TestRunRepository repository;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private ObjectMapper objectMapper; // For JSON serialization

    public TestRun createTestRun(TestRun testRun) {
        // Business logic: Set initial status, etc.
        testRun.setStatus("PENDING");
        TestRun saved = repository.save(testRun);

        // Enqueue to RabbitMQ as JSON
        try {
            TestRunRequest request = new TestRunRequest(saved.getId(), saved.getName()); // Schema
            String jsonMessage = objectMapper.writeValueAsString(request);
            amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, jsonMessage);
        } catch (Exception e) {
            // Handle serialization/enqueue error
            throw new RuntimeException("Failed to enqueue test run", e);
        }

        return saved;
    }

    public Optional<TestRun> getTestRunById(Long id) {
        return repository.findById(id);
    }

    public List<TestRun> getAllTestRuns() {
        return repository.findAll();
    }
}