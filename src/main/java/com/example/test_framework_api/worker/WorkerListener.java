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
        
        // Find the parent TestRun to update its status later
        Optional<TestRun> optionalTestRun = testRunService.getTestRunById(request.getTestId());
        if (optionalTestRun.isEmpty()) {
            System.err.println("TestRun with ID " + request.getTestId() + " not found. Cannot execute test.");
            return; // Exit if the parent run doesn't exist
        }
        TestRun testRun = optionalTestRun.get();

        try {
            // Mark as IN_PROGRESS before executing
            testRun.setStatus("IN_PROGRESS");
            testRunService.updateTestRun(testRun);

            // Execute the actual test logic
            testExecutor.executeTest(request);
            
            // Mark as COMPLETED if successful
            testRun.setStatus("COMPLETED");

        } catch (Exception e) {
            System.err.println("Error processing message for TestRun ID " + request.getTestId() + ": " + e.getMessage());
            // Mark as FAILED if an exception occurs during execution
            testRun.setStatus("FAILED");
        
        } finally {
            // **THE FIX**: Use the dedicated update method, NOT createTestRun
            testRunService.updateTestRun(testRun);
            System.out.println("Finished processing and updated status for TestRun ID: " + request.getTestId());
        }
    }
}
