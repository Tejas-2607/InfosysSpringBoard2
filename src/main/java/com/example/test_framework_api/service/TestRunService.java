// src/main/java/com/example/test_framework_api/service/TestRunService.java
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestRunService {

    @Autowired
    private TestRunRepository testRunRepository;

    public TestRun createTestRun(TestRunRequest request) {
        TestRun testRun = new TestRun();
        testRun.setName(request.getSuiteName());
        testRun.setStatus("PENDING");
        testRun.setCreatedAt(LocalDateTime.now());
        return testRunRepository.save(testRun);
    }

    public List<TestRun> getAllTestRuns() {
        return testRunRepository.findAll();
    }
}