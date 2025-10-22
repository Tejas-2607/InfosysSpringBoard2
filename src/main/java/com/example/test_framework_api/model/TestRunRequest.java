package com.example.test_framework_api.model;

public class TestRunRequest {

    private Long testId;
    private String suiteName;

    // Default constructor
    public TestRunRequest() {
    }

    // Parameterized constructor
    public TestRunRequest(Long testId, String suiteName) {
        this.testId = testId;
        this.suiteName = suiteName;
    }

    // Getters and Setters
    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    @Override
    public String toString() {
        return "TestRunRequest(testId=" + testId + ", suiteName=" + suiteName + ")";
    }
}