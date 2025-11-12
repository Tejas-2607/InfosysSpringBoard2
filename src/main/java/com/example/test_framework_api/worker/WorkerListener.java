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
import static com.example.test_framework_api.config.RabbitMQConfig.QUEUE;

/**
 * Worker listener for processing test execution requests.
 * Now supports parallel execution via async service calls.
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

    @RabbitListener(queues = "elementTestQueue", containerFactory = "rabbitListenerContainerFactory")
    public void handleElementTest(Map<String, Object> payload) {
        String url = (String) payload.get("url");
        String elementId = (String) payload.get("elementId");
        String action = (String) payload.get("action");
        List<Map<String, Object>> actionsList = (List<Map<String, Object>>) payload.get("actions");
        String expectedResult = (String) payload.get("expectedResult");
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
     * NEW FEATURE: Suite execution with parallel support.
     * Delegates to service layer for async execution.
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

        run.setStatus(TestStatus.RUNNING);
        runService.updateTestRun(run);

        // Choose execution mode based on parallelThreads
        if (request.getParallelThreads() > 1) {
            log.info("Executing suite {} in PARALLEL mode ({} threads)", 
                request.getTestSuiteId(), request.getParallelThreads());
            
            // Async parallel execution via service
            suiteService.executeSuiteParallel(
                request.getTestSuiteId(), 
                run, 
                request.getParallelThreads()
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Parallel execution failed: {}", ex.getMessage());
                    run.setStatus(TestStatus.FAILED);
                } else {
                    run.setStatus(TestStatus.COMPLETED);
                }
                runService.updateTestRun(run);
                log.info("Suite {} execution complete", request.getTestSuiteId());
            });
        } else {
            log.info("Executing suite {} in SEQUENTIAL mode", request.getTestSuiteId());
            
            // Sequential execution (existing logic)
            List<TestCase> cases = caseRepository.findByTestSuiteId(request.getTestSuiteId());
            int executed = 0;
            int passed = 0;
            int failed = 0;

            for (TestCase tc : cases) {
                if (!Boolean.TRUE.equals(tc.getRun())) {
                    log.debug("Skipping disabled test case: {}", tc.getTestCaseId());
                    continue;
                }

                try {
                    log.info("Executing test case: {} - {}", tc.getTestCaseId(), tc.getTestName());
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
                    failureResult.setDuration(0L);
                    failureResult.setRetryCount(0);
                    failureResult.setCreatedAt(LocalDateTime.now());
                    testResultService.saveTestResult(failureResult);
                }
            }

            run.setStatus(failed > 0 ? TestStatus.FAILED : TestStatus.PASSED);
            runService.updateTestRun(run);
            suiteService.updateSuiteStatus(request.getTestSuiteId());

            log.info("Sequential suite execution complete: {} executed, {} passed, {} failed", 
                executed, passed, failed);
        }
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
// package com.example.test_framework_api.worker;

// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestRunRequest;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.repository.TestRunRepository;
// import com.example.test_framework_api.service.TestResultService;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.retry.support.RetryTemplate;
// import org.springframework.stereotype.Component;

// import com.example.test_framework_api.config.RabbitMQConfig;
// import com.example.test_framework_api.dto.TestCaseExecutionRequest;
// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.repository.TestCaseRepository;
// import com.example.test_framework_api.service.TestRunService;
// import com.example.test_framework_api.service.TestSuiteService;

// import lombok.RequiredArgsConstructor;

// import java.time.LocalDateTime;
// import java.util.Map;
// import java.util.List;
// import static com.example.test_framework_api.config.RabbitMQConfig.QUEUE;

// @Component
// @RequiredArgsConstructor
// public class WorkerListener {

//     private final RetryTemplate retryTemplate;
//     private final TestRunService runService;
//     private final TestCaseRepository caseRepository;
//     private final TestExecutor testExecutor;
//     private final TestRunRepository testRunRepository;
//     private final TestResultService testResultService;
//     private final TestSuiteService suiteService; // FIXED #2: Inject for status update

//     @RabbitListener(queues = QUEUE, containerFactory = "rabbitListenerContainerFactory")
//     public void receiveMessage(TestRunRequest request) {
//         long startTime = System.currentTimeMillis();
//         retryTemplate.execute(context -> {
//             System.out.println("Attempt #" + (context.getRetryCount() + 1)
//                     + " – processing TestRun " + request.getTestId());

//             TestRun testRun = testRunRepository.findById(request.getTestId())
//                     .orElseThrow(() -> new RuntimeException("TestRun not found: " + request.getTestId()));

//             testExecutor.executeTest();
//             long duration = System.currentTimeMillis() - startTime;
//             updateTestRun(testRun, TestStatus.PASSED);
//             saveResult(testRun, TestStatus.PASSED, duration, context.getRetryCount());
//             return null;

//         }, recovery -> {
//             long duration = System.currentTimeMillis() - startTime;
//             System.out.println("MAX RETRIES EXCEEDED – marking FAILED and sending to DLQ");
//             TestRun testRun = testRunRepository.findById(request.getTestId()).orElse(null);
//             if (testRun != null) {
//                 updateTestRun(testRun, TestStatus.FAILED);
//                 saveResult(testRun, TestStatus.FAILED, duration, recovery.getRetryCount());
//             }
//             return null;
//         });
//     }

//     @RabbitListener(queues = "elementTestQueue", containerFactory = "rabbitListenerContainerFactory")
//     public void handleElementTest(Map<String, Object> payload) {
//         String url = (String) payload.get("url");
//         String elementId = (String) payload.get("elementId");
//         String action = (String) payload.get("action");
//         List<Map<String, Object>> actionsList = (List<Map<String, Object>>) payload.get("actions");
//         String expectedResult = (String) payload.get("expectedResult");
//         Object testRunIdObj = payload.get("testRunId");
//         Long testRunId = (testRunIdObj instanceof Number) ? ((Number) testRunIdObj).longValue()
//                 : Long.valueOf(testRunIdObj.toString());

//         long startTime = System.currentTimeMillis();
//         try {
//             if (actionsList != null && !actionsList.isEmpty()) {
//                 for (Map<String, Object> step : actionsList) {
//                     String stepAction = (String) step.get("type");
//                     String value = (String) step.get("value");
//                     testExecutor.executeDynamicTest(url, elementId, stepAction, expectedResult,
//                             value != null ? value : "");
//                 }
//             } else if (action != null) {
//                 testExecutor.executeDynamicTest(url, elementId, action, expectedResult, "");
//             } else {
//                 throw new IllegalArgumentException("No action provided");
//             }

//             TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
//             if (testRun != null) {
//                 updateTestRun(testRun, TestStatus.PASSED);
//                 saveResult(testRun, TestStatus.PASSED, System.currentTimeMillis() - startTime, 0);
//             }
//         } catch (Exception e) {
//             long duration = System.currentTimeMillis() - startTime;
//             TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
//             if (testRun != null) {
//                 updateTestRun(testRun, TestStatus.FAILED);
//                 saveResult(testRun, TestStatus.FAILED, duration, 1);
//             }
//             System.err.println("Dynamic test FAILED: " + e.getMessage());
//         }
//     }

//     /**
//      * FIXED #2 & #3: Suite execution with proper result tracking and error handling
//      */
//     @RabbitListener(queues = RabbitMQConfig.TEST_SUITE_QUEUE)
//     public void handleSuiteExecution(TestCaseExecutionRequest request) {
//         TestRun run = runService.getTestRunById(request.getTestRunId());
//         if (run == null) {
//             System.err.println("TestRun not found for ID: " + request.getTestRunId());
//             return;
//         }

//         run.setStatus(TestStatus.RUNNING);
//         runService.updateTestRun(run);

//         List<TestCase> cases = caseRepository.findByTestSuiteId(request.getTestSuiteId());
//         System.out.println("Executing " + cases.size() + " test cases for suite " + request.getTestSuiteId());

//         int executed = 0;
//         int passed = 0;
//         int failed = 0;

//         for (TestCase tc : cases) {
//             if (!Boolean.TRUE.equals(tc.getRun())) {
//                 System.out.println("Skipping disabled test case: " + tc.getTestCaseId());
//                 continue;
//             }

//             try {
//                 // FIXED #3: Catch exceptions per test case, don't let one failure stop execution
//                 System.out.println("Executing test case: " + tc.getTestCaseId() + " - " + tc.getTestName());
//                 testExecutor.executeTestCase(tc, run);
                
//                 // FIXED #2: Verify result was saved by checking test_result table
//                 List<TestResult> results = testResultService.findByTestRunIdAndTestName(
//                     run.getId(), tc.getTestName()
//                 );
                
//                 if (!results.isEmpty()) {
//                     TestResult latestResult = results.get(results.size() - 1);
//                     if (latestResult.getStatus() == TestStatus.PASSED) {
//                         passed++;
//                         System.out.println("✓ PASSED: " + tc.getTestCaseId());
//                     } else {
//                         failed++;
//                         System.out.println("✗ FAILED: " + tc.getTestCaseId() + " - " + 
//                             latestResult.getErrorMessage());
//                     }
//                 } else {
//                     System.err.println("⚠ WARNING: No result saved for " + tc.getTestCaseId());
//                 }
                
//                 executed++;
//             } catch (Exception e) {
//                 // FIXED #3: Continue execution even if one test fails
//                 failed++;
//                 System.err.println("✗ EXCEPTION in test case " + tc.getTestCaseId() + ": " + 
//                     e.getMessage());
                
//                 // Manually save failed result if executeTestCase didn't save it
//                 TestResult failureResult = new TestResult();
//                 failureResult.setTestName(tc.getTestName());
//                 failureResult.setStatus(TestStatus.FAILED);
//                 failureResult.setErrorMessage("Exception: " + e.getMessage());
//                 failureResult.setTestRun(run);
//                 failureResult.setDuration(0L);
//                 failureResult.setRetryCount(0);
//                 failureResult.setCreatedAt(LocalDateTime.now());
//                 testResultService.saveTestResult(failureResult);
//             }
//         }

//         // FIXED #5: Suite execution completes, but system stays running
//         run.setStatus(failed > 0 ? TestStatus.FAILED : TestStatus.PASSED);
//         runService.updateTestRun(run);

//         // FIXED #2: Update suite status based on actual results
//         suiteService.updateSuiteStatus(request.getTestSuiteId());

//         System.out.println("Suite execution complete: " + executed + " executed, " + 
//             passed + " passed, " + failed + " failed");
//         System.out.println("System remains active, ready for next suite...");
//     }

//     private void updateTestRun(TestRun tr, TestStatus status) {
//         tr.setStatus(status);
//         testRunRepository.save(tr);
//     }

//     private void saveResult(TestRun tr, TestStatus status, long duration, int retryCount) {
//         TestResult r = new TestResult();
//         r.setTestName(tr.getName());
//         r.setStatus(status);
//         r.setDuration(duration);
//         r.setCreatedAt(LocalDateTime.now());
//         r.setTestRun(tr);
//         r.setRetryCount(retryCount);
//         testResultService.saveTestResult(r);
//         System.out.println("Saved TestResult for TestRun ID: " + tr.getId()
//                 + " | Status: " + status + " | Duration: " + duration + "ms | Retries: " + retryCount);
//     }
// }