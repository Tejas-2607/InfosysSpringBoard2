// src/main/java/com/example/test_framework_api/service/TestResultService.java
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestResultService {

    @Autowired
    private TestResultRepository testResultRepository;

    public List<TestResult> getAllTestResults() {
        return testResultRepository.findAll();
    }

    public TestResult saveTestResult(TestResult testResult) {
        return testResultRepository.save(testResult);
    }
}