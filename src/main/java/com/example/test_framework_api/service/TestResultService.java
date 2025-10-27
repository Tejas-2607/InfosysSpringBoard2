package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestResultService {
    @Autowired private TestResultRepository repository;

    public TestResult saveTestResult(TestResult result) {
        return repository.save(result);
    }
}