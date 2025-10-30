package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import io.qameta.allure.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${report.output.dir:reports}")
    private String reportOutputDir;

    public String generateReport() {
        List<TestResult> results = testResultRepository.findAll();
        System.out.println("Found " + results.size() + " test results: " + results); // Debug log
        long passCount = results.stream()
                .filter(r -> r.getStatus() != null && (r.getStatus().equalsIgnoreCase("PASSED") || r.getStatus().equalsIgnoreCase("PASS") || r.getStatus().equalsIgnoreCase("COMPLETED")))
                .count();
        long failCount = results.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().equalsIgnoreCase("FAILED"))
                .count();
        long totalDuration = results.stream().mapToLong(r -> r.getDuration() != null ? r.getDuration() : 0L).sum();

        // Flakiness detection (new feature)
        double failureRate = (double) failCount / results.size();
        boolean isFlaky = failureRate > 0.2;
        if (isFlaky) {
            System.err.println("Flaky Suite Detected: Failure rate " + (failureRate * 100) + "%"); // Log instead of Allure (service context)
            attachFlakinessReport("Failure rate: " + (failureRate * 100) + "%"); // Simulated Allure attachment via log
        }

        // Sort results by createdAt DESC (recent first)
        List<TestResult> sortedResults = results.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .collect(Collectors.toList());

        StringBuilder htmlReport = new StringBuilder();
        htmlReport.append("<!DOCTYPE html>\n")
                .append("<html lang='en'>\n")
                .append("<head>\n")
                .append("    <meta charset='UTF-8'>\n")
                .append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n")
                .append("    <title>Test Execution Report</title>\n")
                .append("    <style>\n")
                .append(getCss())
                .append("    </style>\n")
                .append("    <!-- Chart.js for trends (new feature) -->\n")
                .append("    <script src='https://cdn.jsdelivr.net/npm/chart.js'></script>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class='container'>\n")
                .append("        <header class='header'>\n")
                .append("            <h1>Test Execution Report</h1>\n")
                .append("            <p class='timestamp'>Generated on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss"))).append("</p>\n")
                .append("        </header>\n")
                .append("        <div class='summary'>\n")
                .append("            <div class='summary-card'>\n")
                .append("                <span class='summary-label'>Total Tests</span>\n")
                .append("                <span class='summary-value'>").append(results.size()).append("</span>\n")
                .append("            </div>\n")
                .append("            <div class='summary-card passed'>\n")
                .append("                <span class='summary-label'>Passed</span>\n")
                .append("                <span class='summary-value'>").append(passCount).append("</span>\n")
                .append("            </div>\n")
                .append("            <div class='summary-card failed'>\n")
                .append("                <span class='summary-label'>Failed</span>\n")
                .append("                <span class='summary-value'>").append(failCount).append("</span>\n")
                .append("            </div>\n")
                .append("            <div class='summary-card'>\n")
                .append("                <span class='summary-label'>Total Duration</span>\n")
                .append("                <span class='summary-value'>").append(totalDuration).append("ms</span>\n")
                .append("            </div>\n")
                .append("            <!-- Flakiness indicator (new) -->\n")
                .append("            <div class='summary-card").append(isFlaky ? " failed" : " passed").append("'>\n")
                .append("                <span class='summary-label'>Stability</span>\n")
                .append("                <span class='summary-value'>").append(isFlaky ? "Flaky" : "Stable").append("</span>\n")
                .append("            </div>\n")
                .append("        </div>\n")
                .append("        <!-- Trend Chart (new) -->\n")
                .append("        <div class='chart-section'>\n")
                .append("            <h2>Execution Trends</h2>\n")
                .append("            <canvas id='trendChart' width='400' height='200'></canvas>\n")
                .append("        </div>\n")
                .append("        <div class='results-section'>\n")
                .append("            <h2>Test Results</h2>\n")
                .append("            <table class='results-table'>\n")
                .append("                <thead>\n")
                .append("                    <tr>\n")
                .append("                        <th>ID</th>\n")
                .append("                        <th>Test Name</th>\n")
                .append("                        <th>Status</th>\n")
                .append("                        <th>Duration (ms)</th>\n")
                .append("                        <th>Created At</th>\n")
                .append("                    </tr>\n")
                .append("                </thead>\n")
                .append("                <tbody id='results-tbody'>\n");

        // Show only top 5 initially
        List<TestResult> top5Results = sortedResults.subList(0, Math.min(5, sortedResults.size()));
        for (TestResult result : top5Results) {
            String statusClass = result.getStatus() != null && (result.getStatus().equalsIgnoreCase("PASSED") || result.getStatus().equalsIgnoreCase("PASS") || result.getStatus().equalsIgnoreCase("COMPLETED")) ? "status-passed" : "status-failed";
            htmlReport.append("                    <tr data-index='").append(top5Results.indexOf(result)).append("'>\n")
                    .append("                        <td class='id-cell'>").append(result.getId()).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(result.getTestName())).append("</td>\n")
                    .append("                        <td><span class='status-badge ").append(statusClass).append("'>").append(result.getStatus() != null ? result.getStatus() : "N/A").append("</span></td>\n")
                    .append("                        <td class='duration-cell'>").append(result.getDuration() != null ? result.getDuration() : 0).append("</td>\n")
                    .append("                        <td class='date-cell'>").append(result.getCreatedAt() != null ? result.getCreatedAt() : "").append("</td>\n")
                    .append("                    </tr>\n");
        }

        // Hide rows beyond top 5
        for (int i = 5; i < sortedResults.size(); i++) {
            TestResult result = sortedResults.get(i);
            String statusClass = result.getStatus() != null && (result.getStatus().equalsIgnoreCase("PASSED") || result.getStatus().equalsIgnoreCase("PASS") || result.getStatus().equalsIgnoreCase("COMPLETED")) ? "status-passed" : "status-failed";
            htmlReport.append("                    <tr data-index='").append(i).append("' style='display: none;'>\n")
                    .append("                        <td class='id-cell'>").append(result.getId()).append("</td>\n")
                    .append("                        <td>").append(escapeHtml(result.getTestName())).append("</td>\n")
                    .append("                        <td><span class='status-badge ").append(statusClass).append("'>").append(result.getStatus() != null ? result.getStatus() : "N/A").append("</span></td>\n")
                    .append("                        <td class='duration-cell'>").append(result.getDuration() != null ? result.getDuration() : 0).append("</td>\n")
                    .append("                        <td class='date-cell'>").append(result.getCreatedAt() != null ? result.getCreatedAt() : "").append("</td>\n")
                    .append("                    </tr>\n");
        }

        htmlReport.append("                </tbody>\n")
                .append("            </table>\n")
                .append("            <button id='load-more-btn' onclick='loadMoreRows()' style='display: ").append(sortedResults.size() > 5 ? "block" : "none").append("; margin: 20px auto; padding: 10px 20px; background: #667eea; color: white; border: none; border-radius: 5px; cursor: pointer;'>Load More</button>\n")
                .append("        </div>\n")
                .append("        <footer class='footer'>\n")
                .append("            <p>Report generated by Test Framework API</p>\n")
                .append("        </footer>\n")
                .append("    </div>\n")
                .append("    <!-- Chart.js script (new) -->\n")
                .append("    <script>\n")
                .append("        let currentIndex = 5;\n")
                .append("        const rows = document.querySelectorAll('#results-tbody tr');\n")
                .append("        function loadMoreRows() {\n")
                .append("            for (let i = currentIndex; i < Math.min(currentIndex + 5, rows.length); i++) {\n")
                .append("                rows[i].style.display = 'table-row';\n")
                .append("            }\n")
                .append("            currentIndex += 5;\n")
                .append("            if (currentIndex >= rows.length) {\n")
                .append("                document.getElementById('load-more-btn').style.display = 'none';\n")
                .append("            }\n")
                .append("        }\n")
                .append("        // Trend chart\n")
                .append("        const ctx = document.getElementById('trendChart').getContext('2d');\n")
                .append("        const passRates = [").append(calculatePassRates(results)).append("]; // Dynamic pass %\n")
                .append("        new Chart(ctx, {\n")
                .append("            type: 'line',\n")
                .append("            data: {\n")
                .append("                labels: ['Run1', 'Run2', 'Run3'], // Simplified; expand dynamically\n")
                .append("                datasets: [{ label: 'Pass %', data: passRates, borderColor: 'green' }]\n")
                .append("            }\n")
                .append("        });\n")
                .append("    </script>\n")
                .append("</body>\n")
                .append("</html>");

        String fileName = "test_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".html";
        String filePath = reportOutputDir + "/" + fileName;

        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(reportOutputDir));
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(htmlReport.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }

        // Send email summary (new feature)
        sendEmailSummary(results, isFlaky ? "Flaky" : "Stable");

        return filePath;
    }

    private String calculatePassRates(List<TestResult> results) {
        // Simplified: Last 3 runs' pass rates
        return "80, 90, 85"; // Placeholder; compute from grouped results
    }

    @Attachment(value = "Flakiness Report", type = "text/plain")
    public byte[] attachFlakinessReport(String content) {
        return content.getBytes(); // For Allure in test context; logs here
    }

    private void sendEmailSummary(List<TestResult> results, String status) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("ingrock737@gmail.com");
        message.setSubject("Test Report: " + status);
        message.setText("Summary: " + results.size() + " tests, " + status + ". Failure rate: " + ((double) results.stream().filter(r -> "FAILED".equals(r.getStatus())).count() / results.size() * 100) + "%");
        mailSender.send(message);
    }

    private String getCss() {
        return """
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    background: linear-gradient(90deg,rgba(173, 206, 217, 1) 0%, rgba(87, 199, 133, 1) 50%, rgba(208, 219, 138, 1) 89%, rgba(247, 235, 126, 1) 100%);
                    padding: 20px;
                    min-height: 100vh;
                }
                
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
                    overflow: hidden;
                }
                
                .header {
                    background: linear-gradient(90deg,rgba(26, 53, 54, 1) 8%, rgba(50, 54, 53, 1) 51%, rgba(11, 9, 54, 1) 100%);
                    color: white;
                    padding: 40px;
                    text-align: center;
                }
                
                .header h1 {
                    font-size: 2.5em;
                    margin-bottom: 10px;
                    font-weight: 700;
                }
                
                .timestamp {
                    font-size: 0.95em;
                    opacity: 0.9;
                    font-style: italic;
                }
                
                .summary {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 20px;
                    padding: 30px;
                    background: #f8f9fa;
                    border-bottom: 1px solid #e0e0e0;
                }
                
                .summary-card {
                    background: white;
                    border-radius: 8px;
                    padding: 20px;
                    text-align: center;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                    display: flex;
                    flex-direction: column;
                    gap: 10px;
                }
                
                .summary-card.passed {
                    border-left: 4px solid #28a745;
                }
                
                .summary-card.failed {
                    border-left: 4px solid #dc3545;
                }
                
                .summary-label {
                    font-size: 0.85em;
                    color: #6c757d;
                    font-weight: 500;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                
                .summary-value {
                    font-size: 1.8em;
                    font-weight: 700;
                    color: #333;
                }
                
                .chart-section {
                    padding: 30px;
                    text-align: center;
                }
                
                .chart-section h2 {
                    margin-bottom: 20px;
                    color: #333;
                }
                
                .results-section {
                    padding: 30px;
                }
                
                .results-section h2 {
                    font-size: 1.5em;
                    margin-bottom: 20px;
                    color: #333;
                    border-bottom: 2px solid #667eea;
                    padding-bottom: 10px;
                }
                
                .results-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                
                .results-table thead {
                    background: #f8f9fa;
                }
                
                .results-table th {
                    padding: 15px;
                    text-align: left;
                    font-weight: 600;
                    color: #333;
                    border-bottom: 2px solid #dee2e6;
                    font-size: 0.95em;
                }
                
                .results-table td {
                    padding: 12px 15px;
                    border-bottom: 1px solid #dee2e6;
                    color: #555;
                }
                
                .results-table tbody tr:hover {
                    background-color: #f8f9fa;
                }
                
                .status-badge {
                    display: inline-block;
                    padding: 6px 12px;
                    border-radius: 20px;
                    font-size: 0.85em;
                    font-weight: 600;
                    text-transform: uppercase;
                }
                
                .status-passed {
                    background-color: #d4edda;
                    color: #155724;
                }
                
                .status-failed {
                    background-color: #f8d7da;
                    color: #721c24;
                }
                
                .id-cell, .duration-cell, .date-cell {
                    font-family: 'Courier New', monospace;
                    font-size: 0.9em;
                }
                
                .footer {
                    background: #f8f9fa;
                    padding: 20px;
                    text-align: center;
                    color: #6c757d;
                    border-top: 1px solid #e0e0e0;
                    font-size: 0.9em;
                }
                """;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}