package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.service.TestRunService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.example.test_framework_api.TestFrameworkApiApplication.QUEUE;

@Component
public class WorkerListener {

    @Autowired
    private TestExecutor testExecutor;

    @Autowired
    private TestRunService testRunService;

    @RabbitListener(queues = QUEUE)
    public void receiveMessage(TestRunRequest request) {
        System.out.println("Received test run request: " + request);
        Optional<TestRun> optionalTestRun = testRunService.getTestRunById(request.getTestId());
        if (!optionalTestRun.isPresent()) {
            System.err.println("TestRun ID " + request.getTestId() + " not found; skipping");
            return;
        }

        TestRun testRun = optionalTestRun.get();
        try {
            testExecutor.executeTest(request);
            testRun.setStatus("COMPLETED"); // Success case
        } catch (Exception e) {
            testRun.setStatus("FAILED"); // Fail after retries
            System.err.println("Test failed for ID " + request.getTestId() + ": " + e.getMessage());
        } finally {
            testRunService.createTestRun(testRun); // Update status in DB
            System.out.println("Updated status for TestRun ID: " + request.getTestId() + " to " + testRun.getStatus());
        }
    }
}