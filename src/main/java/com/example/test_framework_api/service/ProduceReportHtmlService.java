// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestSuite;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.repository.TestRunRepository;
// import com.example.test_framework_api.repository.TestSuiteRepository;
// import com.example.test_framework_api.repository.TestResultRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;

// import java.io.File;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardOpenOption;
// import java.util.List;
// import java.util.Map;
// // import java.util.ArrayList;
// import java.util.stream.Collectors;

// /**
//  * CRITICAL FIXES:
//  * 1. Added execution validation - reports only generate if suite has been executed
//  * 2. Fetch results by test_suite_id instead of test_run_id
//  * 3. Return actual HTML for PDF (not binary PDF)
//  * 4. Proper error messages when suite not executed
//  */
// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class ProduceReportHtmlService {

//     private final TestSuiteRepository suiteRepository;
//     private final TestRunRepository runRepository;
//     private final TestResultRepository resultRepository;

//     public String generateReport() {
//         List<TestRun> runs = runRepository.findAll();
//         Long latestRunId = runs.isEmpty() ? -1L : runs.get(runs.size() - 1).getId();
//         return generateReport(latestRunId);
//     }

//     public String generateReport(Long runId) {
//         TestRun run = runRepository.findById(runId)
//                 .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
//         String reportPath = "target/allure-results-run-" + runId;
//         new File(reportPath).mkdirs();

//         try {
//             ProcessBuilder pb = new ProcessBuilder("mvn", "allure:generate",
//                     "-Dallure.results.directory=" + reportPath,
//                     "-Dallure.report.directory=target/allure-report-run-" + runId);
//             pb.inheritIO();
//             pb.start().waitFor();
//         } catch (Exception e) {
//             log.error("Maven Allure failed for run {}: {}", runId, e.getMessage());
//             throw new RuntimeException("Allure generation failed", e);
//         }

//         run.setReportPath("target/allure-report-run-" + runId);
//         runRepository.save(run);
//         return reportPath;
//     }

//     /**
//      * FIXED: Validate execution before generating standard HTML report
//      */
//     public String generateSuiteReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         // CRITICAL: Check if suite has been executed
//         List<TestResult> results = resultRepository.findByTestSuiteId(suiteId);
//         if (results.isEmpty()) {
//             throw new IllegalStateException(
//                 "Suite " + suiteId + " has not been executed yet. Execute the suite before generating reports."
//             );
//         }

//         suite.updateStatusFromResults();
//         suiteRepository.save(suite);

//         String reportPath = "reports/suite-" + suiteId;
//         new File(reportPath).mkdirs();

//         // Generate standard HTML report
//         String htmlContent = generateSuiteHtml(suite, null);
//         Path htmlFilePath = Paths.get(reportPath, "suite-report.html");

//         try {
//             Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
//                     StandardOpenOption.TRUNCATE_EXISTING);
//             log.info("Generated HTML report at: {}", htmlFilePath.toAbsolutePath());
//         } catch (Exception e) {
//             log.error("Failed to write suite HTML: {}", e.getMessage());
//             throw new RuntimeException("Failed to write suite HTML", e);
//         }

//         suite.setReportPath(htmlFilePath.toAbsolutePath().toString());
//         suiteRepository.save(suite);
//         return suite.getReportPath();
//     }

//     /**
//      * FIXED: Validate execution before generating analytics HTML
//      */
//     public String generateSuiteReportWithAnalytics(Long suiteId, List<Map<String, Object>> trends) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         // CRITICAL: Check if suite has been executed
//         List<TestResult> results = resultRepository.findByTestSuiteId(suiteId);
//         if (results.isEmpty()) {
//             throw new IllegalStateException(
//                 "Suite " + suiteId + " has not been executed yet. Execute the suite before generating analytics."
//             );
//         }

//         String reportPath = "reports/suite-" + suiteId;
//         new File(reportPath).mkdirs();

//         String htmlContent = generateSuiteHtml(suite, trends);
//         Path htmlFilePath = Paths.get(reportPath, "suite-analytics.html");

//         try {
//             Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
//                     StandardOpenOption.TRUNCATE_EXISTING);
//             log.info("Generated analytics report at: {}", htmlFilePath.toAbsolutePath());
//         } catch (Exception e) {
//             throw new RuntimeException("Failed to write analytics HTML", e);
//         }

//         return htmlFilePath.toAbsolutePath().toString();
//     }

//     /**
//      * CRITICAL FIX: Return HTML (not binary PDF) and validate execution
//      * Browser will render as "PDF" when Content-Type is set to application/pdf
//      */
//     public byte[] generatePdfReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         // CRITICAL: Fetch results by test_suite_id
//         List<TestResult> results = resultRepository.findByTestSuiteId(suiteId);
        
//         if (results.isEmpty()) {
//             throw new IllegalStateException(
//                 "Suite " + suiteId + " has not been executed yet. Execute the suite before generating PDF."
//             );
//         }

//         log.info("Generating PDF for suite {}: Found {} results", suiteId, results.size());

//         // Generate HTML (browser renders as PDF)
//         String html = generatePdfHtml(suite, results);

//         return html.getBytes(StandardCharsets.UTF_8);
//     }

//     /**
//      * CRITICAL FIX: Fetch results by test_suite_id and validate execution
//      */
//     public byte[] generateCsvReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         // CRITICAL: Fetch results by test_suite_id
//         List<TestResult> results = resultRepository.findByTestSuiteId(suiteId);
        
//         if (results.isEmpty()) {
//             throw new IllegalStateException(
//                 "Suite " + suiteId + " has not been executed yet. Execute the suite before generating CSV."
//             );
//         }

//         log.info("Generating CSV for suite {}: Found {} results", suiteId, results.size());

//         StringBuilder csv = new StringBuilder();
//         csv.append("Case ID,Test Name,Type,Status,Duration (ms),Retry Count,Error Message,Created At\n");

//         for (TestCase tc : suite.getTestCases()) {
//             // Find matching result
//             TestResult result = results.stream()
//                     .filter(r -> r.getTestName().equals(tc.getTestName()))
//                     .findFirst()
//                     .orElse(null);

//             if (result != null) {
//                 String status = result.getStatus().toString();
//                 long duration = result.getDuration() != null ? result.getDuration() : 0;
//                 int retries = result.getRetryCount() != null ? result.getRetryCount() : 0;
//                 String errorMsg = result.getErrorMessage() != null ? 
//                     result.getErrorMessage().replace("\"", "\"\"") : ""; // Escape quotes
//                 String createdAt = result.getCreatedAt() != null ? 
//                     result.getCreatedAt().toString() : "N/A";

//                 csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,\"%s\",\"%s\"\n",
//                         tc.getTestCaseId(),
//                         tc.getTestName(),
//                         tc.getTestType(),
//                         status,
//                         duration,
//                         retries,
//                         errorMsg,
//                         createdAt));
//             }
//         }

//         return csv.toString().getBytes(StandardCharsets.UTF_8);
//     }

//     /**
//      * FIXED: Generate PDF-friendly HTML with actual results
//      */
//     private String generatePdfHtml(TestSuite suite, List<TestResult> results) {
//         long total = suite.getTestCases().size();
//         long passed = results.stream()
//                 .filter(r -> r.getStatus() == TestStatus.PASSED)
//                 .count();
//         long failed = results.stream()
//                 .filter(r -> r.getStatus() == TestStatus.FAILED)
//                 .count();
//         double passRate = total > 0 ? (passed * 100.0 / total) : 0;

//         StringBuilder html = new StringBuilder();
//         html.append("<!DOCTYPE html><html><head>");
//         html.append("<meta charset=\"UTF-8\">");
//         html.append("<title>Suite Report: ").append(suite.getName()).append("</title>");
//         html.append("<style>");
//         html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
//         html.append("h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }");
//         html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
//         html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
//         html.append("th { background-color: #4CAF50; color: white; }");
//         html.append(".passed { background-color: #d4edda; }");
//         html.append(".failed { background-color: #f8d7da; }");
//         html.append(".summary { background: #f5f5f5; padding: 20px; margin: 20px 0; border-radius: 5px; }");
//         html.append(".summary p { margin: 8px 0; }");
//         html.append("</style></head><body>");

//         html.append("<h1>Test Suite Report: ").append(suite.getName()).append("</h1>");

//         html.append("<div class='summary'>");
//         html.append("<h2>Summary</h2>");
//         html.append("<p><strong>Suite ID:</strong> ").append(suite.getId()).append("</p>");
//         html.append("<p><strong>Suite Status:</strong> ").append(suite.getStatus()).append("</p>");
//         html.append("<p><strong>Total Cases:</strong> ").append(total).append("</p>");
//         html.append("<p><strong>Passed:</strong> ").append(passed).append("</p>");
//         html.append("<p><strong>Failed:</strong> ").append(failed).append("</p>");
//         html.append("<p><strong>Pass Rate:</strong> ").append(String.format("%.2f%%", passRate)).append("</p>");
//         html.append("</div>");

//         html.append("<h2>Test Cases</h2>");
//         html.append("<table>");
//         html.append("<tr><th>Case ID</th><th>Name</th><th>Type</th><th>Status</th><th>Duration (ms)</th><th>Retries</th><th>Error</th></tr>");

//         for (TestCase tc : suite.getTestCases()) {
//             TestResult result = results.stream()
//                     .filter(r -> r.getTestName().equals(tc.getTestName()))
//                     .findFirst()
//                     .orElse(null);

//             if (result != null) {
//                 String status = result.getStatus().toString();
//                 String statusClass = status.equals("PASSED") ? "passed" : "failed";
//                 long duration = result.getDuration() != null ? result.getDuration() : 0;
//                 int retries = result.getRetryCount() != null ? result.getRetryCount() : 0;
//                 String errorMsg = result.getErrorMessage() != null ? 
//                     result.getErrorMessage().substring(0, Math.min(100, result.getErrorMessage().length())) : "";

//                 html.append("<tr class='").append(statusClass).append("'>");
//                 html.append("<td>").append(tc.getTestCaseId()).append("</td>");
//                 html.append("<td>").append(tc.getTestName()).append("</td>");
//                 html.append("<td>").append(tc.getTestType()).append("</td>");
//                 html.append("<td>").append(status).append("</td>");
//                 html.append("<td>").append(duration).append("</td>");
//                 html.append("<td>").append(retries).append("</td>");
//                 html.append("<td>").append(errorMsg).append("</td>");
//                 html.append("</tr>");
//             }
//         }

//         html.append("</table>");
//         html.append("<p style='margin-top: 30px; color: #666;'>Generated at: ").append(java.time.LocalDateTime.now()).append("</p>");
//         html.append("</body></html>");
//         return html.toString();
//     }

//     /**
//      * FIXED: Generate HTML with optional trend chart, using test_suite_id results
//      */
//     private String generateSuiteHtml(TestSuite suite, List<Map<String, Object>> trends) {
//         // CRITICAL: Get results by test_suite_id
//         List<TestResult> results = resultRepository.findByTestSuiteId(suite.getId());

//         long total = suite.getTestCases().size();
//         long passed = results.stream()
//                 .filter(r -> r.getStatus() == TestStatus.PASSED)
//                 .count();
//         long failed = results.stream()
//                 .filter(r -> r.getStatus() == TestStatus.FAILED)
//                 .count();
//         double passRate = total > 0 ? (passed * 100.0 / total) : 0;

//         StringBuilder html = new StringBuilder();
//         html.append("<!DOCTYPE html><html><head>");
//         html.append("<meta charset=\"UTF-8\">");
//         html.append("<title>Suite Report: ").append(suite.getName()).append("</title>");

//         if (trends != null && !trends.isEmpty()) {
//             html.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>");
//         }

//         html.append("<style>");
//         html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
//         html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
//         html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
//         html.append("th { background-color: #4CAF50; color: white; }");
//         html.append(".passed { background-color: #d4edda; }");
//         html.append(".failed { background-color: #f8d7da; }");
//         html.append(".chart-container { width: 80%; margin: 30px auto; }");
//         html.append("</style></head><body>");

//         html.append("<h1>Suite Report: ").append(suite.getName()).append("</h1>");
//         html.append("<p><strong>Status:</strong> ").append(suite.getStatus()).append("</p>");
//         html.append("<p><strong>Total Cases:</strong> ").append(total);
//         html.append(" | <strong>Passed:</strong> ").append(passed);
//         html.append(" | <strong>Failed:</strong> ").append(failed);
//         html.append(" | <strong>Rate:</strong> ").append(String.format("%.2f%%", passRate)).append("</p>");

//         // Trend chart (if analytics mode)
//         if (trends != null && !trends.isEmpty()) {
//             html.append("<div class=\"chart-container\">");
//             html.append("<h2>Pass Rate Trend</h2>");
//             html.append("<canvas id=\"trendChart\"></canvas>");
//             html.append("</div>");
//             html.append("<script>");
//             html.append("const ctx = document.getElementById('trendChart').getContext('2d');");
//             html.append("new Chart(ctx, {");
//             html.append("type: 'line',");
//             html.append("data: {");
//             html.append("labels: [");
//             html.append(trends.stream()
//                     .map(t -> "'" + t.get("date") + "'")
//                     .collect(Collectors.joining(",")));
//             html.append("],");
//             html.append("datasets: [{");
//             html.append("label: 'Pass Rate (%)',");
//             html.append("data: [");
//             html.append(trends.stream()
//                     .map(t -> t.get("passRate").toString())
//                     .collect(Collectors.joining(",")));
//             html.append("],");
//             html.append("borderColor: 'rgb(75, 192, 192)',");
//             html.append("tension: 0.1");
//             html.append("}]},");
//             html.append("options: { responsive: true, scales: { y: { beginAtZero: true, max: 100 } } }");
//             html.append("});");
//             html.append("</script>");
//         }

//         html.append("<table><tr><th>Case ID</th><th>Name</th><th>Type</th><th>Status</th><th>Duration</th><th>Retries</th></tr>");

//         for (TestCase tc : suite.getTestCases()) {
//             TestResult result = results.stream()
//                     .filter(r -> r.getTestName().equals(tc.getTestName()))
//                     .findFirst()
//                     .orElse(null);

//             if (result != null) {
//                 String status = result.getStatus().toString();
//                 String statusClass = status.equals("PASSED") ? "passed" : "failed";
//                 long duration = result.getDuration() != null ? result.getDuration() : 0;
//                 int retries = result.getRetryCount() != null ? result.getRetryCount() : 0;

//                 html.append("<tr class='").append(statusClass).append("'>");
//                 html.append("<td>").append(tc.getTestCaseId()).append("</td>");
//                 html.append("<td>").append(tc.getTestName()).append("</td>");
//                 html.append("<td>").append(tc.getTestType()).append("</td>");
//                 html.append("<td>").append(status).append("</td>");
//                 html.append("<td>").append(duration).append(" ms</td>");
//                 html.append("<td>").append(retries).append("</td>");
//                 html.append("</tr>");
//             }
//         }

//         html.append("</table></body></html>");
//         return html.toString();
//     }
// }
// package com.example.test_framework_api.service;
// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestSuite;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.repository.TestRunRepository;
// import com.example.test_framework_api.repository.TestSuiteRepository;
// import com.example.test_framework_api.repository.TestResultRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;

// import java.io.File;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardOpenOption;
// import java.util.List;
// import java.util.Map;
// import java.util.ArrayList;
// import java.util.stream.Collectors;

// /**
//  * FIXED: Report generation with proper PDF/CSV export and Allure integration
//  */
// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class ProduceReportHtmlService {

//     private final TestSuiteRepository suiteRepository;
//     private final TestRunRepository runRepository;
//     private final TestResultRepository resultRepository;

//     public String generateReport() {
//         List<TestRun> runs = runRepository.findAll();
//         Long latestRunId = runs.isEmpty() ? -1L : runs.get(runs.size() - 1).getId();
//         return generateReport(latestRunId);
//     }

//     public String generateReport(Long runId) {
//         TestRun run = runRepository.findById(runId)
//                 .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
//         String reportPath = "target/allure-results-run-" + runId;
//         new File(reportPath).mkdirs();

//         try {
//             ProcessBuilder pb = new ProcessBuilder("mvn", "allure:generate",
//                     "-Dallure.results.directory=" + reportPath,
//                     "-Dallure.report.directory=target/allure-report-run-" + runId);
//             pb.inheritIO();
//             pb.start().waitFor();
//         } catch (Exception e) {
//             log.error("Maven Allure failed for run {}: {}", runId, e.getMessage());
//             throw new RuntimeException("Allure generation failed", e);
//         }

//         run.setReportPath("target/allure-report-run-" + runId);
//         runRepository.save(run);
//         return reportPath;
//     }

//     /**
//      * FIXED: Generate standard HTML report (NOT analytics)
//      */
//     public String generateSuiteReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         suite.updateStatusFromResults();
//         suiteRepository.save(suite);

//         String reportPath = "reports/suite-" + suiteId;
//         new File(reportPath).mkdirs();

//         // Generate standard HTML report
//         String htmlContent = generateSuiteHtml(suite, null);
//         Path htmlFilePath = Paths.get(reportPath, "suite-report.html");

//         try {
//             Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
//                     StandardOpenOption.TRUNCATE_EXISTING);
//             log.info("Generated HTML report at: {}", htmlFilePath.toAbsolutePath());
//         } catch (Exception e) {
//             log.error("Failed to write suite HTML: {}", e.getMessage());
//             throw new RuntimeException("Failed to write suite HTML", e);
//         }

//         suite.setReportPath(htmlFilePath.toAbsolutePath().toString());
//         suiteRepository.save(suite);
//         return suite.getReportPath();
//     }

//     /**
//      * FIXED: Generate analytics HTML (separate from standard report)
//      */
//     public String generateSuiteReportWithAnalytics(Long suiteId, List<Map<String, Object>> trends) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         String reportPath = "reports/suite-" + suiteId;
//         new File(reportPath).mkdirs();

//         String htmlContent = generateSuiteHtml(suite, trends);
//         Path htmlFilePath = Paths.get(reportPath, "suite-analytics.html");

//         try {
//             Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
//                     StandardOpenOption.TRUNCATE_EXISTING);
//             log.info("Generated analytics report at: {}", htmlFilePath.toAbsolutePath());
//         } catch (Exception e) {
//             throw new RuntimeException("Failed to write analytics HTML", e);
//         }

//         return htmlFilePath.toAbsolutePath().toString();
//     }

//     /**
//      * FIXED: Generate proper PDF with actual test case details
//      */
//     public byte[] generatePdfReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         // Get actual test results
//         List<TestResult> results = new ArrayList<>();
//         if (suite.getTestRun() != null) {
//             results = resultRepository.findByTestRunId(suite.getTestRun().getId());
//         }

//         // Generate HTML first (easier to convert to PDF)
//         String html = generatePdfHtml(suite, results);

//         // Return as bytes (browser can render HTML as "PDF")
//         return html.getBytes(StandardCharsets.UTF_8);
//     }

//     /**
//      * FIXED: Generate CSV with actual test results
//      */
//     public byte[] generateCsvReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

//         // Get actual test results
//         List<TestResult> results = new ArrayList<>();
//         if (suite.getTestRun() != null) {
//             results = resultRepository.findByTestRunId(suite.getTestRun().getId());
//         }

//         StringBuilder csv = new StringBuilder();
//         csv.append("Case ID,Test Name,Type,Status,Duration (ms),Retry Count,Created At\n");

//         for (TestCase tc : suite.getTestCases()) {
//             // Find matching result
//             TestResult result = results.stream()
//                     .filter(r -> r.getTestName().equals(tc.getTestName()))
//                     .findFirst()
//                     .orElse(null);

//             String status = result != null ? result.getStatus().toString() : "PENDING";
//             long duration = result != null && result.getDuration() != null ? result.getDuration() : 0;
//             int retries = result != null && result.getRetryCount() != null ? result.getRetryCount() : 0;
//             String createdAt = result != null && result.getCreatedAt() != null ? result.getCreatedAt().toString()
//                     : "N/A";

//             csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,\"%s\"\n",
//                     tc.getTestCaseId(),
//                     tc.getTestName(),
//                     tc.getTestType(),
//                     status,
//                     duration,
//                     retries,
//                     createdAt));
//         }

//         return csv.toString().getBytes(StandardCharsets.UTF_8);
//     }

//     /**
//      * Generate PDF-friendly HTML
//      */
//     private String generatePdfHtml(TestSuite suite, List<TestResult> results) {
//         long total = suite.getTestCases().size();
//         long passed = results.stream()
//                 .filter(r -> r.getStatus() == TestStatus.PASSED)
//                 .count();
//         double passRate = total > 0 ? (passed * 100.0 / total) : 0;

//         StringBuilder html = new StringBuilder();
//         html.append("<!DOCTYPE html><html><head><title>Suite Report: ").append(suite.getName()).append("</title>");
//         html.append("<style>");
//         html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
//         html.append("h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }");
//         html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
//         html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
//         html.append("th { background-color: #4CAF50; color: white; }");
//         html.append(".passed { background-color: #d4edda; }");
//         html.append(".failed { background-color: #f8d7da; }");
//         html.append(".pending { background-color: #fff3cd; }");
//         html.append(".summary { background: #f5f5f5; padding: 20px; margin: 20px 0; border-radius: 5px; }");
//         html.append("</style></head><body>");

//         html.append("<h1>Test Suite Report: ").append(suite.getName()).append("</h1>");

//         html.append("<div class='summary'>");
//         html.append("<h2>Summary</h2>");
//         html.append("<p><strong>Suite Status:</strong> ").append(suite.getStatus()).append("</p>");
//         html.append("<p><strong>Total Cases:</strong> ").append(total).append("</p>");
//         html.append("<p><strong>Passed:</strong> ").append(passed).append("</p>");
//         html.append("<p><strong>Failed:</strong> ").append(total - passed).append("</p>");
//         html.append("<p><strong>Pass Rate:</strong> ").append(String.format("%.2f%%", passRate)).append("</p>");
//         html.append("</div>");

//         html.append("<h2>Test Cases</h2>");
//         html.append("<table>");
//         html.append(
//                 "<tr><th>Case ID</th><th>Name</th><th>Type</th><th>Status</th><th>Duration (ms)</th><th>Retries</th></tr>");

//         for (TestCase tc : suite.getTestCases()) {
//             TestResult result = results.stream()
//                     .filter(r -> r.getTestName().equals(tc.getTestName()))
//                     .findFirst()
//                     .orElse(null);

//             String status = result != null ? result.getStatus().toString() : "PENDING";
//             String statusClass = status.equals("PASSED") ? "passed" : status.equals("FAILED") ? "failed" : "pending";
//             long duration = result != null && result.getDuration() != null ? result.getDuration() : 0;
//             int retries = result != null && result.getRetryCount() != null ? result.getRetryCount() : 0;

//             html.append("<tr class='").append(statusClass).append("'>");
//             html.append("<td>").append(tc.getTestCaseId()).append("</td>");
//             html.append("<td>").append(tc.getTestName()).append("</td>");
//             html.append("<td>").append(tc.getTestType()).append("</td>");
//             html.append("<td>").append(status).append("</td>");
//             html.append("<td>").append(duration).append("</td>");
//             html.append("<td>").append(retries).append("</td>");
//             html.append("</tr>");
//         }

//         html.append("</table></body></html>");
//         return html.toString();
//     }

//     /**
//      * Generate HTML with optional trend chart
//      */
//     private String generateSuiteHtml(TestSuite suite, List<Map<String, Object>> trends) {
//         // Get actual test results
//         List<TestResult> results = new ArrayList<>();
//         if (suite.getTestRun() != null) {
//             results = resultRepository.findByTestRunId(suite.getTestRun().getId());
//         }

//         long total = suite.getTestCases().size();
//         long passed = results.stream()
//                 .filter(r -> r.getStatus() == TestStatus.PASSED)
//                 .count();
//         double passRate = total > 0 ? (passed * 100.0 / total) : 0;

//         StringBuilder html = new StringBuilder();
//         html.append("<!DOCTYPE html><html><head><title>Suite Report: ").append(suite.getName()).append("</title>");

//         if (trends != null && !trends.isEmpty()) {
//             html.append("<script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>");
//         }

//         html.append("<style>");
//         html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
//         html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
//         html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
//         html.append("th { background-color: #4CAF50; color: white; }");
//         html.append(".passed { background-color: #d4edda; }");
//         html.append(".failed { background-color: #f8d7da; }");
//         html.append(".pending { background-color: #fff3cd; }");
//         html.append(".chart-container { width: 80%; margin: 30px auto; }");
//         html.append("</style></head><body>");

//         html.append("<h1>Suite Report: ").append(suite.getName()).append("</h1>");
//         html.append("<p><strong>Status:</strong> ").append(suite.getStatus()).append("</p>");
//         html.append("<p><strong>Total Cases:</strong> ").append(total);
//         html.append(" | <strong>Passed:</strong> ").append(passed);
//         html.append(" | <strong>Rate:</strong> ").append(String.format("%.2f%%", passRate)).append("</p>");

//         // Trend chart (if analytics mode)
//         if (trends != null && !trends.isEmpty()) {
//             html.append("<div class=\"chart-container\">");
//             html.append("<h2>Pass Rate Trend</h2>");
//             html.append("<canvas id=\"trendChart\"></canvas>");
//             html.append("</div>");
//             html.append("<script>");
//             html.append("const ctx = document.getElementById('trendChart').getContext('2d');");
//             html.append("new Chart(ctx, {");
//             html.append("type: 'line',");
//             html.append("data: {");
//             html.append("labels: [");
//             html.append(trends.stream()
//                     .map(t -> "'" + t.get("date") + "'")
//                     .collect(Collectors.joining(",")));
//             html.append("],");
//             html.append("datasets: [{");
//             html.append("label: 'Pass Rate (%)',");
//             html.append("data: [");
//             html.append(trends.stream()
//                     .map(t -> t.get("passRate").toString())
//                     .collect(Collectors.joining(",")));
//             html.append("],");
//             html.append("borderColor: 'rgb(75, 192, 192)',");
//             html.append("tension: 0.1");
//             html.append("}]},");
//             html.append("options: { responsive: true, scales: { y: { beginAtZero: true, max: 100 } } }");
//             html.append("});");
//             html.append("</script>");
//         }

//         html.append("<table><tr><th>Case ID</th><th>Name</th><th>Type</th><th>Status</th><th>Duration</th></tr>");

//         for (TestCase tc : suite.getTestCases()) {
//             TestResult result = results.stream()
//                     .filter(r -> r.getTestName().equals(tc.getTestName()))
//                     .findFirst()
//                     .orElse(null);

//             String status = result != null ? result.getStatus().toString() : "PENDING";
//             String statusClass = status.equals("PASSED") ? "passed" : status.equals("FAILED") ? "failed" : "pending";
//             long duration = result != null && result.getDuration() != null ? result.getDuration() : 0;

//             html.append("<tr class='").append(statusClass).append("'>");
//             html.append("<td>").append(tc.getTestCaseId()).append("</td>");
//             html.append("<td>").append(tc.getTestName()).append("</td>");
//             html.append("<td>").append(tc.getTestType()).append("</td>");
//             html.append("<td>").append(status).append("</td>");
//             html.append("<td>").append(duration).append(" ms</td>");
//             html.append("</tr>");
//         }

//         html.append("</table></body></html>");
//         return html.toString();
//     }
// }
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestRunRepository;
import com.example.test_framework_api.repository.TestSuiteRepository;
import com.example.test_framework_api.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
// import java.util.Map;
import java.util.ArrayList;
// import java.util.stream.Collectors;

/**
 * SIMPLIFIED & ROBUST:
 * 1. Handles both old suites (no test_suite_id) and new suites (with test_suite_id)
 * 2. Simple PDF format - just a table with basic info
 * 3. Proper error messages for unexecuted suites
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProduceReportHtmlService {

    private final TestSuiteRepository suiteRepository;
    private final TestRunRepository runRepository;
    private final TestResultRepository resultRepository;

    public String generateReport() {
        List<TestRun> runs = runRepository.findAll();
        Long latestRunId = runs.isEmpty() ? -1L : runs.get(runs.size() - 1).getId();
        return generateReport(latestRunId);
    }

    public String generateReport(Long runId) {
        TestRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        String reportPath = "target/allure-results-run-" + runId;
        new File(reportPath).mkdirs();

        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "allure:generate",
                    "-Dallure.results.directory=" + reportPath,
                    "-Dallure.report.directory=target/allure-report-run-" + runId);
            pb.inheritIO();
            pb.start().waitFor();
        } catch (Exception e) {
            log.error("Maven Allure failed for run {}: {}", runId, e.getMessage());
            throw new RuntimeException("Allure generation failed", e);
        }

        run.setReportPath("target/allure-report-run-" + runId);
        runRepository.save(run);
        return reportPath;
    }

    /**
     * SIMPLIFIED: Get results by suite_id OR run_id (handles old & new suites)
     */
    public String generateSuiteReport(Long suiteId) {
        TestSuite suite = suiteRepository.findById(suiteId)
                .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

        // Try to get results - handles both old and new suites
        List<TestResult> results = getResultsForSuite(suite);
        
        if (results.isEmpty()) {
            String errorMsg = "Suite " + suiteId + " has not been executed yet. " +
                "Please execute the suite using POST /api/suites/" + suiteId + "/execute before generating reports.";
            log.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        log.info("Generating HTML report for suite {}: Found {} results", suiteId, results.size());

        String reportPath = "reports/suite-" + suiteId;
        new File(reportPath).mkdirs();

        String htmlContent = generateSimpleHtml(suite, results);
        Path htmlFilePath = Paths.get(reportPath, "suite-report.html");

        try {
            Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Generated HTML report at: {}", htmlFilePath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to write suite HTML: {}", e.getMessage());
            throw new RuntimeException("Failed to write suite HTML", e);
        }

        suite.setReportPath(htmlFilePath.toAbsolutePath().toString());
        suiteRepository.save(suite);
        return suite.getReportPath();
    }

    /**
     * SIMPLIFIED: Generate simple PDF-style HTML
     */
    // public byte[] generatePdfReport(Long suiteId) {
    //     TestSuite suite = suiteRepository.findById(suiteId)
    //             .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

    //     // Try to get results - handles both old and new suites
    //     List<TestResult> results = getResultsForSuite(suite);
        
    //     if (results.isEmpty()) {
    //         throw new IllegalStateException(
    //             "Suite " + suiteId + " has not been executed yet. Execute the suite before generating PDF."
    //         );
    //     }

    //     log.info("Generating PDF for suite {}: Found {} results", suiteId, results.size());

    //     String html = generateSimplePdfHtml(suite, results);
    //     return html.getBytes(StandardCharsets.UTF_8);
    // }

    /**
     * SIMPLIFIED: Generate CSV with basic info
     */
    public byte[] generateCsvReport(Long suiteId) {
        TestSuite suite = suiteRepository.findById(suiteId)
                .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

        // Try to get results - handles both old and new suites
        List<TestResult> results = getResultsForSuite(suite);
        
        if (results.isEmpty()) {
            throw new IllegalStateException(
                "Suite " + suiteId + " has not been executed yet. Execute the suite before generating CSV."
            );
        }

        log.info("Generating CSV for suite {}: Found {} results", suiteId, results.size());

        StringBuilder csv = new StringBuilder();
        csv.append("Case ID,Test Name,Type,Status,Duration (ms)\n");

        for (TestCase tc : suite.getTestCases()) {
            TestResult result = results.stream()
                    .filter(r -> r.getTestName().equals(tc.getTestName()))
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                String status = result.getStatus().toString();
                long duration = result.getDuration() != null ? result.getDuration() : 0;

                csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d\n",
                        tc.getTestCaseId(),
                        tc.getTestName(),
                        tc.getTestType(),
                        status,
                        duration));
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * CRITICAL: Get results for suite - handles both old and new suites
     * Old suites: Get by test_run_id
     * New suites: Get by test_suite_id
     */
    private List<TestResult> getResultsForSuite(TestSuite suite) {
        // Try new way first (test_suite_id)
        List<TestResult> results = resultRepository.findByTestSuiteId(suite.getId());
        
        if (!results.isEmpty()) {
            log.debug("Found {} results by test_suite_id for suite {}", results.size(), suite.getId());
            return results;
        }
        
        // Fallback to old way (test_run_id) for legacy suites
        if (suite.getTestRun() != null) {
            results = resultRepository.findByTestRunId(suite.getTestRun().getId());
            log.debug("Found {} results by test_run_id for suite {}", results.size(), suite.getId());
            return results;
        }
        
        log.warn("No results found for suite {} (tried both test_suite_id and test_run_id)", suite.getId());
        return new ArrayList<>();
    }

    /**
     * SIMPLIFIED: Generate basic HTML with just essential info
     */
    private String generateSimpleHtml(TestSuite suite, List<TestResult> results) {
        long total = suite.getTestCases().size();
        long passed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.PASSED)
                .count();
        long failed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.FAILED)
                .count();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Suite Report: ").append(suite.getName()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
        html.append(".container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }");
        html.append(".summary { background: #e8f5e9; padding: 20px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #4CAF50; }");
        html.append(".summary p { margin: 8px 0; font-size: 16px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
        html.append("th { background-color: #4CAF50; color: white; font-weight: bold; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append(".passed { background-color: #d4edda !important; }");
        html.append(".failed { background-color: #f8d7da !important; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<h1>Test Suite Report: ").append(suite.getName()).append("</h1>");

        html.append("<div class='summary'>");
        html.append("<p><strong>Total Cases:</strong> ").append(total);
        html.append(" | <strong>Passed:</strong> ").append(passed);
        html.append(" | <strong>Failed:</strong> ").append(failed);
        html.append(" | <strong>Pass Rate:</strong> ").append(String.format("%.2f%%", passRate)).append("</p>");
        html.append("</div>");

        html.append("<table>");
        html.append("<tr><th>Case ID</th><th>Test Name</th><th>Type</th><th>Status</th><th>Duration (ms)</th></tr>");

        for (TestCase tc : suite.getTestCases()) {
            TestResult result = results.stream()
                    .filter(r -> r.getTestName().equals(tc.getTestName()))
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                String status = result.getStatus().toString();
                String statusClass = status.equals("PASSED") ? "passed" : "failed";
                long duration = result.getDuration() != null ? result.getDuration() : 0;

                html.append("<tr class='").append(statusClass).append("'>");
                html.append("<td>").append(tc.getTestCaseId()).append("</td>");
                html.append("<td>").append(tc.getTestName()).append("</td>");
                html.append("<td>").append(tc.getTestType()).append("</td>");
                html.append("<td><strong>").append(status).append("</strong></td>");
                html.append("<td>").append(duration).append("</td>");
                html.append("</tr>");
            }
        }

        html.append("</table>");
        html.append("<p style='margin-top: 30px; color: #666; text-align: center;'>Generated: ").append(java.time.LocalDateTime.now()).append("</p>");
        html.append("</div>");
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * SIMPLIFIED: Generate PDF-style HTML (simple table format)
     */
    private String generateSimplePdfHtml(TestSuite suite, List<TestResult> results) {
        long total = suite.getTestCases().size();
        long passed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.PASSED)
                .count();
        long failed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.FAILED)
                .count();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Suite Report: ").append(suite.getName()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
        html.append("h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; margin-bottom: 30px; }");
        html.append(".summary { background: #e8f5e9; padding: 20px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #4CAF50; }");
        html.append(".summary p { margin: 10px 0; font-size: 16px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 30px; }");
        html.append("th, td { border: 1px solid #333; padding: 12px; text-align: left; }");
        html.append("th { background-color: #4CAF50; color: white; font-weight: bold; }");
        html.append(".passed { background-color: #d4edda; }");
        html.append(".failed { background-color: #f8d7da; }");
        html.append("</style></head><body>");

        html.append("<h1>Test Suite Report</h1>");
        html.append("<p><strong>Suite Name:</strong> ").append(suite.getName()).append("</p>");

        html.append("<div class='summary'>");
        html.append("<p><strong>Total Cases:</strong> ").append(total).append("</p>");
        html.append("<p><strong>Passed:</strong> ").append(passed).append("</p>");
        html.append("<p><strong>Failed:</strong> ").append(failed).append("</p>");
        html.append("<p><strong>Pass Rate:</strong> ").append(String.format("%.2f%%", passRate)).append("</p>");
        html.append("</div>");

        html.append("<table>");
        html.append("<tr><th>Case ID</th><th>Test Name</th><th>Type</th><th>Status</th><th>Duration (ms)</th></tr>");

        for (TestCase tc : suite.getTestCases()) {
            TestResult result = results.stream()
                    .filter(r -> r.getTestName().equals(tc.getTestName()))
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                String status = result.getStatus().toString();
                String statusClass = status.equals("PASSED") ? "passed" : "failed";
                long duration = result.getDuration() != null ? result.getDuration() : 0;

                html.append("<tr class='").append(statusClass).append("'>");
                html.append("<td>").append(tc.getTestCaseId()).append("</td>");
                html.append("<td>").append(tc.getTestName()).append("</td>");
                html.append("<td>").append(tc.getTestType()).append("</td>");
                html.append("<td><strong>").append(status).append("</strong></td>");
                html.append("<td>").append(duration).append("</td>");
                html.append("</tr>");
            }
        }

        html.append("</table>");
        html.append("<p style='margin-top: 40px; color: #666;'><strong>Generated:</strong> ").append(java.time.LocalDateTime.now()).append("</p>");
        html.append("</body></html>");
        return html.toString();
    }
}