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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

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

    public String generateReportforrun(TestRun run) {
        List<TestResult> results = resultRepository.findByTestRunId(run.getId());

        long total = results.size();
        long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
        long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String formattedDate = run.getCreatedAt().format(formatter);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Test Run Report: ").append(run.getName()).append("</title>");
        html.append("<style>");
        html.append(
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }");
        html.append(
                ".container { max-width: 1200px; margin: 0 auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }");
        html.append("h1 { color: #2d3748; margin: 0 0 10px 0; font-size: 2.5em; }");
        html.append(".subtitle { color: #718096; font-size: 1.1em; margin-bottom: 30px; }");
        html.append(
                ".summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 30px 0; }");
        html.append(
                ".stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px; border-radius: 10px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        html.append(".stat-card.passed { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }");
        html.append(".stat-card.failed { background: linear-gradient(135deg, #ee0979 0%, #ff6a00 100%); }");
        html.append(".stat-card h3 { margin: 0 0 10px 0; font-size: 1em; text-transform: uppercase; opacity: 0.9; }");
        html.append(".stat-card .value { font-size: 3em; font-weight: bold; margin: 10px 0; }");
        html.append(
                ".progress-bar { background: #e2e8f0; border-radius: 10px; height: 30px; margin: 20px 0; overflow: hidden; position: relative; }");
        html.append(
                ".progress-fill { height: 100%; background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%); display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; transition: width 0.3s ease; }");
        html.append(
                "table { border-collapse: collapse; width: 100%; margin-top: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(
                "th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px; text-align: left; font-weight: 600; text-transform: uppercase; font-size: 0.85em; }");
        html.append("td { padding: 15px; border-bottom: 1px solid #e2e8f0; }");
        html.append("tr:hover { background-color: #f7fafc; }");
        html.append(
                ".status-badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-weight: bold; font-size: 0.85em; }");
        html.append(".status-badge.passed { background: #c6f6d5; color: #22543d; }");
        html.append(".status-badge.failed { background: #fed7d7; color: #742a2a; }");
        html.append(
                ".footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #e2e8f0; text-align: center; color: #718096; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<h1>🧪 Test Run Report</h1>");
        html.append("<div class='subtitle'>").append(run.getName()).append("</div>");

        // Summary Cards
        html.append("<div class='summary'>");
        html.append("<div class='stat-card'>");
        html.append("<h3>Total Tests</h3>");
        html.append("<div class='value'>").append(total).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card passed'>");
        html.append("<h3>Passed</h3>");
        html.append("<div class='value'>").append(passed).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card failed'>");
        html.append("<h3>Failed</h3>");
        html.append("<div class='value'>").append(failed).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card'>");
        html.append("<h3>Pass Rate</h3>");
        html.append("<div class='value'>").append(String.format("%.1f%%", passRate)).append("</div>");
        html.append("</div>");
        html.append("</div>");

        // Progress Bar
        html.append("<div class='progress-bar'>");
        html.append("<div class='progress-fill' style='width: ").append(passRate).append("%;'>");
        html.append(String.format("%.1f%% Pass Rate", passRate));
        html.append("</div>");
        html.append("</div>");

        // Results Table
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>Test Name</th>");
        html.append("<th>Status</th>");
        html.append("<th>Duration</th>");
        html.append("<th>Retries</th>");
        html.append("<th>Error Message</th>");
        html.append("</tr></thead>");
        html.append("<tbody>");

        for (TestResult result : results) {
            String status = result.getStatus().toString();
            String statusClass = status.equals("PASSED") ? "passed" : "failed";
            double duration = (double) (result.getDuration() != null ? result.getDuration() : 0) / 1000;
            String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "-";
            if (errorMsg.length() > 100) {
                errorMsg = errorMsg.substring(0, 100) + "...";
            }

            html.append("<tr>");
            html.append("<td><strong>").append(result.getTestName()).append("</strong></td>");
            html.append("<td><span class='status-badge ").append(statusClass).append("'>")
                    .append(status).append("</span></td>");
            html.append("<td>").append(String.format("%.2f s", duration)).append("</td>");
            html.append("<td>").append(result.getRetryCount()).append("</td>");
            html.append("<td style='color: #e53e3e; font-size: 0.9em;'>").append(errorMsg).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>📅 Generated: ").append(formattedDate).append("</p>");
        html.append("<p>🔧 Test Framework API | Run ID: ").append(run.getId()).append("</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    public String generateReport(Long runId) {
        TestRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        
        // Generate simple HTML report
        String reportPath = "reports/run-" + runId;
        new File(reportPath).mkdirs();
        
        String htmlContent = generateReportforrun(run);
        Path htmlFilePath = Paths.get(reportPath, "run-report.html");
        
        try {
            Files.write(htmlFilePath, htmlContent.getBytes(StandardCharsets.UTF_8), 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Generated HTML report at: {}", htmlFilePath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to write run HTML: {}", e.getMessage());
            throw new RuntimeException("Failed to write run HTML", e);
        }
        
        run.setReportPath(htmlFilePath.toAbsolutePath().toString());
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
                    "Please execute the suite using POST /api/suites/" + suiteId
                    + "/execute before generating reports.";
            log.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        log.info("Generating HTML report for suite {}: Found {} results", suiteId, results.size());

        String reportPath = "reports/suite-" + suiteId;
        new File(reportPath).mkdirs();

        String htmlContent = generateSimpleHtml1(suite, results);
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

    public byte[] generateCsvReport(Long suiteId) {
        TestSuite suite = suiteRepository.findById(suiteId)
                .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

        // Try to get results - handles both old and new suites
        List<TestResult> results = getResultsForSuite(suite);

        if (results.isEmpty()) {
            throw new IllegalStateException(
                    "Suite " + suiteId + " has not been executed yet. Execute the suite before generating CSV.");
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
        long total = suite.getTestCases().size() - 1;
        long passed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.PASSED)
                .count();
        long failed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.FAILED)
                .count();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String formattedDate = LocalDateTime.now().format(formatter);
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
    // CRITICAL FIX #8: Proper status-based row coloring
    html.append(".status-passed { background-color: #d4edda !important; color: #155724; font-weight: bold; }");
    html.append(".status-failed { background-color: #f8d7da !important; color: #721c24; font-weight: bold; }");
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
    html.append("<tr><th>Case ID</th><th>Test Name</th><th>Type</th><th>Status</th><th>Duration (s)</th></tr>");

        for (TestCase tc : suite.getTestCases()) {
            TestResult result = results.stream()
                    .filter(r -> r.getTestName().equals(tc.getTestName()))
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                String status = result.getStatus().toString();
                String statusClass = status.equals("PASSED") ? "passed" : "failed";
                double duration = (double) (result.getDuration() != null ? result.getDuration() : 0) / 1000;

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
        html.append("<p style='margin-top: 30px; color: #666; text-align: center;'>Generated: ")
                .append(formattedDate)
                .append("</p>");
        html.append("</div>");
        html.append("</body></html>");
        return html.toString();
    }
    private String generateSimpleHtml1(TestSuite suite, List<TestResult> results) {
        long total = suite.getTestCases().size();
        long passed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.PASSED)
                .count();
        long failed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.FAILED)
                .count();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String formattedDate = LocalDateTime.now().format(formatter);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
    html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Test Run Report: ").append(suite.getName()).append("</title>");
        html.append("<style>");
        html.append(
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }");
        html.append(
                ".container { max-width: 1200px; margin: 0 auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }");
        html.append("h1 { color: #2d3748; margin: 0 0 10px 0; font-size: 2.5em; }");
        html.append(".subtitle { color: #718096; font-size: 1.1em; margin-bottom: 30px; }");
        html.append(
                ".summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 30px 0; }");
        html.append(
                ".stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 25px; border-radius: 10px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        html.append(".stat-card.passed { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }");
        html.append(".stat-card.failed { background: linear-gradient(135deg, #ee0979 0%, #ff6a00 100%); }");
        html.append(".stat-card h3 { margin: 0 0 10px 0; font-size: 1em; text-transform: uppercase; opacity: 0.9; }");
        html.append(".stat-card .value { font-size: 3em; font-weight: bold; margin: 10px 0; }");
        html.append(
                ".progress-bar { background: #e2e8f0; border-radius: 10px; height: 30px; margin: 20px 0; overflow: hidden; position: relative; }");
        html.append(
                ".progress-fill { height: 100%; background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%); display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; transition: width 0.3s ease; }");
        html.append(
                "table { border-collapse: collapse; width: 100%; margin-top: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append(
                "th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 15px; text-align: left; font-weight: 600; text-transform: uppercase; font-size: 0.85em; }");
        html.append("td { padding: 15px; border-bottom: 1px solid #e2e8f0; }");
        html.append("tr:hover { background-color: #f7fafc; }");
        html.append(
                ".status-badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-weight: bold; font-size: 0.85em; }");
        html.append(".status-badge.passed { background: #c6f6d5; color: #22543d; }");
        html.append(".status-badge.failed { background: #fed7d7; color: #742a2a; }");
        html.append(
                ".footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #e2e8f0; text-align: center; color: #718096; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<h1>🧪 Test Run Report</h1>");
        html.append("<div class='subtitle'>").append(suite.getName()).append("</div>");

        // Summary Cards
        html.append("<div class='summary'>");
        html.append("<div class='stat-card'>");
        html.append("<h3>Total Tests</h3>");
        html.append("<div class='value'>").append(total).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card passed'>");
        html.append("<h3>Passed</h3>");
        html.append("<div class='value'>").append(passed).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card failed'>");
        html.append("<h3>Failed</h3>");
        html.append("<div class='value'>").append(failed).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card'>");
        html.append("<h3>Pass Rate</h3>");
        html.append("<div class='value'>").append(String.format("%.1f%%", passRate)).append("</div>");
        html.append("</div>");
        html.append("</div>");

        // Progress Bar
        html.append("<div class='progress-bar'>");
        html.append("<div class='progress-fill' style='width: ").append(passRate).append("%;'>");
        html.append(String.format("%.1f%% Pass Rate", passRate));
        html.append("</div>");
        html.append("</div>");

        // Results Table
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>Test Case</th>");
        html.append("<th>Test Name</th>");
        html.append("<th>Type</th>");
        html.append("<th>Result</th>");
        html.append("<th>Duration</th>");
        html.append("</tr></thead>");
        html.append("<tbody>");

        for (TestCase tc : suite.getTestCases()) {
            TestResult result = results.stream()
                    .filter(r -> r.getTestName().equals(tc.getTestName()))
                    .findFirst()
                    .orElse(null);

            if (result != null) {
                String status = result.getStatus().toString();
                String statusClass = status.equals("PASSED") ? "passed" : "failed";
                double duration = (double) (result.getDuration() != null ? result.getDuration() : 0) / 1000;

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
        html.append("<p style='margin-top: 30px; color: #666; text-align: center;'>Generated: ")
                .append(formattedDate)
                .append("</p>");
        html.append("</div>");
        html.append("</body></html>");
        return html.toString();
    }
}