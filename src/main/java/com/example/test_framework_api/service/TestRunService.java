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
    private AmqpTemplate amqpTemplate; // Used for sending

    public TestRun createTestRun(TestRun testRun) {
        testRun.setStatus("PENDING");
        TestRun saved = repository.save(testRun);

        // Enqueue TestRunRequest object directly (no manual serialization)
        TestRunRequest request = new TestRunRequest(saved.getId(), saved.getName());
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        System.out.println("Enqueued message for TestRun ID: " + saved.getId()); // Raw log

        return saved;
    }

    public Optional<TestRun> getTestRunById(Long id) {
        return repository.findById(id);
    }

    public List<TestRun> getAllTestRuns() {
        return repository.findAll();
    }
}