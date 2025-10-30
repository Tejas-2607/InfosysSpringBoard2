// src/main/java/com/example/test_framework_api/service/ReportService.java
package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ResponseEntity<String> generateHtmlReportForAll(List<TestRun> testRuns) {
        String html = buildHtml(testRuns);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test-runs-report.html");
        return ResponseEntity.ok().headers(headers).body(html);
    }

    private String buildHtml(List<TestRun> testRuns) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n<title>Test Runs Report</title>\n");
        sb.append("<style>").append(getCss()).append("</style>\n");
        sb.append("</head>\n<body>\n<div class=\"container\">\n");
        sb.append("<h1>Test Execution Report</h1>\n");
        sb.append("<p class=\"generated\">Generated: ")
          .append(LocalDateTime.now().format(FMT)).append("</p>\n");

        if (testRuns.isEmpty()) {
            sb.append("<p class=\"no-data\">No test runs available.</p>\n");
        } else {
            sb.append("<table>\n<thead><tr>")
              .append("<th>ID</th><th>Name</th><th>Status</th><th>Created</th><th>Results</th>")
              .append("</tr></thead>\n<tbody>\n");
            for (TestRun r : testRuns) {
                sb.append("<tr class=\"").append(r.getStatus().toLowerCase()).append("\">")
                  .append("<td>").append(r.getId()).append("</td>")
                  .append("<td>").append(esc(r.getName())).append("</td>")
                  .append("<td><span class=\"status ").append(r.getStatus().toLowerCase()).append("\">")
                  .append(r.getStatus()).append("</span></td>")
                  .append("<td>").append(r.getCreatedAt().format(FMT)).append("</td>")
                  .append("<td>");

                // List<TestResult> results = r.getTestResults();
                // if (results == null || results.isEmpty()) {
                //     sb.append("<em>None</em>");
                // } else {
                //     sb.append("<ul class=\"results\">");
                //     for (TestResult res : results) {
                //         sb.append("<li><strong>").append(esc(res.getTestName())).append("</strong> - ")
                //           .append("<span class=\"status ").append(res.getStatus().toLowerCase()).append("\">")
                //           .append(res.getStatus()).append("</span> (")
                //           .append(res.getDuration()).append(" ms)</li>");
                //     }
                //     sb.append("</ul>");
                // }
                sb.append("</td></tr>\n");
            }
            sb.append("</tbody></table>\n");
        }
        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    private String getCss() {
        return """
            body{font-family:Arial,sans-serif;background:#f4f6f9;color:#333;margin:0}
            .container{max-width:1200px;margin:40px auto;padding:20px;background:#fff;border-radius:12px;
                       box-shadow:0 4px 20px rgba(0,0,0,.1)}
            h1{text-align:center;color:#2c3e50;margin-bottom:10px}
            .generated{text-align:center;color:#7f8c8d;font-size:.9em;margin-bottom:30px}
            table{width:100%;border-collapse:collapse;margin-top:20px;font-size:.95em}
            th{background:#3498db;color:#fff;padding:12px 15px;text-align:left;font-weight:600}
            td{padding:12px 15px;border-bottom:1px solid #ddd;vertical-align:top}
            tr:hover{background:#f8f9fa}
            .status{padding:4px 10px;border-radius:20px;font-size:.8em;font-weight:bold;
                    text-transform:uppercase;color:#fff}
            .status.completed{background:#27ae60}
            .status.failed{background:#c0392b}
            .status.pending,.status.running{background:#f39c12}
            .results{margin:8px 0;padding-left:20px;list-style:none}
            .results li{margin:6px 0}
            .no-data{text-align:center;font-style:italic;color:#95a5a6;padding:40px}
            @media(max-width:768px){
                table,thead,tbody,th,td,tr{display:block}
                thead tr{position:absolute;top:-9999px;left:-9999px}
                tr{margin-bottom:15px;border:1px solid #ccc;border-radius:8px}
                td{border:none;position:relative;padding-left:50%}
                td:before{content:attr(data-label);position:absolute;left:15px;width:45%;
                         font-weight:bold;color:#2c3e50}
            }
            """;
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}