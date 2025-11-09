package com.example.test_framework_api.model;

public enum TestStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"), // FIXED: Added missing
    COMPLETED("COMPLETED"), // FIXED: Added missing
    FAILED("FAILED"),
    PASSED("PASSED");

    private final String value;

    TestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}