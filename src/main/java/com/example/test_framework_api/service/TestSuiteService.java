package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.TestCaseRepository;
import com.example.test_framework_api.repository.TestResultRepository;
import com.example.test_framework_api.repository.TestSuiteRepository;
import com.example.test_framework_api.repository.UserRepository;
import com.example.test_framework_api.worker.TestExecutor;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * FIXED: Auto-update suite status when test cases are loaded
 */
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
    private final UserRepository userRepository;

    /**
     * FIXED ISSUE #1: Auto-update suite status when test cases are loaded
     */
    @Transactional
    public TestSuite importFromCsv(MultipartFile file, String suiteName, String description,
            Authentication authentication)
            throws IOException, CsvValidationException {
        if (file.isEmpty())
            throw new IllegalArgumentException("CSV file is empty");

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        TestSuite suite = new TestSuite();
        suite.setName(suiteName + " - " + System.currentTimeMillis());
        suite.setDescription(description);
        suite.setStatus(TestStatus.PENDING);
        suite.setCreatedBy(currentUser); // FIXED #4: Track creator
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

        // FIXED ISSUE #1: Mark suite as "LOADED" after all test cases saved
        suite.setStatus(TestStatus.COMPLETED);
        suiteRepository.save(suite);

        log.info("✓ Created suite ID {} with {} test cases - Status: COMPLETED (loaded)",
                suite.getId(), cases.size());
        return suite;
    }

    public List<TestSuite> getAllSuites() {
        return suiteRepository.findAll();
    }

    public TestSuite getSuiteById(Long id) {
        return suiteRepository.findById(id).orElse(null);
    }

    @Async("generalExecutor")
    public CompletableFuture<Void> executeSuiteParallel(Long suiteId, TestRun run, int parallelThreads) {
        log.info("Starting execution for suite {} with {} threads", suiteId, parallelThreads);

        if (parallelThreads < 1 || parallelThreads > 8) {
            log.warn("Invalid parallelThreads {} for suite {}, defaulting to 1", parallelThreads, suiteId);
            parallelThreads = 1;
        }

        List<TestCase> allCases = caseRepository.findByTestSuiteId(suiteId);

        if (allCases == null || allCases.isEmpty()) {
            log.warn("Empty suite {} - no test cases to execute", suiteId);
            run.setStatus(TestStatus.COMPLETED);
            updateSuiteStatus(suiteId);
            return CompletableFuture.completedFuture(null);
        }

        List<TestCase> enabledCases = allCases.stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getRun()))
                .collect(Collectors.toList());

        if (enabledCases.isEmpty()) {
            log.warn("All test cases disabled for suite {} - marking complete", suiteId);
            run.setStatus(TestStatus.COMPLETED);
            updateSuiteStatus(suiteId);
            return CompletableFuture.completedFuture(null);
        }

        if (parallelThreads == 1) {
            log.info("Executing suite {} in SEQUENTIAL mode", suiteId);
            return executeSequential(enabledCases, run, suiteId);
        }

        log.info("Executing suite {} in PARALLEL mode ({} threads)", suiteId, parallelThreads);
        return executeParallel(enabledCases, run, suiteId);
    }

    private CompletableFuture<Void> executeSequential(List<TestCase> cases, TestRun run, Long suiteId) {
        int executed = 0;
        int passed = 0;
        int failed = 0;

        for (TestCase tc : cases) {
            if (!Boolean.TRUE.equals(tc.getRun())) {
                log.debug("Skipping disabled test case: {}", tc.getTestCaseId());
                continue;
            }

            try {
                log.info("Sequential execution: {} - {}", tc.getTestCaseId(), tc.getTestName());
                testExecutor.executeTestCase(tc, run);

                List<TestResult> results = resultRepository.findByTestRunIdAndTestName(
                        run.getId(), tc.getTestName());

                if (!results.isEmpty()) {
                    TestResult latest = results.get(results.size() - 1);
                    if (latest.getStatus() == TestStatus.PASSED) {
                        passed++;
                        log.info("✓ PASSED: {}", tc.getTestCaseId());
                    } else {
                        failed++;
                        log.warn("✗ FAILED: {} - {}", tc.getTestCaseId(),
                                latest.getErrorMessage());
                    }
                } else {
                    log.warn("⚠ WARNING: No result saved for {}", tc.getTestCaseId());
                }

                executed++;
            } catch (Exception e) {
                failed++;
                log.error("✗ EXCEPTION in test case {}: {}", tc.getTestCaseId(), e.getMessage());
            }
        }

        if (failed > 0 && passed > 0) {
            run.setStatus(TestStatus.COMPLETED);
            double rate = passed * 100.0 / (passed + failed);
            log.info("Sequential suite COMPLETED (partial): {} passed, {} failed ({:.2f}%)",
                    passed, failed, rate);
        } else if (failed == 0) {
            run.setStatus(TestStatus.PASSED);
            log.info("Sequential suite PASSED: {}/{} (100%)", passed, executed);
        } else {
            run.setStatus(TestStatus.FAILED);
            log.warn("Sequential suite FAILED: 0/{} passed", executed);
        }

        updateSuiteStatus(suiteId);
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> executeParallel(List<TestCase> cases, TestRun run, Long suiteId) {
        List<TestCase> uiCases = cases.stream()
                .filter(tc -> "UI".equals(tc.getTestType()))
                .collect(Collectors.toList());

        List<TestCase> apiCases = cases.stream()
                .filter(tc -> "API".equals(tc.getTestType()))
                .collect(Collectors.toList());

        log.info("Executing {} UI tests and {} API tests in parallel", uiCases.size(), apiCases.size());

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

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();
        allFutures.addAll(uiFutures);
        allFutures.addAll(apiFutures);

        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                allFutures.toArray(new CompletableFuture[0]));

        return allOf.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Parallel execution completed with errors: {}", ex.getMessage());
            } else {
                log.info("Suite {} parallel execution completed successfully", suiteId);
            }

            updateSuiteStatus(suiteId);
        });
    }

    /**
     * FIXED: Update suite status with actual test results
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

            if (passed == total && failed == 0) {
                suite.setStatus(TestStatus.PASSED);
                log.info("Suite {} PASSED: {}/{} (100%)", suiteId, passed, total);
            } else if (passed > 0) {
                suite.setStatus(TestStatus.COMPLETED);
                double rate = passed * 100.0 / total;
                log.info("Suite {} COMPLETED (partial): {}/{} ({:.2f}%)", suiteId, passed, total, rate);
            } else {
                suite.setStatus(TestStatus.FAILED);
                log.warn("Suite {} FAILED: 0/{} passed", suiteId, total);
            }
        }

        suiteRepository.save(suite);
    }

    public List<TestSuite> getSuitesByUser(Long userId) {
        return suiteRepository.findByCreatedById(userId);
    }
}