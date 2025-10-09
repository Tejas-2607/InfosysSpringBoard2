package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.test_framework_api.TestFrameworkApiApplication.*;

@Service
public class TestRunService {

    @Autowired
    private TestRunRepository repository;
    
    @Autowired
    private AmqpTemplate amqpTemplate;

    
    public TestRun createTestRun(TestRun testRun) {
        
        testRun.setStatus("PENDING");
        testRun.setCreatedAt(LocalDateTime.now());
        TestRun saved = repository.save(testRun);

        
        try {
            TestRunRequest request = new TestRunRequest(saved.getId(), saved.getName());
            amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception e) {
            
            throw new RuntimeException("Failed to enqueue test run", e);
        }

        return saved;
    }

    /**
     * Updates an existing TestRun.
     * CRITICAL: This method ONLY saves to the database and does NOT send a message
     * to RabbitMQ, to prevent infinite loops.
     */
    public TestRun updateTestRun(TestRun testRun) {
        return repository.save(testRun);
    }

    public Optional<TestRun> getTestRunById(Long id) {
        return repository.findById(id);
    }

    public List<TestRun> getAllTestRuns() {
        return repository.findAll();
    }
}
