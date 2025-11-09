// src/main/java/com/example/test_framework_api/controller/TestRunController.java
package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.dto.MetricsDto;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestResultService;

// import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import com.example.test_framework_api.model.TestElementRequest;
import com.example.test_framework_api.service.MetricsService;
import com.example.test_framework_api.service.ProduceReportHtmlService;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runs")
public class TestRunController {

    @Autowired
    private TestRunService testRunService;
    // @Autowired
    // private RabbitTemplate rabbitTemplate; // ADD THIS
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private ProduceReportHtmlService produceReportHtmlService;
    @Autowired
    private MetricsService metricsService;
    

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

    @GetMapping("/reports-producehtml")
    public ResponseEntity<?> produceHtmlReport() {
        try {
            String fileName = produceReportHtmlService.generateReport();

            String publicUrl = "http://localhost:8080/reports/" + fileName;

            return ResponseEntity.ok(Map.of(
                    "message", "Report generated",
                    "file", fileName,
                    "url", publicUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsDto> getMetrics() {
        MetricsService.Summary s = metricsService.getSummary();
        // List<Object[]> rawTrend = metricsService.getTrend7Days();
        // List<double[]> trend = rawTrend.stream()
        // .map(row -> new double[] {
        // ((java.sql.Date)
        // row[0]).toLocalDate().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC)
        // * 1000,
        // (Double) row[1]
        // })
        // .toList();

        MetricsDto dto = new MetricsDto(
                s.total(), s.passed(), s.failed(),
                s.passRate(), s.avgDurationMs(), s.stabilityLast10()); // ,trend
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<String> produceHtmlReport(@PathVariable Long id) {
        TestRun run = testRunService.getTestRunById(id);
        if (run == null)
            return ResponseEntity.notFound().build();
        String reportPath = produceReportHtmlService.generateReport(run.getId()); // FIXED: Pass arg
        return ResponseEntity.ok(reportPath);
    }

    // @PostMapping("/test-element")
    // public ResponseEntity<?> runTestElement(@RequestBody TestElementRequest
    // request) {

    // if (request.getUrl() == null || request.getElementId() == null ||
    // request.getAction() == null) {
    // return ResponseEntity.badRequest()
    // .body(Map.of("error", "Missing required fields: URL, Element ID, Action"));
    // }
    // TestRunRequest trRequest = new TestRunRequest();
    // trRequest.setSuiteName("Dynamic Element Test: " + request.getElementId());
    // TestRun testRun = testRunService.createTestRun(trRequest);

    // // Trigger Selenium via worker (pass inputs as JSON payload)
    // Map<String, Object> payload = Map.of(
    // "url", request.getUrl(),
    // "elementId", request.getElementId(),
    // "action", request.getAction(),
    // "expectedResult", request.getExpectedResult(),
    // "testRunId", testRun.getId());
    // rabbitTemplate.convertAndSend("testRunExchange", "elementTestKey", payload);

    // return ResponseEntity.ok(Map.of(
    // "message", "Test triggered successfully",
    // "testRunId", testRun.getId(),
    // "status", "PENDING"));
    // }
}
