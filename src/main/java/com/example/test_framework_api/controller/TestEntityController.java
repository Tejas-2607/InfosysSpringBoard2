package com.example.test_framework_api.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.test_framework_api.service.TestRunService;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test_framework_api.model.TestElementRequest;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
// import org.springframework.http.HttpStatus;

/**
 * Controller for dynamic element testing.
 * Endpoint: POST /test-element
 * Supports single or multiple actions.
 * Fixed: Null-safe payload for multi-actions.
 */
@RestController
@RequestMapping("/test-element")
public class TestEntityController {
  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private TestRunService testRunService;

  @PostMapping
  public ResponseEntity<?> runTestElement(@RequestBody TestElementRequest request) {

    // Validate basics
    if (request.getUrl() == null || request.getElementId() == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Missing required fields: URL, Element ID"));
    }

    // Create test run
    TestRunRequest trRequest = new TestRunRequest();
    trRequest.setSuiteName("Dynamic Element Test: " + request.getElementId());
    TestRun testRun = testRunService.createTestRun(trRequest);

    // FIXED: Null-safe payload (Map.ofNullable ignores nulls)
    Map<String, Object> payload = new HashMap<>();
    payload.put("url", request.getUrl());
    payload.put("elementId", request.getElementId());
    if (request.getAction() != null) {
      payload.put("action", request.getAction()); // Single action only if present
    }
    payload.put("actions", request.getActions()); // Multi always included (null OK)
    payload.put("expectedResult", request.getExpectedResult());
    payload.put("testRunId", testRun.getId());

    rabbitTemplate.convertAndSend("testRunExchange", "elementTestKey", payload);

    return ResponseEntity.ok(Map.of(
        "message", "Test triggered successfully",
        "testRunId", testRun.getId(),
        "status", "PENDING"));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getTestRunStatus(@PathVariable Long id) {
    TestRun testRun = testRunService.getTestRunById(id); // Implement this in TestRunService if not already present

    if (testRun == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Test run not found for ID: " + id));
    }

    // Customize this response based on your TestRun fields (e.g., add results, logs
    // if available)
    return ResponseEntity.ok(Map.of(
        "testRunId", testRun.getId(),
        "suiteName", testRun.getName(),
        "status", testRun.getStatus(), // Assuming TestRun has a 'status' field (e.g., PENDING, RUNNING, COMPLETED,
                                       // FAILED)
        "createdAt", testRun.getCreatedAt() // Optional: Include timestamps if available
    ));
  }

  @GetMapping("/{id}/results")
  public ResponseEntity<?> getTestResults(@PathVariable Long id) {
    // First, check if test run exists
    TestRun testRun = testRunService.getTestRunById(id);
    if (testRun == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Test run not found for ID: " + id));
    }

    // Fetch results separately
    var testResultsList = testRunService.getTestResultsByTestRunId(id).stream()
        .map(result -> Map.of(
            "id", result.getId(),
            "testName", result.getTestName(),
            "status", result.getStatus(),
            "retryCount", result.getRetryCount(),
            "duration", result.getDuration(),
            "createdAt", result.getCreatedAt()))
        .collect(Collectors.toList());

    return ResponseEntity.ok(Map.of(
        "testRunId", id,
        "testResults", testResultsList));
  }
}