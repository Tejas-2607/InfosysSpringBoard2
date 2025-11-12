// // Updated ProduceReportHtmlService.java - Fixed unused variables by using them in logic/return
// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestSuite;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.repository.TestRunRepository;
// import com.example.test_framework_api.repository.TestSuiteRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardOpenOption;
// import java.util.List;

// @Service
// @RequiredArgsConstructor
// public class ProduceReportHtmlService {

//     private final TestSuiteRepository suiteRepository;
//     private final TestRunRepository runRepository;

//     // FIXED: No-arg overload for general endpoint
//     public String generateReport() {
//         List<TestRun> runs = runRepository.findAll();
//         Long latestRunId = runs.isEmpty() ? -1L : runs.get(runs.size() - 1).getId();
//         return generateReport(latestRunId);
//     }

//     public String generateReport(Long runId) {
//         TestRun run = runRepository.findById(runId)
//                 .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
//         // Existing logic: Copy to allure-results, run Allure...
//         String reportPath = "target/allure-results-run-" + runId;
//         // ... implement Allure generation ...
//         run.setReportPath(reportPath); // Assume TestRun has reportPath
//         runRepository.save(run);
//         return reportPath;
//     }

//     // FIXED: Suite-specific report - Use reportPath (write to file), htmlContent
//     // (write to file), passed (in HTML)
//     public String generateSuiteReport(Long suiteId) {
//         TestSuite suite = suiteRepository.findById(suiteId)
//                 .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));
//         suite.updateStatusFromResults();

//         String reportPath = "target/allure-results-suite-" + suiteId; // FIXED: Used - Create dir
//         new java.io.File(reportPath).mkdirs();

//         String htmlContent = generateSuiteHtml(suite); // FIXED: Used - Write to file
//         Path htmlFilePath = Paths.get(reportPath, "suite-summary.html");
//         try {
//             Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
//                     StandardOpenOption.TRUNCATE_EXISTING);
//         } catch (IOException e) {
//             throw new RuntimeException("Failed to write suite HTML", e);
//         }

//         long passed = 0; // FIXED: Used in HTML (passed var now referenced)
//         if (suite.getTestRun() != null) {
//             List<TestResult> allResults = suite.getTestRun().getTestResults();
//             passed = allResults.stream().filter(tr -> tr.getStatus() == TestStatus.PASSED).count();
//             // Optionally: Use passed for Allure config or log
//             System.out.println("Suite " + suiteId + ": " + passed + " passed out of " + allResults.size());
//         }

//         // Run Allure generate (e.g., via ProcessBuilder or Maven exec)
//         try {
//             ProcessBuilder pb = new ProcessBuilder("allure", "generate", reportPath, "-o",
//                     "target/allure-report-suite-" + suiteId);
//             pb.inheritIO();
//             pb.start().waitFor();
//         } catch (Exception e) {
//             throw new RuntimeException("Allure generation failed for suite " + suiteId, e);
//         }

//         suite.setReportPath("target/allure-report-suite-" + suiteId);
//         suiteRepository.save(suite);
//         return suite.getReportPath();
//     }

//     private String generateSuiteHtml(TestSuite suite) {
//         long total = suite.getTestCases().size();
//         long passed = 0; // Will be set from caller or query
//         if (suite.getTestRun() != null) {
//             passed = suite.getTestRun().getTestResults().stream().filter(tr -> tr.getStatus() == TestStatus.PASSED)
//                     .count();
//         }

//         StringBuilder html = new StringBuilder();
//         html.append("<html><body><h1>Suite Report: ").append(suite.getName()).append("</h1>")
//                 .append("<p>Status: ").append(suite.getStatus()).append("</p>")
//                 .append("<p>Total Cases: ").append(total).append(" | Passed: ").append(passed).append(" | Rate: ")
//                 .append(String.format("%.2f", total > 0 ? (passed * 100.0 / total) : 0)).append("%</p>") // FIXED: Use
//                                                                                                          // passed
//                 .append("<table border='1'><tr><th>Case ID</th><th>Name</th><th>Status</th></tr>");
//         for (TestCase tc : suite.getTestCases()) {
//             html.append("<tr><td>").append(tc.getTestCaseId()).append("</td><td>").append(tc.getTestName())
//                     .append("</td><td>")
//                     .append(tc.getTestSuite() != null ? tc.getTestSuite().getStatus() : "Pending").append("</td></tr>");
//         }
//         html.append("</table></body></html>");
//         return html.toString();
//     }
// }

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
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProduceReportHtmlService {

    private final TestSuiteRepository suiteRepository;
    private final TestRunRepository runRepository;

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
        // FIXED: Use Maven Allure plugin instead of CLI
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "allure:generate", "-Dallure.results.directory=" + reportPath,
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
     * FIXED #4: Suite report generation with proper error handling and return value
     */
    public String generateSuiteReport(Long suiteId) {
        TestSuite suite = suiteRepository.findById(suiteId)
                .orElseThrow(() -> new IllegalArgumentException("Suite not found: " + suiteId));

        // Update suite status before generating report
        suite.updateStatusFromResults();
        suiteRepository.save(suite);

        String reportPath = "target/allure-results-suite-" + suiteId;
        new java.io.File(reportPath).mkdirs();

        String htmlContent = generateSuiteHtml(suite);
        Path htmlFilePath = Paths.get(reportPath, "suite-summary.html");

        try {
            Files.write(htmlFilePath, htmlContent.getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Generated HTML summary at: " + htmlFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write suite HTML: " + e.getMessage());
            throw new RuntimeException("Failed to write suite HTML", e);
        }

        long passed = 0;
        long total = suite.getTestCases().size();

        if (suite.getTestRun() != null) {
            List<TestResult> allResults = suite.getTestRun().getTestResults();
            passed = allResults.stream().filter(tr -> tr.getStatus() == TestStatus.PASSED).count();
            log.info("Suite {}: {} passed out of {} (rate: {:.2f}%)", suiteId, passed, total,
                    total > 0 ? (passed * 100.0 / total) : 0); // FIXED: Use total in log
        }

        // Generate Allure report
        try {
            ProcessBuilder pb = new ProcessBuilder("mvn", "allure:generate",
                    "-Dallure.results.directory=" + reportPath,
                    "-Dallure.report.directory=target/allure-report-suite-" + suiteId,
                    "--quiet");
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Maven Allure failed for suite {} (exit {}); check pom.xml plugin", suiteId, exitCode);
                throw new RuntimeException("Maven Allure generation failed (exit " + exitCode + ")");
            }
            log.info("Maven Allure report generated for suite {}", suiteId);
        } catch (Exception e) {
            log.error("Maven Allure process failed for suite {}: {}", suiteId, e.getMessage());
            throw new RuntimeException("Maven Allure execution failed", e);
        }

        suite.setReportPath("target/allure-report-suite-" + suiteId);
        suiteRepository.save(suite);
        return suite.getReportPath();
    }
    // }catch(

    // Exception e)
    // {
    // System.err.println("Allure generation failed for suite " + suiteId + ": " +
    // e.getMessage());
    // // FIXED #4: Don't throw exception on Allure failure
    // suite.setReportPath("target/allure-report-suite-" + suiteId + " (allure
    // error)");
    // }

    // suiteRepository.save(suite);

    // // FIXED #4: Always return valid path, even if Allure failed
    // return suite.getReportPath();
    // }

    private String generateSuiteHtml(TestSuite suite) {
        long total = suite.getTestCases().size();
        long passed = 0;

        if (suite.getTestRun() != null) {
            passed = suite.getTestRun().getTestResults().stream()
                    .filter(tr -> tr.getStatus() == TestStatus.PASSED)
                    .count();
        }
        double passRate = total > 0 ? (passed * 100.0 / total) : 0; // FIXED: Use total in rate
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Suite Report: ").append(suite.getName()).append("</title>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; margin: 20px; }")
                .append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }")
                .append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                .append("th { background-color: #4CAF50; color: white; }")
                .append(".passed { background-color: #d4edda; }")
                .append(".failed { background-color: #f8d7da; }")
                .append("</style></head><body>")
                .append("<h1>Suite Report: ").append(suite.getName()).append("</h1>")
                .append("<p><strong>Status:</strong> ").append(suite.getStatus()).append("</p>")
                .append("<p><strong>Total Cases:</strong> ").append(total)
                .append(" | <strong>Passed:</strong> ").append(passed)
                .append(" | <strong>Rate:</strong> ")
                .append(String.format("%.2f", passRate)).append("%</p>") // FIXED: Use total/passRate
                .append("<table><tr><th>Case ID</th><th>Name</th><th>Type</th><th>Status</th></tr>");

        for (TestCase tc : suite.getTestCases()) {
            String statusClass = tc.getTestSuite().getStatus() == TestStatus.PASSED ? "passed" : "failed";
            html.append("<tr class='").append(statusClass).append("'><td>").append(tc.getTestCaseId())
                    .append("</td><td>").append(tc.getTestName())
                    .append("</td><td>").append(tc.getTestType())
                    .append("</td><td>")
                    .append(tc.getTestSuite() != null ? tc.getTestSuite().getStatus() : "Pending")
                    .append("</td></tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }
}