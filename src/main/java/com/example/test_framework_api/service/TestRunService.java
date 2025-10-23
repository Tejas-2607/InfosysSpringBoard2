package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.example.test_framework_api.config.RabbitMQConfig.EXCHANGE;
import static com.example.test_framework_api.config.RabbitMQConfig.ROUTING_KEY;

@Service
public class TestRunService {

    @Autowired
    private TestRunRepository repository;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public TestRun createTestRun(TestRun testRun) {
        testRun.setStatus("PENDING");
        TestRun saved = repository.save(testRun);

        try {
            TestRunRequest request = new TestRunRequest(saved.getId(), saved.getName());
            amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
            System.out.println("Enqueued message for TestRun ID: " + saved.getId());
        } catch (Exception e) {
            System.err.println("Failed to enqueue message for TestRun ID: " + saved.getId() + ": " + e.getMessage());
            // Optionally, update status to FAILED or rethrow
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