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
        try {
            testExecutor.executeTest(request);
            // Update TestRun status if found (handle missing ID)
            Optional<TestRun> optionalTestRun = testRunService.getTestRunById(request.getTestId());
            if (optionalTestRun.isPresent()) {
                TestRun testRun = optionalTestRun.get();
                testRun.setStatus("COMPLETED");
                testRunService.createTestRun(testRun); // Or add update method: testRunService.updateTestRun(testRun);
            } else {
                System.err.println("TestRun ID " + request.getTestId() + " not found; skipping update");
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            // Do NOT throw - this breaks retry loop. For production, log and nack message.
            // If retry needed, configure selective rethrow (e.g., only for transient errors).
        }
    }
}