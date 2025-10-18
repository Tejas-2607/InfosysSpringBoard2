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

import static com.example.test_framework_api.TestFrameworkApiApplication.EXCHANGE;
import static com.example.test_framework_api.TestFrameworkApiApplication.ROUTING_KEY;

@Service
public class TestRunService {

    @Autowired
    private TestRunRepository repository;

    @Autowired
    private AmqpTemplate amqpTemplate; // Fixed: Used for sending

    @Autowired
    private ObjectMapper objectMapper; // Fixed: Used for serialization

    public TestRun createTestRun(TestRun testRun) {
        testRun.setStatus("PENDING");
        TestRun saved = repository.save(testRun);

        // Enqueue to RabbitMQ (Sprint 2 Team 3: Producer code)
        try {
            TestRunRequest request = new TestRunRequest(saved.getId(), saved.getName());
            String jsonMessage = objectMapper.writeValueAsString(request); // Sprint 2 Team 4: Jackson serialization
            amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, jsonMessage);
            System.out.println("Enqueued message for TestRun ID: " + saved.getId()); // Raw log
        } catch (Exception e) {
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