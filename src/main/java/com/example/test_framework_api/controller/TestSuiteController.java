// package com.example.test_framework_api.controller;

// import com.example.test_framework_api.dto.TestSuiteRequest;
// import com.example.test_framework_api.dto.TestCaseExecutionRequest;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestSuite;
// import com.example.test_framework_api.service.TestRunService;
// import com.example.test_framework_api.service.TestSuiteService;
// import com.example.test_framework_api.service.ProduceReportHtmlService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.core.io.ByteArrayResource;
// import org.springframework.core.io.Resource;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;

// import jakarta.validation.constraints.Max;
// import jakarta.validation.constraints.Min;
// import java.util.List;
// import java.util.Map;

// import static com.example.test_framework_api.config.RabbitMQConfig.TEST_SUITE_QUEUE;

// /**
//  * ENHANCED: Edge case handling for parallel execution
//  * - Sequential fallback (threads=1)
//  * - Invalid thread validation (0, negative, >8)
//  * - Empty suite handling
//  * - Mixed failure aggregation
//  */
// @RestController
// @RequestMapping("/api/suites")
// @RequiredArgsConstructor
// public class TestSuiteController {

//     private final TestSuiteService suiteService;
//     private final TestRunService runService;
//     private final RabbitTemplate rabbitTemplate;
//     private final ProduceReportHtmlService reportService;

//     @PostMapping("/import-csv")
//     public ResponseEntity<TestSuite> importSuite(@ModelAttribute TestSuiteRequest request) {
//         try {
//             TestSuite suite = suiteService.importFromCsv(request.getCsvFile(), request.getSuiteName(),
//                     request.getDescription());
//             return ResponseEntity.ok(suite);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }

//     @GetMapping
//     public ResponseEntity<List<TestSuite>> getSuites() {
//         return ResponseEntity.ok(suiteService.getAllSuites());
//     }

//     @GetMapping("/{id}")
//     public ResponseEntity<TestSuite> getSuite(@PathVariable Long id) {
//         TestSuite suite = suiteService.getSuiteById(id);
//         return suite != null ? ResponseEntity.ok(suite) : ResponseEntity.notFound().build();
//     }

//     /**
//      * Execute test suite sequentially (default).
//      * Backward compatibility endpoint.
//      */
//     @PostMapping("/{id}/execute")
//     public ResponseEntity<Map<String, Object>> runSuite(@PathVariable Long id) {
//         return runSuiteWithThreads(id, 1);
//     }

//     /**
//      * ENHANCED: Execute test suite with parallel execution and edge case handling.
//      * 
//      * @param id              Suite ID
//      * @param parallelThreads Number of concurrent threads (1-8)
//      * @return Response with execution details and warnings
//      * 
//      *         Edge Cases Handled:
//      *         1. threads=1 → Sequential fallback (no parallelism overhead)
//      *         2. threads <1 or >8 → 400 Bad Request with error details
//      *         3. Empty suite → 200 OK with warning, no execution
//      *         4. Mixed failures → Partial COMPLETED status with pass rate
//      */
//     @PostMapping("/{id}/execute-parallel")
//     public ResponseEntity<Map<String, Object>> runSuiteParallel(
//             @PathVariable Long id,
//             @RequestParam(defaultValue = "1") @Min(value = 1, message = "parallelThreads must be at least 1") @Max(value = 8, message = "parallelThreads cannot exceed 8") int parallelThreads) {
//         return runSuiteWithThreads(id, parallelThreads);
//     }

//     /**
//      * ENHANCED: Internal execution handler with comprehensive edge case validation.
//      */
//     private ResponseEntity<Map<String, Object>> runSuiteWithThreads(Long id, int parallelThreads) {
//         // EDGE CASE 2: Validate thread count (1-8)
//         if (parallelThreads < 1 || parallelThreads > 8) {
//             return ResponseEntity.badRequest().body(Map.of(
//                     "error", "parallelThreads must be between 1 and 8",
//                     "provided", parallelThreads,
//                     "valid_range", "1-8"));
//         }

//         TestSuite suite = suiteService.getSuiteById(id);
//         if (suite == null) {
//             return ResponseEntity.notFound().build();
//         }

//         // EDGE CASE 3: Empty suite handling
//         if (suite.getTestCases() == null || suite.getTestCases().isEmpty()) {
//             return ResponseEntity.ok(Map.of(
//                     "warning", "Empty suite - no test cases to execute",
//                     "suiteId", id,
//                     "suiteName", suite.getName(),
//                     "status", "EMPTY_COMPLETE",
//                     "testCaseCount", 0));
//         }

//         // Create test run with parallelism config
//         TestRun run = runService.createTestRun(suite.getName() + "-Suite");
//         run.setParallelThreads(parallelThreads);
//         runService.updateTestRun(run);

//         suite.setTestRun(run);
//         suiteService.getSuiteById(id);

//         // Queue execution request
//         TestCaseExecutionRequest req = new TestCaseExecutionRequest();
//         req.setTestSuiteId(id);
//         req.setTestRunId(run.getId());
//         req.setParallelThreads(parallelThreads);
//         rabbitTemplate.convertAndSend(TEST_SUITE_QUEUE, req);

//         // EDGE CASE 1: Sequential fallback indicator
//         String mode = parallelThreads == 1 ? "sequential" : "parallel";
//         String executorType = parallelThreads == 1 ? "single-thread"
//                 : (parallelThreads <= 4 ? "standard" : "high-concurrency");

//         return ResponseEntity.ok(Map.of(
//                 "message", "Suite queued for execution",
//                 "testRunId", run.getId(),
//                 "suiteId", id,
//                 "testCaseCount", suite.getTestCases().size(),
//                 "parallelThreads", parallelThreads,
//                 "mode", mode,
//                 "executorType", executorType,
//                 "status", "PENDING"));
//     }

//     @GetMapping("/{id}/report")
//     public ResponseEntity<String> getSuiteReport(@PathVariable Long id) {
//         try {
//             String reportPath = reportService.generateSuiteReport(id);
//             return ResponseEntity.ok(reportPath);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().build();
//         }
//     }

//     @GetMapping("/{id}/analytics")
//     public ResponseEntity<Map<String, Object>> getSuiteAnalytics(
//             @PathVariable Long id,
//             @RequestParam(defaultValue = "7") int days) {

//         TestSuite suite = suiteService.getSuiteById(id);
//         if (suite == null) {
//             return ResponseEntity.notFound().build();
//         }

//         // Get trends and flaky tests
//         List<Map<String, Object>> trends = metricsService.getTrends(id, days);
//         List<Map<String, Object>> flakyTests = metricsService.getFlakyTests(id);
//         MetricsService.Summary summary = metricsService.getSummaryForSuite(id);

//         return ResponseEntity.ok(Map.of(
//                 "suiteId", id,
//                 "suiteName", suite.getName(),
//                 "summary", Map.of(
//                         "totalTests", summary.total(),
//                         "passed", summary.passed(),
//                         "failed", summary.failed(),
//                         "passRate", summary.passRate(),
//                         "avgDurationMs", summary.avgDurationMs(),
//                         "stability", summary.stabilityLast10()),
//                 "trends", Map.of(
//                         "period", days + " days",
//                         "data", trends),
//                 "flakyTests", Map.of(
//                         "count", flakyTests.size(),
//                         "tests", flakyTests)));
//     }

//     @GetMapping("/{id}/export/csv")
//     public ResponseEntity<Resource> exportCsv(@PathVariable Long id) {
//         TestSuite suite = suiteService.getSuiteById(id);
//         if (suite == null) {
//             return ResponseEntity.notFound().build();
//         }

//         try {
//             byte[] csvContent = reportService.generateCsvReport(id);
//             ByteArrayResource resource = new ByteArrayResource(csvContent);

//             return ResponseEntity.ok()
//                     .header(HttpHeaders.CONTENT_DISPOSITION,
//                             "attachment; filename=suite-" + id + "-report.csv")
//                     .contentType(MediaType.parseMediaType("text/csv"))
//                     .contentLength(csvContent.length)
//                     .body(resource);
//         } catch (Exception e) {
//             return ResponseEntity.internalServerError().build();
//         }
//     }
// }

package com.example.test_framework_api.controller;

import com.example.test_framework_api.dto.TestSuiteRequest;
import com.example.test_framework_api.dto.TestCaseExecutionRequest;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestSuiteService;
import com.example.test_framework_api.service.ProduceReportHtmlService;
import com.example.test_framework_api.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

import static com.example.test_framework_api.config.RabbitMQConfig.TEST_SUITE_QUEUE;

/**
 * ANALYTICS ENHANCED: Advanced reporting features
 * - Trend analysis (pass rate over time)
 * - Flaky test detection (high retry count)
 * - Export formats (PDF, CSV)
 */
@RestController
@RequestMapping("/api/suites")
@RequiredArgsConstructor
public class TestSuiteController {

    private final TestSuiteService suiteService;
    private final TestRunService runService;
    private final RabbitTemplate rabbitTemplate;
    private final ProduceReportHtmlService reportService;
    private final MetricsService metricsService;

    @PostMapping("/import-csv")
    public ResponseEntity<TestSuite> importSuite(@ModelAttribute TestSuiteRequest request) {
        try {
            TestSuite suite = suiteService.importFromCsv(request.getCsvFile(),
                    request.getSuiteName(),
                    request.getDescription());
            return ResponseEntity.ok(suite);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<TestSuite>> getSuites() {
        return ResponseEntity.ok(suiteService.getAllSuites());
    }

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
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "parallelThreads must be at least 1") @Max(value = 8, message = "parallelThreads cannot exceed 8") int parallelThreads) {
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
     * ANALYTICS: Get trend analysis for suite (pass rate over time).
     *
     * @param id   Suite ID
     * @param days Number of days to analyze (default: 7)
     * @return Pass rate trends with flaky test indicators
     */
    @GetMapping("/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getSuiteAnalytics(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days) {

        TestSuite suite = suiteService.getSuiteById(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }

        // Get trends and flaky tests
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
                        "data", trends),
                "flakyTests", Map.of(
                        "count", flakyTests.size(),
                        "tests", flakyTests)));
    }

    /**
     * ANALYTICS: Export suite report as PDF.
     *
     * @param id Suite ID
     * @return PDF file download
     */
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<Resource> exportPdf(@PathVariable Long id) {
        TestSuite suite = suiteService.getSuiteById(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] pdfContent = reportService.generatePdfReport(id);
            ByteArrayResource resource = new ByteArrayResource(pdfContent);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=suite-" + id + "-report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ANALYTICS: Export suite report as CSV.
     *
     * @param id Suite ID
     * @return CSV file download
     */
    @GetMapping("/{id}/export/csv")
    public ResponseEntity<Resource> exportCsv(@PathVariable Long id) {
        TestSuite suite = suiteService.getSuiteById(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] csvContent = reportService.generateCsvReport(id);
            ByteArrayResource resource = new ByteArrayResource(csvContent);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=suite-" + id + "-report.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(csvContent.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ENHANCED: Get HTML report with embedded analytics.
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<String> getSuiteReport(@PathVariable Long id) {
        try {
            // Get trends for chart
            List<Map<String, Object>> trends = metricsService.getTrends(id, 7);

            // Generate HTML with analytics
            String reportPath = reportService.generateSuiteReportWithAnalytics(id,
                    trends);
            return ResponseEntity.ok(reportPath);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}