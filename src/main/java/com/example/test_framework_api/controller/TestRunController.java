package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.service.TestRunService;
import jakarta.validation.Valid; // Added for validation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class TestRunController {

    @Autowired
    private TestRunService testRunService;

    @PostMapping
    public ResponseEntity<TestRun> createTestRun(@Valid @RequestBody TestRun testRun) { // Added @Valid for schema validation
        TestRun saved = testRunService.createTestRun(testRun);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestRun> getTestRunById(@PathVariable Long id) {
        return testRunService.getTestRunById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TestRun>> getAllTestRuns() {
        return ResponseEntity.ok(testRunService.getAllTestRuns());
    }
}