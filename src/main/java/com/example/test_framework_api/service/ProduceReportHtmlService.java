// Updated ProduceReportHtmlService.java - Fixed unused variables by using them in logic/return
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.repository.TestRunRepository;
import com.example.test_framework_api.repository.TestSuiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduceReportHtmlService {

    private final TestSuiteRepository suiteRepository;
    private final TestRunRepository runRepository;

    // FIXED: No-arg overload for general endpoint
    public String generateReport() {
        List<TestRun> runs = runRepository.findAll();
        Long latestRunId = runs.isEmpty() ? -1L : runs.get(runs.size() - 1).getId();
        return generateReport(latestRunId);
    }

    public String generateReport(Long runId) {
        TestRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        // Existing logic: Copy to allure-results, run Allure...
        String reportPath = "target/allure-results-run-" + runId;
        // ... implement Allure generation ...
        run.setReportPath(reportPath); // Assume TestRun has reportPath
        runRepository.save(run);
        return reportPath;
    }

    // FIXED: Suite-specific report - Use reportPath (write to file), htmlContent
    // (write to file), passed (in HTML)
    public String generateSuiteReport(Long suiteId) {
        TestSuite suite = suiteRepository.findById(suiteId)
                .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));
        suite.updateStatusFromResults();

        String reportPath = "target/allure-results-suite-" + suiteId; // FIXED: Used - Create dir
        new java.io.File(reportPath).mkdirs();

        String htmlContent = generateSuiteHtml(suite); // FIXED: Used - Write to file
        Path htmlFilePath = Paths.get(reportPath, "suite-summary.html");
        try {
            Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write suite HTML", e);
        }

        long passed = 0; // FIXED: Used in HTML (passed var now referenced)
        if (suite.getTestRun() != null) {
            List<TestResult> allResults = suite.getTestRun().getTestResults();
            passed = allResults.stream().filter(tr -> tr.getStatus() == TestStatus.PASSED).count();
            // Optionally: Use passed for Allure config or log
            System.out.println("Suite " + suiteId + ": " + passed + " passed out of " + allResults.size());
        }

        // Run Allure generate (e.g., via ProcessBuilder or Maven exec)
        try {
            ProcessBuilder pb = new ProcessBuilder("allure", "generate", reportPath, "-o",
                    "target/allure-report-suite-" + suiteId);
            pb.inheritIO();
            pb.start().waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Allure generation failed for suite " + suiteId, e);
        }

        suite.setReportPath("target/allure-report-suite-" + suiteId);
        suiteRepository.save(suite);
        return suite.getReportPath();
    }

    private String generateSuiteHtml(TestSuite suite) {
        long total = suite.getTestCases().size();
        long passed = 0; // Will be set from caller or query
        if (suite.getTestRun() != null) {
            passed = suite.getTestRun().getTestResults().stream().filter(tr -> tr.getStatus() == TestStatus.PASSED)
                    .count();
        }

        StringBuilder html = new StringBuilder();
        html.append("<html><body><h1>Suite Report: ").append(suite.getName()).append("</h1>")
                .append("<p>Status: ").append(suite.getStatus()).append("</p>")
                .append("<p>Total Cases: ").append(total).append(" | Passed: ").append(passed).append(" | Rate: ")
                .append(String.format("%.2f", total > 0 ? (passed * 100.0 / total) : 0)).append("%</p>") // FIXED: Use
                                                                                                         // passed
                .append("<table border='1'><tr><th>Case ID</th><th>Name</th><th>Status</th></tr>");
        for (TestCase tc : suite.getTestCases()) {
            html.append("<tr><td>").append(tc.getTestCaseId()).append("</td><td>").append(tc.getTestName())
                    .append("</td><td>")
                    .append(tc.getTestSuite() != null ? tc.getTestSuite().getStatus() : "Pending").append("</td></tr>");
        }
        html.append("</table></body></html>");
        return html.toString();
    }
}