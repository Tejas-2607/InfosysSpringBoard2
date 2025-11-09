// package com.example.test_framework_api.worker;

// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestRunRequest;
// import com.example.test_framework_api.repository.TestRunRepository;
// import com.example.test_framework_api.service.TestResultService;
// import org.springframework.amqp.AmqpRejectAndDontRequeueException;
// import org.springframework.amqp.core.AmqpTemplate;
// import org.springframework.amqp.rabbit.core.RabbitAdmin;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.retry.annotation.Backoff;
// import org.springframework.retry.annotation.Retryable;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.util.Optional;

// @Component
// public class WorkerListener {

//     @Autowired
//     private TestExecutor testExecutor;

//     @Autowired
//     private TestRunRepository testRunRepository;
//     @Autowired
//     private TestResultService testResultService;

//     @Autowired
//     private AmqpTemplate amqpTemplate;

//     @Autowired
//     private RabbitAdmin rabbitAdmin;

//     @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
//     @RabbitListener(queues = "testRunQueue")
//     public void receiveMessage(TestRunRequest request) throws Exception {
//         long startTime = System.currentTimeMillis();
//         System.out.println("Received test run request: " + request);
//         // Basic monitoring: Log queue depth
//         rabbitAdmin.getQueueProperties("testRunQueue").forEach((k, v) -> System.out.println("Queue " + k + ": " + v));
//         Optional<TestRun> optionalTestRun = testRunRepository.findById(request.getTestId());
//         if (!optionalTestRun.isPresent()) {
//             throw new AmqpRejectAndDontRequeueException("TestRun not found");
//         }

//         TestRun testRun = optionalTestRun.get();
//         try {
//             System.out.println("Executing test for ID: " + request.getTestId());
//             testExecutor.executeTest(request);
//             testRun.setStatus("COMPLETED");
//             System.out.println("Test execution succeeded for ID: " + request.getTestId());
//         } catch (Exception e) {
//             testRun.setStatus("FAILED");
//             System.err.println("Test failed for ID " + request.getTestId() + ": " + e.getMessage());
//             if (isPoisonMessage(e)) {
//                 routeToDLQ(request, e.getMessage());
//                 throw new AmqpRejectAndDontRequeueException("Poison message routed to DLQ");
//             }
//             throw e; // Retryable
//         } finally {
//             long duration = System.currentTimeMillis() - startTime;
//             testRunRepository.save(testRun);

//             // Fixed: Create and set the relationship
//             TestResult testResult = new TestResult();
//             testResult.setTestName(request.getSuiteName());
//             testResult.setStatus(testRun.getStatus());
//             testResult.setDuration(duration);
//             testResult.setCreatedAt(LocalDateTime.now());
//             testResult.setTestRun(testRun);  // Set the FK via relationship
//             testResultService.saveTestResult(testResult);
//             System.out.println("Saved TestResult for TestRun ID: " + request.getTestId());
//         }
//     }

//     private boolean isPoisonMessage(Exception e) {
//         return e.getMessage().contains("Invalid data");
//     }

//     private void routeToDLQ(TestRunRequest request, String errorMsg) {
//         System.err.println("Routing poison message to DLQ: " + errorMsg);
//         amqpTemplate.convertAndSend("testExchange", "dlq", request);
//     }
// }

// package com.example.test_framework_api.worker;

// import com.example.test_framework_api.model.TestRunRequest;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.repository.TestRunRepository;
// // import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;
// import org.springframework.beans.factory.annotation.Value;
// @Component
// public class WorkerListener {

//     @Autowired
//     private TestExecutor testExecutor;

//     @Autowired
//     private TestRunRepository testRunRepository;

//     @Value("${rabbitmq.queue.name}")
//     private String queueName;
//     public void receiveMessage(TestRunRequest request) throws Exception {
//         System.out.println("Received test run request for ID: " + request.getTestId());

//         // Get test run from database
//         TestRun testRun = testRunRepository.findById(request.getTestId())
//             .orElseThrow(() -> new RuntimeException("Test run not found: " + request.getTestId()));

//         try {
//             // Update status to IN_PROGRESS
//             testRun.setStatus("IN_PROGRESS");
//             testRunRepository.save(testRun);

//             // Execute the test
//             testExecutor.executeTest(request);

//             // Update status to PASSED if no exception
//             testRun.setStatus("PASSED");
//             testRunRepository.save(testRun);

//         } catch (Exception e) {
//             // Update status to FAILED on exception
//             testRun.setStatus("FAILED");
//             testRun.setReportPath("Error: " + e.getMessage());
//             testRunRepository.save(testRun);

//             // Optionally rethrow if you want the message to be rejected
//             throw e;
//         }
//     }
// }

// src/main/java/com/example/test_framework_api/worker/WorkerListener.java
// package com.example.test_framework_api.worker;

// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestRunRequest;
// import com.example.test_framework_api.repository.TestRunRepository;
// import com.example.test_framework_api.service.TestResultService;
// import org.springframework.amqp.AmqpRejectAndDontRequeueException;
// import org.springframework.amqp.core.AmqpTemplate;
// import org.springframework.amqp.rabbit.core.RabbitAdmin;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.retry.annotation.Backoff;
// import org.springframework.retry.annotation.Retryable;
// import org.springframework.retry.support.RetryTemplate;
// import org.springframework.stereotype.Component;

// import java.time.LocalDateTime;
// import java.util.Optional;

// @Component
// public class WorkerListener {
//     private final RetryTemplate retryTemplate;
//     @Autowired
//     private TestExecutor testExecutor;

//     @Autowired
//     private TestRunRepository testRunRepository;
//     @Autowired
//     private TestResultService testResultService;

//     @Autowired
//     private AmqpTemplate amqpTemplate;

//     @Autowired
//     private RabbitAdmin rabbitAdmin;

//     @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
//     @RabbitListener(queues = "testRunQueue", containerFactory = "rabbitListenerContainerFactory")
//     public void receiveMessage(TestRunRequest request) {
//         retryTemplate.execute(context -> {
//             System.out.println("Received test run request: " + request);
//             System.out.println("Retry count: " + context.getRetryCount());

//             TestRun testRun = testRunRepository.findById(request.getTestId())
//                     .orElseThrow(() -> new RuntimeException("TestRun not found: " + request.getTestId()));

//             System.out.println("Executing test for ID: " + testRun.getId());
//             testExecutor.executeTest(); // This throws on failure

//             updateTestRunStatus(testRun, TestStatus.PASSED);
//             saveTestResult(testRun, "Click Button Test", TestStatus.PASSED, 15);
//             return null;
//         }, recoveryContext -> {
//             // This runs AFTER 3 failed attempts
//             System.out.println("MAX RETRIES EXCEEDED. Moving to DLQ.");
//             TestRun testRun = testRunRepository.findById(request.getTestId()).orElse(null);
//             if (testRun != null) {
//                 updateTestRunStatus(testRun, TestStatus.FAILED);
//                 saveTestResult(testRun, "Click Button Test", TestStatus.FAILED, 15);
//             }
//             return null;
//         });
//     }
//     private boolean isPoisonMessage(Exception e) {
//         return e.getMessage().contains("Invalid data");
//     }

//     private void routeToDLQ(TestRunRequest request, String errorMsg) {
//         System.err.println("Routing poison message to DLQ: " + errorMsg);
//         amqpTemplate.convertAndSend("testRunExchange", "dlq", request);
//     }
// }

// @RabbitListener(queues = "testRunQueue", containerFactory = "rabbitListenerContainerFactory")
//     public void receiveMessage(TestRunRequest request) {
//         retryTemplate.execute(context -> {
//             System.out.println("Received test run request: " + request);
//             System.out.println("Retry count: " + context.getRetryCount());

//             TestRun testRun = testRunRepository.findById(request.getTestId())
//                     .orElseThrow(() -> new RuntimeException("TestRun not found: " + request.getTestId()));

//             System.out.println("Executing test for ID: " + testRun.getId());
//             testExecutor.executeTest(); // This throws on failure

//             updateTestRunStatus(testRun, TestStatus.PASSED);
//             saveTestResult(testRun, "Click Button Test", TestStatus.PASSED, 15);
//             return null;
//         }, recoveryContext -> {
//             // This runs AFTER 3 failed attempts
//             System.out.println("MAX RETRIES EXCEEDED. Moving to DLQ.");
//             TestRun testRun = testRunRepository.findById(request.getTestId()).orElse(null);
//             if (testRun != null) {
//                 updateTestRunStatus(testRun, TestStatus.FAILED);
//                 saveTestResult(testRun, "Click Button Test", TestStatus.FAILED, 15);
//             }
//             return null;
//         });

// @RabbitListener(queues = "testRunQueue",containerFactory = "rabbitListenerContainerFactory")  // Hardcoded to avoid placeholder issues
//     public void receiveMessage(TestRunRequest request) throws Exception {
//         long startTime = System.currentTimeMillis();
//         System.out.println("Received test run request: " + request);
//         // Basic monitoring: Log queue depth
//         rabbitAdmin.getQueueProperties("testRunQueue").forEach((k, v) -> System.out.println("Queue " + k + ": " + v));
//         Optional<TestRun> optionalTestRun = testRunRepository.findById(request.getTestId());
//         if (!optionalTestRun.isPresent()) {
//             throw new AmqpRejectAndDontRequeueException("TestRun not found");
//         }

//         TestRun testRun = optionalTestRun.get();
//         try {
//             System.out.println("Executing test for ID: " + request.getTestId());
//             testExecutor.executeTest();
//             testRun.setStatus("COMPLETED");
//             System.out.println("Test execution succeeded for ID: " + request.getTestId());
//         } catch (Exception e) {
//             testRun.setStatus("FAILED");
//             System.err.println("Test failed for ID: " + request.getTestId() + ": " + e.getMessage());
//             if (isPoisonMessage(e)) {
//                 routeToDLQ(request, e.getMessage());
//                 throw new AmqpRejectAndDontRequeueException("Poison message routed to DLQ");
//             }
//             throw e; // Retryable
//         } finally {
//             long duration = System.currentTimeMillis() - startTime;
//             testRunRepository.save(testRun);

//             TestResult testResult = new TestResult();
//             testResult.setTestName(request.getSuiteName());
//             testResult.setStatus(testRun.getStatus());
//             testResult.setDuration(duration);
//             testResult.setCreatedAt(LocalDateTime.now());
//             testResult.setTestRun(testRun);  // Set relationship
//             testResultService.saveTestResult(testResult);
//             System.out.println("Saved TestResult for TestRun ID: " + request.getTestId());
//         }
//     }

// @Component
// @RequiredArgsConstructor
// public class WorkerListener {

//     private final RetryTemplate retryTemplate;
//     private final TestRunService runService; // FIXED: Now wired
//     private final TestCaseRepository caseRepository; // FIXED: Now wired

//     @Autowired
//     private TestExecutor testExecutor;
//     @Autowired
//     private TestRunRepository testRunRepository;
//     @Autowired
//     private TestResultService testResultService;

//     // public WorkerListener(RetryTemplate retryTemplate) {
//     //     this.retryTemplate = retryTemplate;
//     // }

//     @RabbitListener(queues = QUEUE, containerFactory = "rabbitListenerContainerFactory")
//     public void receiveMessage(TestRunRequest request) {
//         long startTime = System.currentTimeMillis();
//         retryTemplate.execute(context -> {
//             System.out.println("Attempt #" + (context.getRetryCount() + 1)
//                     + " – processing TestRun " + request.getTestId());

//             TestRun testRun = testRunRepository.findById(request.getTestId())
//                     .orElseThrow(() -> new RuntimeException("TestRun not found: " + request.getTestId()));

//             testExecutor.executeTest(); // throws on failure
//             long duration = System.currentTimeMillis() - startTime;
//             updateTestRun(testRun, TestStatus.PASSED);
//             saveResult(testRun, TestStatus.PASSED, duration, context.getRetryCount());
//             return null;

//         }, recovery -> { // runs **after** max attempts
//             long duration = System.currentTimeMillis() - startTime;
//             System.out.println("MAX RETRIES EXCEEDED – marking FAILED and sending to DLQ");
//             TestRun testRun = testRunRepository.findById(request.getTestId()).orElse(null);
//             if (testRun != null) {
//                 updateTestRun(testRun, TestStatus.FAILED);
//                 saveResult(testRun, TestStatus.FAILED, duration, recovery.getRetryCount());
//             }
//             return null; // let container reject → DLQ
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
//                 : Long.valueOf(testRunIdObj.toString()); // FIXED: Safe cast for testRunId (handles Integer/Long/Object)

//         long startTime = System.currentTimeMillis();
//         try {
//             if (actionsList != null && !actionsList.isEmpty()) {
//                 // Multi-actions: Execute sequentially
//                 for (Map<String, Object> step : actionsList) {
//                     String stepAction = (String) step.get("type");
//                     String value = (String) step.get("value");
//                     testExecutor.executeDynamicTest(url, elementId, stepAction, value);
//                 }
//             } else if (action != null) {
//                 // Single action (backward compatible)
//                 testExecutor.executeDynamicTest(url, elementId, action, expectedResult);
//             } else {
//                 throw new IllegalArgumentException("No action provided");
//             }

//             // Success
//             TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
//             if (testRun != null) {
//                 updateTestRun(testRun, TestStatus.PASSED);
//                 saveResult(testRun, TestStatus.PASSED, System.currentTimeMillis() - startTime, 0);
//             }
//         } catch (Exception e) {
//             // Failure
//             long duration = System.currentTimeMillis() - startTime;
//             TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
//             if (testRun != null) {
//                 updateTestRun(testRun, TestStatus.FAILED);
//                 saveResult(testRun, TestStatus.FAILED, duration, 1);
//             }
//             System.err.println("Dynamic test FAILED: " + e.getMessage());
//         }
//     }

//     @RabbitListener(queues = RabbitMQConfig.TEST_SUITE_QUEUE)
//     public void handleSuiteExecution(TestCaseExecutionRequest request) {
//         TestRun run = runService.getTestRunById(request.getTestRunId()); // Assume getTestRunById exists
//         run.setStatus(TestStatus.RUNNING); // FIXED: Full enum
//         runService.updateTestRun(run); // FIXED: Now defined

//         List<TestCase> cases = caseRepository.findByTestSuiteId(request.getTestSuiteId());
//         for (TestCase tc : cases) {
//             if (tc.getRun()) {
//                 testExecutor.executeTestCase(tc, run);
//             }
//         }

//         run.setStatus(TestStatus.COMPLETED); // FIXED: Full enum
//         runService.updateTestRun(run); // FIXED: Now defined
//     }

//     private void updateTestRun(TestRun tr, TestStatus status) {
//         tr.setStatus(status.name());
//         testRunRepository.save(tr);
//     }

//     private void saveResult(TestRun tr, TestStatus status, long duration, int retryCount) {
//         TestResult r = new TestResult();
//         r.setTestName(tr.getName());
//         r.setStatus(status.name());
//         r.setDuration(duration); // you can compute real duration
//         r.setCreatedAt(LocalDateTime.now());
//         r.setTestRun(tr);
//         r.setRetryCount(retryCount); // <-- flakiness metric
//         testResultService.saveTestResult(r);
//         System.out.println("Saved TestResult for TestRun ID: " + tr.getId()
//                 + " | Status: " + status + " | Duration: " + duration + "ms | Retries: " + retryCount);
//     }
// }
package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.repository.TestRunRepository;
import com.example.test_framework_api.service.TestResultService;
// import com.example.test_framework_api.model.*;
// import org.springframework.retry.RetryContext;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.example.test_framework_api.config.RabbitMQConfig;
import com.example.test_framework_api.dto.TestCaseExecutionRequest;
import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.repository.TestCaseRepository;
import com.example.test_framework_api.service.TestRunService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import static com.example.test_framework_api.config.RabbitMQConfig.QUEUE;

@Component
@RequiredArgsConstructor
public class WorkerListener {

    private final RetryTemplate retryTemplate;
    private final TestRunService runService; // FIXED: Now wired
    private final TestCaseRepository caseRepository; // FIXED: Now wired
    private final TestExecutor testExecutor; // FIXED: Wired via RequiredArgsConstructor
    private final TestRunRepository testRunRepository; // FIXED: Wired
    private final TestResultService testResultService; // FIXED: Wired

    @RabbitListener(queues = QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(TestRunRequest request) {
        long startTime = System.currentTimeMillis();
        retryTemplate.execute(context -> {
            System.out.println("Attempt #" + (context.getRetryCount() + 1)
                    + " – processing TestRun " + request.getTestId());

            TestRun testRun = testRunRepository.findById(request.getTestId())
                    .orElseThrow(() -> new RuntimeException("TestRun not found: " + request.getTestId()));

            testExecutor.executeTest(); // throws on failure
            long duration = System.currentTimeMillis() - startTime;
            updateTestRun(testRun, TestStatus.PASSED); // FIXED: Pass enum
            saveResult(testRun, TestStatus.PASSED, duration, context.getRetryCount()); // FIXED: Pass enum
            return null;

        }, recovery -> { // runs **after** max attempts
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("MAX RETRIES EXCEEDED – marking FAILED and sending to DLQ");
            TestRun testRun = testRunRepository.findById(request.getTestId()).orElse(null);
            if (testRun != null) {
                updateTestRun(testRun, TestStatus.FAILED); // FIXED: Pass enum
                saveResult(testRun, TestStatus.FAILED, duration, recovery.getRetryCount()); // FIXED: Pass enum
            }
            return null; // let container reject → DLQ
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
                : Long.valueOf(testRunIdObj.toString()); // FIXED: Safe cast for testRunId (handles Integer/Long/Object)

        long startTime = System.currentTimeMillis();
        try {
            if (actionsList != null && !actionsList.isEmpty()) {
                // Multi-actions: Execute sequentially
                for (Map<String, Object> step : actionsList) {
                    String stepAction = (String) step.get("type");
                    String value = (String) step.get("value");
                    testExecutor.executeDynamicTest(url, elementId, stepAction, expectedResult,
                            value != null ? value : ""); // FIXED: Pass inputValue (value or "")
                }
            } else if (action != null) {
                // Single action (backward compatible)
                testExecutor.executeDynamicTest(url, elementId, action, expectedResult, ""); // FIXED: Pass empty
                                                                                             // inputValue
            } else {
                throw new IllegalArgumentException("No action provided");
            }

            // Success
            TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
            if (testRun != null) {
                updateTestRun(testRun, TestStatus.PASSED); // FIXED: Pass enum
                saveResult(testRun, TestStatus.PASSED, System.currentTimeMillis() - startTime, 0); // FIXED: Pass enum
            }
        } catch (Exception e) {
            // Failure
            long duration = System.currentTimeMillis() - startTime;
            TestRun testRun = testRunRepository.findById(testRunId).orElse(null);
            if (testRun != null) {
                updateTestRun(testRun, TestStatus.FAILED); // FIXED: Pass enum
                saveResult(testRun, TestStatus.FAILED, duration, 1); // FIXED: Pass enum
            }
            System.err.println("Dynamic test FAILED: " + e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TEST_SUITE_QUEUE)
    public void handleSuiteExecution(TestCaseExecutionRequest request) {
        TestRun run = runService.getTestRunById(request.getTestRunId()); // Assume getTestRunById exists
        run.setStatus(TestStatus.RUNNING); // FIXED: Full enum
        runService.updateTestRun(run); // FIXED: Now defined

        List<TestCase> cases = caseRepository.findByTestSuiteId(request.getTestSuiteId());
        for (TestCase tc : cases) {
            if (tc.getRun()) {
                testExecutor.executeTestCase(tc, run);
            }
        }

        run.setStatus(TestStatus.COMPLETED); // FIXED: Full enum
        runService.updateTestRun(run); // FIXED: Now defined
    }

    private void updateTestRun(TestRun tr, TestStatus status) {
        tr.setStatus(status); // FIXED: Pass enum directly (no .name())
        testRunRepository.save(tr);
    }

    private void saveResult(TestRun tr, TestStatus status, long duration, int retryCount) {
        TestResult r = new TestResult();
        r.setTestName(tr.getName());
        r.setStatus(status); // FIXED: Pass enum directly (no .name())
        r.setDuration(duration); // you can compute real duration
        r.setCreatedAt(LocalDateTime.now());
        r.setTestRun(tr);
        r.setRetryCount(retryCount); // <-- flakiness metric
        testResultService.saveTestResult(r);
        System.out.println("Saved TestResult for TestRun ID: " + tr.getId()
                + " | Status: " + status + " | Duration: " + duration + "ms | Retries: " + retryCount);
    }

    // FIXED: Suppress unchecked cast warning (assuming line 61 is the cast in
    // handleElementTest or similar)
    // @SuppressWarnings("unchecked")
    // private List<Map<String, Object>> castToList(Object obj) {
    // return (List<Map<String, Object>>) obj;
    // }
}