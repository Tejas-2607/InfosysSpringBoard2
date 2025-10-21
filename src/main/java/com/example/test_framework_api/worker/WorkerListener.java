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
            System.out.println("Executing test for ID: " + request.getTestId());
            testExecutor.executeTest(request);
            testRun.setStatus("COMPLETED"); // Set to COMPLETED on success
            System.out.println("Test execution succeeded for ID: " + request.getTestId());
        } catch (Exception e) {
            testRun.setStatus("FAILED"); // Set to FAILED on exception
            System.err.println("Test failed for ID " + request.getTestId() + ": " + e.getMessage());
        } finally {
            TestRun updatedRun = testRunService.createTestRun(testRun); // Save updated status
            System.out.println("Updated status for TestRun ID: " + updatedRun.getId() + " to " + updatedRun.getStatus());
        }
    }
}