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
                    "Please execute the suite using POST /api/suites/" + suiteId
                    + "/execute before generating reports.";
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
        html.append(
                ".container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }");
        html.append(
                ".summary { background: #e8f5e9; padding: 20px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #4CAF50; }");
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
}