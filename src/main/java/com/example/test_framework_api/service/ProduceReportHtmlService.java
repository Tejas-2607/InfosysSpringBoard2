package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates an HTML test-result report and saves it under
 * src/main/resources/static/reports/
 *
 * Endpoint: GET /api/runs/reports-producehtml
 */
@Service
public class ProduceReportHtmlService {

    private static final String REPORT_DIR = "src/main/resources/static/reports";
    private static final DateTimeFormatter FILE_DTF = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * Public method used by the controller.
     * @return the generated file name (e.g. test_report_20251030_223505.html)
     */
    public String generateReport() throws Exception {
        List<TestResult> results = testResultRepository.findAll();

        Context ctx = new Context();
        ctx.setVariable("results", results);
        ctx.setVariable("generatedAt", LocalDateTime.now());

        String html = templateEngine.process("report-template", ctx);

        // ---- create folder if missing ----
        Path dir = Paths.get(REPORT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // ---- file name with timestamp ----
        String timestamp = LocalDateTime.now().format(FILE_DTF);
        String fileName = "test_report_" + timestamp + ".html";
        String fullPath = REPORT_DIR + "/" + fileName;

        try (FileWriter fw = new FileWriter(fullPath)) {
            fw.write(html);
        }

        return fileName;   // returned to the controller
    }
}