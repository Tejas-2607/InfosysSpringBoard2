package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.repository.TestCaseRepository;
import com.example.test_framework_api.repository.TestResultRepository;
import com.example.test_framework_api.repository.TestSuiteRepository;
import com.example.test_framework_api.worker.TestExecutor;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestSuiteService {

    private final TestSuiteRepository suiteRepository;
    private final TestCaseRepository caseRepository;
    private final TestResultRepository resultRepository;
    private final TestExecutor testExecutor;
    private final Executor uiTestExecutor;
    private final Executor apiTestExecutor;

    @Transactional
    public TestSuite importFromCsv(MultipartFile file, String suiteName, String description)
            throws IOException, CsvValidationException {
        if (file.isEmpty())
            throw new IllegalArgumentException("CSV file is empty");

        TestSuite suite = new TestSuite();
        suite.setName(suiteName + " - " + System.currentTimeMillis());
        suite.setDescription(description);
        suite.setStatus(TestStatus.PENDING);
        suite = suiteRepository.save(suite);

        List<TestCase> cases = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length < 12)
                throw new IllegalArgumentException("Invalid CSV: Expected 12+ columns");

            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                rowNum++;
                if (row.length < 12) {
                    log.warn("Skipping invalid row {} (only {} columns)", rowNum, row.length);
                    continue;
                }

                TestCase tc = new TestCase();
                tc.setTestCaseId(row[0] + "-" + UUID.randomUUID().toString().substring(0, 8));
                tc.setTestName(row[1]);
                tc.setTestType(row[2]);
                tc.setUrlEndpoint(row[3]);

                String actionLocator = row[4];
                if (actionLocator.contains("/")) {
                    String[] parts = actionLocator.split("/", 2);
                    tc.setHttpMethodAction(parts[0].trim());
                    tc.setLocatorType(parts[1].trim());
                } else {
                    tc.setHttpMethodAction(actionLocator);
                }

                tc.setLocatorType(row.length > 5 ? row[5] : "");
                tc.setLocatorValue(row.length > 6 ? row[6] : "");
                tc.setInputData(row.length > 7 ? row[7] : "");
                tc.setExpectedResult(row.length > 8 ? row[8] : "");
                tc.setPriority(row.length > 9 ? row[9] : "Medium");

                String runStr = row.length > 10 ? row[10] : "true";
                tc.setRun("YES".equalsIgnoreCase(runStr) || "Yes".equalsIgnoreCase(runStr)
                        || Boolean.parseBoolean(runStr));

                tc.setDescription(row.length > 11 ? row[11] : "");
                if (row.length > 12)
                    tc.setActionsJson(row[12]);

                tc.setTestSuite(suite);
                cases.add(tc);
            }
        }

        caseRepository.saveAll(cases);
        suite.setTestCases(cases);
        suiteRepository.save(suite);

        log.info("Created suite ID {} with {} test cases", suite.getId(), cases.size());
        return suite;
    }

    public List<TestSuite> getAllSuites() {
        return suiteRepository.findAll();
    }

    public TestSuite getSuiteById(Long id) {
        return suiteRepository.findById(id).orElse(null);
    }

    /**
     * NEW FEATURE: Execute test suite with parallel execution.
     * Separates UI and API tests for optimal resource usage.
     * 
     * @param suiteId Suite to execute
     * @param run TestRun for result tracking
     * @param parallelThreads Number of concurrent threads
     */
    @Async("generalExecutor")
    public CompletableFuture<Void> executeSuiteParallel(Long suiteId, TestRun run, int parallelThreads) {
        log.info("Starting parallel execution for suite {} with {} threads", suiteId, parallelThreads);
        
        List<TestCase> allCases = caseRepository.findByTestSuiteId(suiteId);
        List<TestCase> enabledCases = allCases.stream()
            .filter(tc -> Boolean.TRUE.equals(tc.getRun()))
            .collect(Collectors.toList());

        // Separate by type for optimal executor usage
        List<TestCase> uiCases = enabledCases.stream()
            .filter(tc -> "UI".equals(tc.getTestType()))
            .collect(Collectors.toList());
        
        List<TestCase> apiCases = enabledCases.stream()
            .filter(tc -> "API".equals(tc.getTestType()))
            .collect(Collectors.toList());

        log.info("Executing {} UI tests and {} API tests", uiCases.size(), apiCases.size());

        // Execute UI tests (limited concurrency)
        List<CompletableFuture<Void>> uiFutures = uiCases.stream()
            .map(tc -> CompletableFuture.runAsync(() -> {
                try {
                    log.debug("Executing UI test: {}", tc.getTestCaseId());
                    testExecutor.executeTestCase(tc, run);
                } catch (Exception e) {
                    log.error("UI test {} failed: {}", tc.getTestCaseId(), e.getMessage());
                }
            }, uiTestExecutor))
            .collect(Collectors.toList());

        // Execute API tests (higher concurrency)
        List<CompletableFuture<Void>> apiFutures = apiCases.stream()
            .map(tc -> CompletableFuture.runAsync(() -> {
                try {
                    log.debug("Executing API test: {}", tc.getTestCaseId());
                    testExecutor.executeTestCase(tc, run);
                } catch (Exception e) {
                    log.error("API test {} failed: {}", tc.getTestCaseId(), e.getMessage());
                }
            }, apiTestExecutor))
            .collect(Collectors.toList());

        // Combine all futures
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
        allFutures.addAll(uiFutures);
        allFutures.addAll(apiFutures);

        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            allFutures.toArray(new CompletableFuture[0])
        );

        return allOf.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Suite execution completed with errors: {}", ex.getMessage());
            } else {
                log.info("Suite {} execution completed successfully", suiteId);
            }
            
            // Update suite status after all tests complete
            updateSuiteStatus(suiteId);
        });
    }

    /**
     * Update suite status based on test results.
     * Thread-safe aggregation of parallel results.
     */
    public void updateSuiteStatus(Long suiteId) {
        TestSuite suite = getSuiteById(suiteId);
        if (suite == null || suite.getTestRun() == null) {
            log.warn("Cannot update status: suite or testRun is null for ID {}", suiteId);
            return;
        }

        Long runId = suite.getTestRun().getId();
        List<TestResult> results = resultRepository.findByTestRunId(runId);

        if (results.isEmpty()) {
            log.warn("No test results found for run ID {} (suite {})", runId, suiteId);
            suite.setStatus(TestStatus.PENDING);
        } else {
            long total = suite.getTestCases().stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getRun()))
                .count();
            long passed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.PASSED)
                .count();
            long failed = results.stream()
                .filter(r -> r.getStatus() == TestStatus.FAILED)
                .count();

            if (failed > 0) {
                suite.setStatus(TestStatus.FAILED);
            } else if (passed == total) {
                suite.setStatus(TestStatus.PASSED);
            } else {
                suite.setStatus(TestStatus.COMPLETED);
            }

            log.info("Updated suite {} status: {} (Passed: {}/{}, Failed: {})", 
                suiteId, suite.getStatus(), passed, total, failed);
        }

        suiteRepository.save(suite);
    }
}

// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.model.TestSuite;
// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.repository.TestCaseRepository;
// import com.example.test_framework_api.repository.TestResultRepository;
// import com.example.test_framework_api.repository.TestSuiteRepository;
// import com.opencsv.CSVReader;
// import com.opencsv.exceptions.CsvValidationException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;

// @Service
// @RequiredArgsConstructor
// public class TestSuiteService {

//     private final TestSuiteRepository suiteRepository;
//     private final TestCaseRepository caseRepository;
//     private final TestResultRepository resultRepository;

//     /**
//      * FIXED: Always create NEW suite with unique test cases, even if content is identical
//      * Each upload gets fresh test_suite_id and test_case records
//      */
//     @Transactional
//     public TestSuite importFromCsv(MultipartFile file, String suiteName, String description)
//             throws IOException, CsvValidationException {
//         if (file.isEmpty())
//             throw new IllegalArgumentException("CSV file is empty");

//         // FIXED #1: Always create NEW suite (never reuse old suite_id)
//         TestSuite suite = new TestSuite();
//         suite.setName(suiteName + " - " + System.currentTimeMillis()); // Unique name with timestamp
//         suite.setDescription(description);
//         suite.setStatus(TestStatus.PENDING);
//         suite = suiteRepository.save(suite); // Save first to get suite_id

//         List<TestCase> cases = new ArrayList<>();
//         try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
//             String[] headers = reader.readNext();
//             if (headers == null || headers.length < 12)
//                 throw new IllegalArgumentException("Invalid CSV: Expected 12+ columns");

//             String[] row;
//             int rowNum = 1;
//             while ((row = reader.readNext()) != null) {
//                 rowNum++;
//                 if (row.length < 12) {
//                     System.err.println("Skipping invalid row " + rowNum + " (only " + row.length + " columns)");
//                     continue;
//                 }

//                 // FIXED #1: Create NEW test case with unique ID for each upload
//                 TestCase tc = new TestCase();
//                 tc.setTestCaseId(row[0] + "-" + UUID.randomUUID().toString().substring(0, 8)); // Unique ID
//                 tc.setTestName(row[1]);
//                 tc.setTestType(row[2]);
//                 tc.setUrlEndpoint(row[3]);

//                 String actionLocator = row[4];
//                 if (actionLocator.contains("/")) {
//                     String[] parts = actionLocator.split("/", 2);
//                     tc.setHttpMethodAction(parts[0].trim());
//                     tc.setLocatorType(parts[1].trim());
//                 } else {
//                     tc.setHttpMethodAction(actionLocator);
//                 }

//                 tc.setLocatorType(row.length > 5 ? row[5] : "");
//                 tc.setLocatorValue(row.length > 6 ? row[6] : "");
//                 tc.setInputData(row.length > 7 ? row[7] : "");
//                 tc.setExpectedResult(row.length > 8 ? row[8] : "");
//                 tc.setPriority(row.length > 9 ? row[9] : "Medium");

//                 String runStr = row.length > 10 ? row[10] : "true";
//                 tc.setRun("YES".equalsIgnoreCase(runStr) || "Yes".equalsIgnoreCase(runStr)
//                         || Boolean.parseBoolean(runStr));

//                 tc.setDescription(row.length > 11 ? row[11] : "");
//                 if (row.length > 12)
//                     tc.setActionsJson(row[12]);

//                 tc.setTestSuite(suite); // Link to NEW suite
//                 cases.add(tc);
//             }
//         }

//         // FIXED #1: Save all NEW test cases (even if identical to old ones)
//         caseRepository.saveAll(cases);
//         suite.setTestCases(cases);
//         suiteRepository.save(suite);

//         System.out.println("Created NEW suite ID " + suite.getId() + " with " + cases.size() + " test cases");
//         return suite;
//     }

//     public List<TestSuite> getAllSuites() {
//         return suiteRepository.findAll();
//     }

//     public TestSuite getSuiteById(Long id) {
//         return suiteRepository.findById(id).orElse(null);
//     }

//     /**
//      * FIXED #2: Update suite status based on actual test_result records
//      */
//     public void updateSuiteStatus(Long suiteId) {
//         TestSuite suite = getSuiteById(suiteId);
//         if (suite == null || suite.getTestRun() == null) {
//             System.err.println("Cannot update status: suite or testRun is null for ID " + suiteId);
//             return;
//         }

//         Long runId = suite.getTestRun().getId();
//         List<TestResult> results = resultRepository.findByTestRunId(runId);

//         if (results.isEmpty()) {
//             System.err.println("No test results found for run ID " + runId + " (suite " + suiteId + ")");
//             suite.setStatus(TestStatus.PENDING); // No results yet
//         } else {
//             long total = suite.getTestCases().stream().filter(tc -> Boolean.TRUE.equals(tc.getRun())).count();
//             long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
//             long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();

//             if (failed > 0) {
//                 suite.setStatus(TestStatus.FAILED); // At least one failure
//             } else if (passed == total) {
//                 suite.setStatus(TestStatus.PASSED); // All passed
//             } else {
//                 suite.setStatus(TestStatus.COMPLETED); // Partial completion
//             }

//             System.out.println("Updated suite " + suiteId + " status: " + suite.getStatus() +
//                     " (Passed: " + passed + "/" + total + ", Failed: " + failed + ")");
//         }

//         suiteRepository.save(suite);
//     }
// }