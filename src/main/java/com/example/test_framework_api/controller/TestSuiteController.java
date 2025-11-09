package com.example.test_framework_api.controller;

import com.example.test_framework_api.dto.TestSuiteRequest;
import com.example.test_framework_api.dto.TestCaseExecutionRequest; // FIXED: Added import
import com.example.test_framework_api.model.TestRun; // FIXED: Added import
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestSuiteService;
import com.example.test_framework_api.service.ProduceReportHtmlService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;
import java.util.List;

import static com.example.test_framework_api.config.RabbitMQConfig.TEST_SUITE_QUEUE; // NEW FEATURE: New queue

@RestController
@RequestMapping("/api/suites")
@RequiredArgsConstructor
public class TestSuiteController {

  private final TestSuiteService suiteService;
  private final TestRunService runService;
  private final RabbitTemplate rabbitTemplate;
  private final ProduceReportHtmlService reportService;

  // NEW FEATURE: Import CSV as test suite
  @PostMapping("/import")
  public ResponseEntity<TestSuite> importSuite(@ModelAttribute TestSuiteRequest request) {
    try {
      TestSuite suite = suiteService.importFromCsv(request.getCsvFile(), request.getSuiteName(),
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

  // NEW FEATURE: Queue suite execution as TestRun
  @PostMapping("/{id}/run")
  public ResponseEntity<String> runSuite(@PathVariable Long id) {
    TestSuite suite = suiteService.getSuiteById(id);
    if (suite == null)
      return ResponseEntity.notFound().build();

    TestRun run = runService.createTestRun(suite.getName() + "-Suite"); // Create linked run
    suite.setTestRun(run);
    suiteService.getSuiteById(id); // Update

    TestCaseExecutionRequest req = new TestCaseExecutionRequest();
    req.setTestSuiteId(id);
    req.setTestRunId(run.getId());
    rabbitTemplate.convertAndSend(TEST_SUITE_QUEUE, req); // Send to new queue
    return ResponseEntity.ok("Suite queued: " + run.getId());
  }

  @GetMapping("/{id}/report")
  public ResponseEntity<String> getSuiteReport(@PathVariable Long id) {
    try {
      String reportPath = reportService.generateSuiteReport(id);
      return ResponseEntity.ok(reportPath);
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }
}