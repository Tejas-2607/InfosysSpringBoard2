package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.service.TestRunService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            // Update TestRun status
            TestRun testRun = testRunService.getTestRunById(request.getTestId()).orElseThrow();
            testRun.setStatus("COMPLETED");
            testRunService.createTestRun(testRun); // Save update
        } catch (Exception e) {
            throw new RuntimeException("Failed to process test run", e);
        }
    }
}