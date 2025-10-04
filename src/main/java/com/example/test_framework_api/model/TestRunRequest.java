package com.example.test_framework_api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Added for constructor
@JsonIgnoreProperties(ignoreUnknown = true) // For backward compatibility
public class TestRunRequest {
    private Long testId;
    private String suiteName;
    // Add more fields as needed, ensure optional for compatibility
}