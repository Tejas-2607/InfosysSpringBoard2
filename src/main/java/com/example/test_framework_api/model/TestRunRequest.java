package com.example.test_framework_api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestRunRequest {
    private final Long testId;
    private final String suiteName;

    @JsonCreator
    public TestRunRequest(@JsonProperty("testId") Long testId, @JsonProperty("suiteName") String suiteName) {
        this.testId = testId;
        this.suiteName = suiteName;
    }

    public Long getTestId() { return testId; }
    public String getSuiteName() { return suiteName; }

    @Override
    public String toString() {
        return "TestRunRequest{testId=" + testId + ", suiteName='" + suiteName + "'}";
    }
}