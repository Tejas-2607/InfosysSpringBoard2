package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRunRequest;
import org.springframework.stereotype.Component;

@Component
public class TestExecutor {

    // Skeleton for executing tests (expand in later sprints)
    public void executeTest(TestRunRequest request) {
  
        System.out.println("Executing test for ID: " + request.getTestId());
        // Simulate execution
    }
}