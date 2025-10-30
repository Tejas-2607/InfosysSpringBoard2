// src/main/java/com/example/test_framework_api/controller/TestRunController.java
package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class TestRunController {

    @Autowired
    private TestRunService testRunService;

    @Autowired
    private TestResultService testResultService;

    // CREATE test_run
    @PostMapping
    public ResponseEntity<TestRun> createTestRun(@RequestBody TestRunRequest request) {
        TestRun testRun = testRunService.createTestRun(request);
        return ResponseEntity.ok(testRun);
    }

    // GET ONLY test_run TABLE
    @GetMapping
    public ResponseEntity<List<TestRun>> getTestRuns() {
        return ResponseEntity.ok(testRunService.getAllTestRuns());
    }

    // GET ONLY test_result TABLE
    @GetMapping("/reports")
    public ResponseEntity<List<TestResult>> getTestResults() {
        return ResponseEntity.ok(testResultService.getAllTestResults());
    }
}