package com.example.test_framework_api.controller;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.dto.MetricsDto;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestResultService;
import com.example.test_framework_api.service.MetricsService;
import com.example.test_framework_api.service.ProduceReportHtmlService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/runs")
@Slf4j
public class TestRunController {

    @Autowired
    private TestRunService testRunService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private ProduceReportHtmlService produceReportHtmlService;
    @Autowired
    private MetricsService metricsService;

    @PostMapping
    public ResponseEntity<TestRun> createTestRun(@RequestBody TestRunRequest request) {
        TestRun testRun = testRunService.createTestRun(request);
        return ResponseEntity.ok(testRun);
    }

    // FIX: Return DTOs instead of entities to avoid circular references
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getTestRuns() {
        List<TestRun> runs = testRunService.getAllTestRuns();
        
        List<Map<String, Object>> runDtos = runs.stream()
            .map(run -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", run.getId());
                dto.put("name", run.getName());
                dto.put("status", run.getStatus());
                dto.put("parallelThreads", run.getParallelThreads());
                dto.put("createdAt", run.getCreatedAt());
                dto.put("reportPath", run.getReportPath());
                
                // Include creator info safely
                if (run.getCreatedBy() != null) {
                    Map<String, Object> creator = new HashMap<>();
                    creator.put("id", run.getCreatedBy().getId());
                    creator.put("username", run.getCreatedBy().getUsername());
                    dto.put("createdBy", creator);
                }
                
                // Include result count without full objects
                dto.put("resultCount", run.getTestResults() != null ? run.getTestResults().size() : 0);
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(runDtos);
    }

    // FIX: Return DTOs for test results
    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> getTestResults() {
        List<TestResult> results = testResultService.getAllTestResults();
        
        List<Map<String, Object>> resultDtos = results.stream()
            .map(result -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", result.getId());
                dto.put("testName", result.getTestName());
                dto.put("status", result.getStatus());
                dto.put("duration", result.getDuration());
                dto.put("retryCount", result.getRetryCount());
                dto.put("errorMessage", result.getErrorMessage());
                dto.put("createdAt", result.getCreatedAt());
                dto.put("flakyScore", result.getFlakyScore());
                
                // Include test run ID without full object
                if (result.getTestRun() != null) {
                    dto.put("testRunId", result.getTestRun().getId());
                    dto.put("testRunName", result.getTestRun().getName());
                }
                
                // Include executor info safely
                if (result.getExecutedBy() != null) {
                    Map<String, Object> executor = new HashMap<>();
                    executor.put("id", result.getExecutedBy().getId());
                    executor.put("username", result.getExecutedBy().getUsername());
                    dto.put("executedBy", executor);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(resultDtos);
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsDto> getMetrics() {
        MetricsService.Summary s = metricsService.getSummary();
        MetricsDto dto = new MetricsDto(
            s.total(), s.passed(), s.failed(),
            s.passRate(), s.avgDurationMs(), s.stabilityLast10()
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> produceHtmlReport(@PathVariable Long id) {
        TestRun run = testRunService.getTestRunById(id);
        if (run == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            String reportPath = produceReportHtmlService.generateReport(id);
            String publicUrl = "http://localhost:8080/reports/run-" + id + "/run-report.html";
            
            return ResponseEntity.ok(Map.of(
                "message", "Report generated successfully",
                "reportPath", reportPath,
                "url", publicUrl,
                "testRunId", id,
                "testRunName", run.getName()
            ));
        } catch (Exception e) {
            log.error("Report generation failed for run {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Report generation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    // NEW: Get single test run by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTestRunById(@PathVariable Long id) {
        TestRun run = testRunService.getTestRunById(id);
        if (run == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", run.getId());
        dto.put("name", run.getName());
        dto.put("status", run.getStatus());
        dto.put("parallelThreads", run.getParallelThreads());
        dto.put("createdAt", run.getCreatedAt());
        dto.put("reportPath", run.getReportPath());
        
        if (run.getCreatedBy() != null) {
            Map<String, Object> creator = new HashMap<>();
            creator.put("id", run.getCreatedBy().getId());
            creator.put("username", run.getCreatedBy().getUsername());
            dto.put("createdBy", creator);
        }
        
        // Include results as DTOs
        if (run.getTestResults() != null) {
            List<Map<String, Object>> resultDtos = run.getTestResults().stream()
                .map(r -> {
                    Map<String, Object> rdto = new HashMap<>();
                    rdto.put("id", r.getId());
                    rdto.put("testName", r.getTestName());
                    rdto.put("status", r.getStatus());
                    rdto.put("duration", r.getDuration());
                    rdto.put("retryCount", r.getRetryCount());
                    return rdto;
                })
                .collect(Collectors.toList());
            dto.put("testResults", resultDtos);
        }
        
        return ResponseEntity.ok(dto);
    }
}