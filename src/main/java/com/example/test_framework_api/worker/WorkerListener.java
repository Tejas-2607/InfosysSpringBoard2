package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WorkerListener {

    @Autowired
    private TestExecutor testExecutor;

    @Autowired
    private TestRunRepository testRunRepository;

    @RabbitListener(queues = "testRunQueue")
    public void receiveMessage(TestRunRequest request) {
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
            testRunRepository.save(testRun); // Update status in DB
            System.out.println("Updated status for TestRun ID: " + request.getTestId() + " to " + testRun.getStatus());
        }
    }
}