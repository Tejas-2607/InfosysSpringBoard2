package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.service.TestRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class TestRunController {

    @Autowired
    private TestRunService service;

    @PostMapping
    public ResponseEntity<TestRun> createTestRun(@RequestBody TestRun testRun) {
        return ResponseEntity.ok(service.createTestRun(testRun));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestRun> getTestRun(@PathVariable Long id) {
        return service.getTestRunById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<TestRun> getAllTestRuns() {
        return service.getAllTestRuns();
    }
}