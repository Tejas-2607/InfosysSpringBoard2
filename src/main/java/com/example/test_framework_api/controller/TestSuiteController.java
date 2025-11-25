package com.example.test_framework_api.controller;

import com.example.test_framework_api.dto.TestSuiteRequest;
import com.example.test_framework_api.dto.TestCaseExecutionRequest;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestSuiteService;
import com.example.test_framework_api.service.ProduceReportHtmlService;
import com.example.test_framework_api.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
// import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.test_framework_api.config.RabbitMQConfig.TEST_SUITE_QUEUE;

/**
 * CRITICAL FIXES:
 * - Proper exception handling for IllegalStateException (suite not executed)
 * - Clear error messages with execution instructions
 * - Separated report vs analytics endpoints
 * - CSV/PDF with actual test results
 */
@RestController
@RequestMapping("/api/suites")
@RequiredArgsConstructor
@Slf4j
public class TestSuiteController {

    private final TestSuiteService suiteService;
    private final TestRunService runService;
    private final RabbitTemplate rabbitTemplate;
    private final ProduceReportHtmlService reportService;
    private final MetricsService metricsService;
    private final UserRepository userRepository;

    @PostMapping("/import-csv")
    public ResponseEntity<TestSuite> importSuite(@ModelAttribute TestSuiteRequest request,
            Authentication authentication) {
        try {
            TestSuite suite = suiteService.importFromCsv(request.getCsvFile(),
                    request.getSuiteName(),
                    request.getDescription(),
                    authentication);
            return ResponseEntity.ok(suite);
        } catch (Exception e) {
            log.error("Failed to import CSV: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // @GetMapping
    // public ResponseEntity<List<TestSuite>> getSuites() {
    // return ResponseEntity.ok(suiteService.getAllSuites());
    // }

    @GetMapping("/{id}")
    public ResponseEntity<TestSuite> getSuite(@PathVariable Long id) {
        TestSuite suite = suiteService.getSuiteById(id);
        return suite != null ? ResponseEntity.ok(suite) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> runSuite(@PathVariable Long id) {
        return runSuiteWithThreads(id, 1);
    }

    @PostMapping("/{id}/execute-parallel")
    public ResponseEntity<Map<String, Object>> runSuiteParallel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(value = 1) @Max(value = 8) int parallelThreads) {
        return runSuiteWithThreads(id, parallelThreads);
    }

    private ResponseEntity<Map<String, Object>> runSuiteWithThreads(Long id, int parallelThreads) {
        if (parallelThreads < 1 || parallelThreads > 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "parallelThreads must be between 1 and 8",
                    "provided", parallelThreads,
                    "valid_range", "1-8"));
        }

        TestSuite suite = suiteService.getSuiteById(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }

        if (suite.getTestCases() == null || suite.getTestCases().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "warning", "Empty suite - no test cases to execute",
                    "suiteId", id,
                    "suiteName", suite.getName(),
                    "status", "EMPTY_COMPLETE",
                    "testCaseCount", 0));
        }

        TestRun run = runService.createTestRun(suite.getName() + "-Suite");
        run.setParallelThreads(parallelThreads);
        runService.updateTestRun(run);

        suite.setTestRun(run);
        suiteService.getSuiteById(id);

        TestCaseExecutionRequest req = new TestCaseExecutionRequest();
        req.setTestSuiteId(id);
        req.setTestRunId(run.getId());
        req.setParallelThreads(parallelThreads);
        rabbitTemplate.convertAndSend(TEST_SUITE_QUEUE, req);

        String mode = parallelThreads == 1 ? "sequential" : "parallel";
        String executorType = parallelThreads == 1 ? "single-thread"
                : (parallelThreads <= 4 ? "standard" : "high-concurrency");

        return ResponseEntity.ok(Map.of(
                "message", "Suite queued for execution",
                "testRunId", run.getId(),
                "suiteId", id,
                "testCaseCount", suite.getTestCases().size(),
                "parallelThreads", parallelThreads,
                "mode", mode,
                "executorType", executorType,
                "status", "PENDING"));
    }

    /**
     * FIXED: Proper exception handling for suite not executed
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<?> getSuiteReport(@PathVariable Long id) {
        try {
            String reportPath = reportService.generateSuiteReport(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Report generated successfully",
                    "reportPath", reportPath,
                    "suiteId", id));
        } catch (IllegalArgumentException e) {
            log.error("Suite not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "Suite not found",
                            "suiteId", id,
                            "message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Suite not executed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(Map.of(
                            "error", "Suite not executed",
                            "suiteId", id,
                            "message", e.getMessage(),
                            "action", "Execute the suite first",
                            "executeUrl", "/api/suites/" + id + "/execute"));
        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Report generation failed",
                            "message", e.getMessage()));
        }
    }

    /**
     * FIXED: Null-safe analytics endpoint with proper error handling
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<?> getSuiteAnalytics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days) {

        try {
            TestSuite suite = suiteService.getSuiteById(id);
            if (suite == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Suite not found", "suiteId", id));
            }

            // FIXED: Null-safe trend and flaky test retrieval
            List<Map<String, Object>> trends = metricsService.getTrends(id, days);
            List<Map<String, Object>> flakyTests = metricsService.getFlakyTests(id);
            MetricsService.Summary summary = metricsService.getSummaryForSuite(id);

            return ResponseEntity.ok(Map.of(
                    "suiteId", id,
                    "suiteName", suite.getName(),
                    "summary", Map.of(
                            "totalTests", summary.total(),
                            "passed", summary.passed(),
                            "failed", summary.failed(),
                            "passRate", summary.passRate(),
                            "avgDurationMs", summary.avgDurationMs(),
                            "stability", summary.stabilityLast10()),
                    "trends", Map.of(
                            "period", days + " days",
                            "data", trends != null ? trends : List.of()),
                    "flakyTests", Map.of(
                            "count", flakyTests != null ? flakyTests.size() : 0,
                            "tests", flakyTests != null ? flakyTests : List.of())));
        } catch (IllegalStateException e) {
            log.warn("Analytics unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body(Map.of(
                            "error", "Analytics unavailable",
                            "suiteId", id,
                            "message", "Suite has not been executed yet",
                            "action", "Execute the suite first",
                            "executeUrl", "/api/suites/" + id + "/execute"));
        } catch (NullPointerException e) {
            log.error("NPE in analytics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Analytics data unavailable",
                            "message", "Ensure suite has been executed",
                            "details", e.getMessage()));
        } catch (Exception e) {
            log.error("Analytics error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to generate analytics",
                            "details", e.getMessage()));
        }
    }

    /**
     * CRITICAL FIX: Proper exception handling for CSV export
     */
    @GetMapping("/{id}/export/csv")
    public ResponseEntity<?> exportCsv(@PathVariable Long id) {
        try {
            TestSuite suite = suiteService.getSuiteById(id);
            if (suite == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Suite not found", "suiteId", id));
            }

            byte[] csvContent = reportService.generateCsvReport(id);

            if (csvContent == null || csvContent.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "CSV generation failed - empty content"));
            }

            ByteArrayResource resource = new ByteArrayResource(csvContent);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=suite-" + id + "-report.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(csvContent.length)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            log.error("Suite not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "Suite not found",
                            "suiteId", id,
                            "message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Suite not executed for CSV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "Suite not executed",
                            "suiteId", id,
                            "message", e.getMessage(),
                            "action", "Execute the suite first using POST /api/suites/" + id + "/execute",
                            "executeUrl", "/api/suites/" + id + "/execute"));
        } catch (Exception e) {
            log.error("CSV generation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "error", "CSV generation failed",
                            "message", e.getMessage()));
        }
    }

    @GetMapping("/my-suites")
    public ResponseEntity<List<Map<String, Object>>> getMySuites(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TestSuite> userSuites = suiteService.getSuitesByUser(currentUser.getId());

        // FIXED: Format response with test case count
        List<Map<String, Object>> enrichedSuites = userSuites.stream()
                .map(suite -> {
                    Map<String, Object> suiteData = new HashMap<>();
                    suiteData.put("id", suite.getId());
                    suiteData.put("name", suite.getName());
                    suiteData.put("description", suite.getDescription());
                    suiteData.put("status", suite.getStatus());
                    int testCaseCount = suite.getTestCases() != null ? suite.getTestCases().size() : 0;
                    suiteData.put("testCases", suite.getTestCases());
                    suiteData.put("testCaseCount", testCaseCount);
                    suiteData.put("createdAt", suite.getCreatedAt());
                    return suiteData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(enrichedSuites);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getSuites() {
        List<TestSuite> suites = suiteService.getAllSuites();

        // Add creator info to each suite
        List<Map<String, Object>> enrichedSuites = suites.stream()
                .map(suite -> {
                    Map<String, Object> suiteData = new HashMap<>();
                    suiteData.put("id", suite.getId());
                    suiteData.put("name", suite.getName());
                    suiteData.put("description", suite.getDescription());
                    suiteData.put("status", suite.getStatus());
                    suiteData.put("testCases", suite.getTestCases());
                    suiteData.put("createdAt", suite.getCreatedAt());

                    int testCaseCount = suite.getTestCases() != null ? suite.getTestCases().size() : 0;
                    suiteData.put("testCaseCount", testCaseCount); // Add count for convenience

                    // FIXED #4: Add creator info
                    if (suite.getCreatedBy() != null) {
                        Map<String, Object> creatorInfo = new HashMap<>();
                        creatorInfo.put("userId", suite.getCreatedBy().getId());
                        creatorInfo.put("username", suite.getCreatedBy().getUsername());
                        creatorInfo.put("roles", suite.getCreatedBy().getRoles());
                        suiteData.put("createdBy", creatorInfo);
                    }

                    return suiteData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(enrichedSuites);
    }
}