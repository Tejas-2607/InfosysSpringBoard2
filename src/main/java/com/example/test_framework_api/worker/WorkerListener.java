package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.repository.TestRunRepository;
import com.example.test_framework_api.service.TestResultService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.example.test_framework_api.config.RabbitMQConfig;
import com.example.test_framework_api.dto.TestCaseExecutionRequest;
import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.repository.TestCaseRepository;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.service.TestSuiteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import static com.example.test_framework_api.config.RabbitMQConfig.QUEUE;

/**
 * ENHANCED: Worker listener with edge case handling for suite execution.
 * FIXED: Type-safe casting for handleElementTest payload
 * 
 * Edge Cases:
 * 1. threads=1 → Sequential execution logged
 * 2. Invalid threads → Default to 1 with warning
 * 3. Empty suite → Immediate COMPLETE status
 * 4. Mixed failures → Accurate aggregation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkerListener {

    private final RetryTemplate retryTemplate;
    private final TestRunService runService;
    private final TestCaseRepository caseRepository;
    private final TestExecutor testExecutor;
    private final TestRunRepository testRunRepository;
    private final TestResultService testResultService;
    private final TestSuiteService suiteService;

    @RabbitListener(queues = QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(TestRunRequest request) {
        long startTime = System.currentTimeMillis();
        retryTemplate.execute(context -> {
            log.info("Attempt #{} – processing TestRun {}", 
                context.getRetryCount() + 1, request.getTestId());

            TestRun testRun = testRunRepository.findById(request.getTestId())
                    .orElseThrow(() -> new RuntimeException("TestRun not found: " + request.getTestId()));

            testExecutor.executeTest();
            long duration = System.currentTimeMillis() - startTime;
            updateTestRun(testRun, TestStatus.PASSED);
            saveResult(testRun, TestStatus.PASSED, duration, context.getRetryCount());
            return null;

        }, recovery -> {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("MAX RETRIES EXCEEDED – marking FAILED and sending to DLQ");
            TestRun testRun = testRunRepository.findById(request.getTestId()).orElse(null);
            if (testRun != null) {
                updateTestRun(testRun, TestStatus.FAILED);
                saveResult(testRun, TestStatus.FAILED, duration, recovery.getRetryCount());
            }
            return null;
        });
    }

    /**
     * FIXED: Type-safe casting for dynamic test payload
     */
    @RabbitListener(queues = "elementTestQueue", containerFactory = "rabbitListenerContainerFactory")
    public void handleElementTest(Map<String, Object> payload) {
        String url = (String) payload.get("url");
        String elementId = (String) payload.get("elementId");
        String action = (String) payload.get("action");
        String expectedResult = (String) payload.get("expectedResult");
        
        // FIXED: Type-safe extraction of actions list
        List<Map<String, Object>> actionsList = extractActionsList(payload.get("actions"));
        
        Object testRunIdObj = payload.get("testRunId");
        Long testRunId = (testRunIdObj instanceof Number) ? ((Number) testRunIdObj).longValue()
                : Long.valueOf(testRunIdObj.toString());

        long startTime = System.currentTimeMillis();
        try {
            if (actionsList != null && !actionsList.isEmpty()) {
                for (Map<String, Object> step : actionsList) {
                    String stepAction = (String) step.get("type");
                    String value = (String) step.get("value");
                    testExecutor.executeDynamicTest(url, elementId, stepAction, expectedResult,
                            value != null ? value : "");
                }
            } else if (action != null) {
                testExecutor.executeDynamicTest(url, elementId, action, expectedResult, "");
            } else {
                throw new IllegalArgumentException("No action provided");
            }

            TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
            if (testRun != null) {
                updateTestRun(testRun, TestStatus.PASSED);
                saveResult(testRun, TestStatus.PASSED, System.currentTimeMillis() - startTime, 0);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
            if (testRun != null) {
                updateTestRun(testRun, TestStatus.FAILED);
                saveResult(testRun, TestStatus.FAILED, duration, 1);
            }
            log.error("Dynamic test FAILED: {}", e.getMessage());
        }
    }

    /**
     * ADDED: Type-safe helper method to extract actions list from payload
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractActionsList(Object actionsObj) {
        if (actionsObj == null) {
            return null;
        }
        
        if (actionsObj instanceof List) {
            try {
                // Validate that it's actually a List of Maps
                List<?> list = (List<?>) actionsObj;
                if (list.isEmpty()) {
                    return new ArrayList<>();
                }
                
                // Check first element to ensure it's a Map
                if (list.get(0) instanceof Map) {
                    return (List<Map<String, Object>>) actionsObj;
                } else {
                    log.warn("Actions list contains non-Map elements, ignoring");
                    return null;
                }
            } catch (ClassCastException e) {
                log.error("Failed to cast actions to List<Map<String, Object>>: {}", e.getMessage());
                return null;
            }
        }
        
        log.warn("Actions payload is not a List: {}", actionsObj.getClass());
        return null;
    }

    /**
     * ENHANCED: Suite execution with edge case handling.
     * 
     * Edge Cases:
     * 1. threads=1 → Sequential mode logged
     * 2. Invalid threads → Defaults to 1 with warning
     * 3. Empty cases → Immediate COMPLETE
     * 4. Mixed failures → Accurate pass rate calculation
     */
    @RabbitListener(queues = RabbitMQConfig.TEST_SUITE_QUEUE)
    public void handleSuiteExecution(TestCaseExecutionRequest request) {
        log.info("Received suite execution request: Suite {}, Run {}, Threads {}", 
            request.getTestSuiteId(), request.getTestRunId(), request.getParallelThreads());

        TestRun run = runService.getTestRunById(request.getTestRunId());
        if (run == null) {
            log.error("TestRun not found for ID: {}", request.getTestRunId());
            return;
        }

        // EDGE CASE 2: Invalid threads validation
        int parallelThreads = request.getParallelThreads();
        if (parallelThreads < 1 || parallelThreads > 8) {
            log.warn("Invalid parallelThreads {} for suite {}, defaulting to 1", 
                parallelThreads, request.getTestSuiteId());
            parallelThreads = 1;
            request.setParallelThreads(1);
        }

        run.setStatus(TestStatus.RUNNING);
        runService.updateTestRun(run);

        List<TestCase> cases = caseRepository.findByTestSuiteId(request.getTestSuiteId());
        
        // EDGE CASE 3: Empty suite handling
        if (cases == null || cases.isEmpty()) {
            log.warn("Empty suite {} - marking COMPLETE immediately", request.getTestSuiteId());
            run.setStatus(TestStatus.COMPLETED);
            runService.updateTestRun(run);
            suiteService.updateSuiteStatus(request.getTestSuiteId());
            return;
        }

        // EDGE CASE 1: Sequential fallback
        if (parallelThreads == 1) {
            log.info("Executing suite {} in SEQUENTIAL mode", request.getTestSuiteId());
            executeSequentialSuite(cases, run, request.getTestSuiteId());
        } else {
            log.info("Executing suite {} in PARALLEL mode ({} threads)", 
                request.getTestSuiteId(), parallelThreads);
            
            // Delegate to service for async parallel execution
            suiteService.executeSuiteParallel(
                request.getTestSuiteId(), 
                run, 
                parallelThreads
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Parallel execution failed: {}", ex.getMessage());
                    run.setStatus(TestStatus.FAILED);
                } else {
                    // EDGE CASE 4: Mixed failure aggregation in service
                    run.setStatus(TestStatus.COMPLETED);
                }
                runService.updateTestRun(run);
                log.info("Suite {} execution complete", request.getTestSuiteId());
            });
        }
    }

    /**
     * EDGE CASE 1 & 4: Sequential execution with mixed failure tracking.
     */
    private void executeSequentialSuite(List<TestCase> cases, TestRun run, Long suiteId) {
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
                
                List<TestResult> results = testResultService.findByTestRunIdAndTestName(
                    run.getId(), tc.getTestName()
                );
                
                if (!results.isEmpty()) {
                    TestResult latestResult = results.get(results.size() - 1);
                    if (latestResult.getStatus() == TestStatus.PASSED) {
                        passed++;
                        log.info("✓ PASSED: {}", tc.getTestCaseId());
                    } else {
                        failed++;
                        log.warn("✗ FAILED: {} - {}", tc.getTestCaseId(), 
                            latestResult.getErrorMessage());
                    }
                } else {
                    log.warn("⚠ WARNING: No result saved for {}", tc.getTestCaseId());
                }
                
                executed++;
            } catch (Exception e) {
                failed++;
                log.error("✗ EXCEPTION in test case {}: {}", tc.getTestCaseId(), e.getMessage());
                
                TestResult failureResult = new TestResult();
                failureResult.setTestName(tc.getTestName());
                failureResult.setStatus(TestStatus.FAILED);
                failureResult.setErrorMessage("Exception: " + e.getMessage());
                failureResult.setTestRun(run);
                failureResult.setTestSuite(tc.getTestSuite()); // CRITICAL: Link result to suite
                failureResult.setDuration(0L);
                failureResult.setRetryCount(0);
                failureResult.setCreatedAt(LocalDateTime.now());
                testResultService.saveTestResult(failureResult);
            }
        }

        // EDGE CASE 4: Mixed failure aggregation
        if (failed > 0 && passed > 0) {
            run.setStatus(TestStatus.COMPLETED); // Partial success
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
        
        runService.updateTestRun(run);
        suiteService.updateSuiteStatus(suiteId);
    }

    private void updateTestRun(TestRun tr, TestStatus status) {
        tr.setStatus(status);
        testRunRepository.save(tr);
    }

    private void saveResult(TestRun tr, TestStatus status, long duration, int retryCount) {
        TestResult r = new TestResult();
        r.setTestName(tr.getName());
        r.setStatus(status);
        r.setDuration(duration);
        r.setCreatedAt(LocalDateTime.now());
        r.setTestRun(tr);
        r.setRetryCount(retryCount);
        testResultService.saveTestResult(r);
        log.debug("Saved TestResult for TestRun ID: {} | Status: {} | Duration: {}ms | Retries: {}", 
            tr.getId(), status, duration, retryCount);
    }
}