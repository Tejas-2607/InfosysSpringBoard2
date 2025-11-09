// src/main/java/com/example/test_framework_api/service/TestRunService.java
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestRunRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestRunService {
    @Autowired
    private RabbitTemplate rabbitTemplate; // ADD THIS
    private final TestRunRepository runRepository; // FIXED: Unified repository reference (removed duplicate @Autowired)

    public static final String EXCHANGE = "testRunExchange";
    public static final String ROUTING_KEY = "testRunKey";

    public TestRun createTestRun(TestRunRequest request) {
        TestRun testRun = new TestRun();
        testRun.setName(request.getSuiteName());
        testRun.setStatus(TestStatus.PENDING); // FIXED: Use enum instead of String
        testRun.setCreatedAt(LocalDateTime.now());
        testRun = runRepository.save(testRun);
        request.setTestId(testRun.getId()); // Important: set the generated ID
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        return testRun;
    }

    public TestRun createTestRun(String suiteName) {
        TestRun run = new TestRun();
        run.setName(suiteName); // FIXED: Use setName instead of setSuiteName (assuming model has setName for suiteName)
        run.setStatus(TestStatus.PENDING); // FIXED: Enum
        run.setCreatedAt(LocalDateTime.now());
        return runRepository.save(run);
    }

    public List<TestRun> getAllTestRuns() {
        return runRepository.findAll();
    }

    @Cacheable(value = "testRuns", key = "#id")
    public TestRun getTestRunById(Long id) {
        Optional<TestRun> optionalTestRun = runRepository.findById(id);
        return optionalTestRun.orElse(null);
    }

    public List<TestResult> getTestResultsByTestRunId(Long testRunId) {

        return runRepository.findTestResultsByTestRunId(testRunId); // Implement this in TestRunRepository
    }

    public TestRun updateTestRun(TestRun run) {
        return runRepository.save(run);
    }
}