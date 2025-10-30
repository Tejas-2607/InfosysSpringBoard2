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
package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import com.example.test_framework_api.service.ReportService;
import com.example.test_framework_api.service.TestResultService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class WorkerListener {

    @Autowired
    private TestExecutor testExecutor;

    @Autowired
    private TestRunRepository testRunRepository;
    @Autowired
    private TestResultService testResultService;

    @Autowired
    private ReportService reportService;  // Added to set reportPath

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    @RabbitListener(queues = "testRunQueue")  // Hardcoded to avoid placeholder issues
    public void receiveMessage(TestRunRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        System.out.println("Received test run request: " + request);
        // Basic monitoring: Log queue depth
        rabbitAdmin.getQueueProperties("testRunQueue").forEach((k, v) -> System.out.println("Queue " + k + ": " + v));
        Optional<TestRun> optionalTestRun = testRunRepository.findById(request.getTestId());
        if (!optionalTestRun.isPresent()) {
            throw new AmqpRejectAndDontRequeueException("TestRun not found");
        }

        TestRun testRun = optionalTestRun.get();
        try {
            System.out.println("Executing test for ID: " + request.getTestId());
            testExecutor.executeTest();
            testRun.setStatus("COMPLETED");
            testRun.setReportPath(reportService.generateReport());  // Added to set reportPath
            System.out.println("Test execution succeeded for ID: " + request.getTestId());
        } catch (Exception e) {
            testRun.setStatus("FAILED");
            System.err.println("Test failed for ID: " + request.getTestId() + ": " + e.getMessage());
            if (isPoisonMessage(e)) {
                routeToDLQ(request, e.getMessage());
                throw new AmqpRejectAndDontRequeueException("Poison message routed to DLQ");
            }
            throw e; // Retryable
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            testRunRepository.save(testRun);

            TestResult testResult = new TestResult();
            testResult.setTestName(request.getSuiteName());
            testResult.setStatus(testRun.getStatus());
            testResult.setDuration(duration);
            testResult.setCreatedAt(LocalDateTime.now());
            testResult.setTestRun(testRun);  // Set relationship
            testResultService.saveTestResult(testResult);
            System.out.println("Saved TestResult for TestRun ID: " + request.getTestId());
        }
    }

    private boolean isPoisonMessage(Exception e) {
        return e.getMessage().contains("Invalid data");
    }

    private void routeToDLQ(TestRunRequest request, String errorMsg) {
        System.err.println("Routing poison message to DLQ: " + errorMsg);
        amqpTemplate.convertAndSend("testRunExchange", "dlq", request);
    }
}