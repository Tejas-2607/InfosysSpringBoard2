package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkerListener {

    @Autowired
    private TestExecutor testExecutor;

    @Autowired
    private TestRunRepository testRunRepository;

    @RabbitListener(queues = "testRunQueue")
    public void receiveMessage(TestRunRequest request) throws Exception {
        System.out.println("Received test run request: " + request);

        // Check if the test run is already completed
        if (testRunRepository.findById(request.getTestId()).isPresent()) {
            String status = testRunRepository.findById(request.getTestId()).get().getStatus();
            if ("COMPLETED".equals(status)) {
                System.out.println("TestRun ID: " + request.getTestId() + " is already completed. Skipping.");
                return;
            }
        }

        // Update status to IN_PROGRESS
        testRunRepository.updateStatus(request.getTestId(), "IN_PROGRESS");
        System.out.println("Updated status for TestRun ID: " + request.getTestId() + " to IN_PROGRESS");

        try {
            testExecutor.executeTest(request);
            // Update status to COMPLETED on success
            testRunRepository.updateStatus(request.getTestId(), "COMPLETED");
            System.out.println("Test execution succeeded for ID: " + request.getTestId());
        } catch (Exception e) {
            // Update status to FAILED on failure
            testRunRepository.updateStatus(request.getTestId(), "FAILED");
            System.out.println("Test failed for ID " + request.getTestId() + ": " + e.getMessage());
            throw e; // Re-queue or handle failure as needed
        }
    }
}