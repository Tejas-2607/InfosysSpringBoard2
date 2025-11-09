package com.example.test_framework_api.dto;

import lombok.Data;

@Data
public class TestCaseExecutionRequest {
    private Long testSuiteId;
    private Long testRunId;  // NEW FEATURE: Link to parent TestRun
}