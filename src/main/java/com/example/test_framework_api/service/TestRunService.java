// src/main/java/com/example/test_framework_api/service/TestRunService.java
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TestRunService {
    @Autowired
    private RabbitTemplate rabbitTemplate; // ADD THIS
    @Autowired
    private TestRunRepository testRunRepository;
    public static final String EXCHANGE = "testRunExchange";
    public static final String ROUTING_KEY = "testRunKey";

    public TestRun createTestRun(TestRunRequest request) {
        TestRun testRun = new TestRun();
        testRun.setName(request.getSuiteName());
        testRun.setStatus("PENDING");
        testRun.setCreatedAt(LocalDateTime.now());
        testRun = testRunRepository.save(testRun);
        request.setTestId(testRun.getId()); // Important: set the generated ID
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        return testRun;
    }

    public List<TestRun> getAllTestRuns() {
        return testRunRepository.findAll();
    }

    @Cacheable(value = "testRuns", key = "#id")
    public TestRun getTestRunById(Long id) {
        Optional<TestRun> optionalTestRun = testRunRepository.findById(id);
        return optionalTestRun.orElse(null);
    }

    public List<TestResult> getTestResultsByTestRunId(Long testRunId) {

        return testRunRepository.findTestResultsByTestRunId(testRunId); // Implement this in TestRunRepository
    }
}