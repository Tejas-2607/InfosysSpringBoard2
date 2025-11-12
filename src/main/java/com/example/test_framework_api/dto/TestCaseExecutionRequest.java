package com.example.test_framework_api.dto;

import lombok.Data;

/**
 * Request DTO for test case execution.
 * Now includes parallelThreads for concurrent execution.
 */
@Data
public class TestCaseExecutionRequest {
    private Long testSuiteId;
    private Long testRunId;
    
    /**
     * Number of concurrent threads for execution.
     * Default: 1 (sequential)
     * Range: 1-8 (validated in controller)
     */
    private int parallelThreads = 1;
}