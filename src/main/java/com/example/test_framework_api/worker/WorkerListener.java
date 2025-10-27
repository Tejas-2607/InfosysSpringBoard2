package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import com.example.test_framework_api.service.TestResultService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
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
    private TestResultService testResultService; // New: Inject to save TestResult

    @RabbitListener(queues = "testRunQueue")
    public void receiveMessage(TestRunRequest request) {
        long startTime = System.currentTimeMillis();
        System.out.println("Received test run request: " + request);
        Optional<TestRun> optionalTestRun = testRunRepository.findById(request.getTestId());
        if (!optionalTestRun.isPresent()) {
            System.err.println("TestRun ID " + request.getTestId() + " not found; skipping");
            return;
        }

        TestRun testRun = optionalTestRun.get();
        try {
            System.out.println("Executing test for ID: " + request.getTestId());
            testExecutor.executeTest(request);
            testRun.setStatus("COMPLETED"); // Success
            System.out.println("Test execution succeeded for ID: " + request.getTestId());
        } catch (Exception e) {
            testRun.setStatus("FAILED"); // Failure
            System.err.println("Test failed for ID " + request.getTestId() + ": " + e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            testRunRepository.save(testRun); // Update status in DB
            System.out.println("Updated status for TestRun ID: " + request.getTestId() + " to " + testRun.getStatus());

            // New: Create and save TestResult for this run
            TestResult testResult = new TestResult();
            testResult.setTestName(request.getSuiteName()); // Use suiteName as testName
            testResult.setStatus(testRun.getStatus()); // Mirror the run status
            testResult.setDuration(duration);
            testResult.setCreatedAt(LocalDateTime.now());
            testResultService.saveTestResult(testResult);
            System.out.println("Saved TestResult for TestRun ID: " + request.getTestId());
        }
    }
}