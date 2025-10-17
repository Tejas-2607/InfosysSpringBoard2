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
        System.out.println("Received test run request: " + request); // Raw log
        try {
            testExecutor.executeTest(request);
            // Update TestRun status if present (handle missing)
            Optional<TestRun> optionalTestRun = testRunService.getTestRunById(request.getTestId());
            if (optionalTestRun.isPresent()) {
                TestRun testRun = optionalTestRun.get();
                testRun.setStatus("COMPLETED");
                testRunService.createTestRun(testRun); // Reuse save for update
                System.out.println("Finished processing and updated status for TestRun ID: " + request.getTestId()); // Log
            } else {
                System.err.println("TestRun ID " + request.getTestId() + " not found; skipping update");
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage()); // Raw log
            // No throw to avoid retry loop
        }
    }
}