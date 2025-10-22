package com.example.test_framework_api.worker;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class WorkerListener {

    @Autowired
    private TestExecutor testExecutor;

    @Autowired
    private TestRunRepository testRunRepository;

    @RabbitListener(queues = "testRunQueue")
    @Transactional  // CRITICAL: Wraps entire method in transaction—DB updates now succeed, no TransactionRequiredException
    public void receiveMessage(TestRunRequest request) {  // Remove 'throws Exception'—handle inside
        System.out.println("Received test run request: " + request);

        // Idempotency check (fetch once, use Optional to avoid N+1 queries)
        Optional<TestRun> optionalRun = testRunRepository.findById(request.getTestId());
        if (optionalRun.isPresent()) {
            String status = optionalRun.get().getStatus();
            if ("COMPLETED".equals(status)) {
                System.out.println("TestRun ID: " + request.getTestId() + " is already completed. Skipping.");
                return;  // Early return: Ack message, no requeue
            }
        } else {
            System.out.println("TestRun ID: " + request.getTestId() + " not found. Skipping.");
            return;
        }

        // Update to IN_PROGRESS (now succeeds due to @Transactional)
        int updated = testRunRepository.updateStatus(request.getTestId(), "IN_PROGRESS");
        if (updated == 0) {
            System.out.println("No rows updated for ID: " + request.getTestId() + ". Possible race condition. Skipping.");
            return;
        }
        System.out.println("Updated status for TestRun ID: " + request.getTestId() + " to IN_PROGRESS");

        try {
            testExecutor.executeTest(request);
            testRunRepository.updateStatus(request.getTestId(), "COMPLETED");
            System.out.println("Test execution succeeded for ID: " + request.getTestId());
        } catch (Exception e) {
            testRunRepository.updateStatus(request.getTestId(), "FAILED");
            System.err.println("Test failed for ID " + request.getTestId() + ": " + e.getMessage());
            // Do NOT throw: Transaction commits "FAILED" status, message is ACKed (no requeue)
        }
    }
}