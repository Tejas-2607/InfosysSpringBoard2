package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRunRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.example.test_framework_api.TestFrameworkApiApplication.QUEUE;

@Component
public class WorkerListener {

    @Autowired
    private TestExecutor testExecutor;

    @RabbitListener(queues = QUEUE)
    public void receiveMessage(TestRunRequest request) {
        // Process message
        System.out.println("Received test run request: " + request);
        try {
            testExecutor.executeTest(request);
            // Acknowledge implicitly or manually if needed
        } catch (Exception e) {
            // Error handling: Throw to trigger retry/DLQ
            throw new RuntimeException("Failed to process test run", e);
        }
    }
}