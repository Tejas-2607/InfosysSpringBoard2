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
        if (optionalTestRun.isEmpty()) {
            System.err.println("TestRun with ID " + request.getTestId() + " not found. Cannot execute test.");
            return; 
        }
        TestRun testRun = optionalTestRun.get();

        try {
            
            testRun.setStatus("IN_PROGRESS");
            testRunService.updateTestRun(testRun);

            
            testExecutor.executeTest(request);
            
            
            testRun.setStatus("COMPLETED");

        } catch (Exception e) {
            System.err.println("Error processing message for TestRun ID " + request.getTestId() + ": " + e.getMessage());
            
            testRun.setStatus("FAILED");
        
        } finally {
            
            testRunService.updateTestRun(testRun);
            System.out.println("Finished processing and updated status for TestRun ID: " + request.getTestId());
        }
    }
}
