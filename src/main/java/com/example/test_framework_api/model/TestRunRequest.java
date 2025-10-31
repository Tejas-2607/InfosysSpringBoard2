// src/main/java/com/example/test_framework_api/model/TestRunRequest.java
package com.example.test_framework_api.model;

public class TestRunRequest {
    private Long testId;
    private String suiteName;

    // Required constructor
    public TestRunRequest(Long testId, String suiteName) {
        this.testId = testId;
        this.suiteName = suiteName;
    }

    public TestRunRequest() {}

    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }

    public String getSuiteName() { return suiteName; }
    public void setSuiteName(String suiteName) { this.suiteName = suiteName; }
}