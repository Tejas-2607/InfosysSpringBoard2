package com.example.test_framework_api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; 
@Data
@NoArgsConstructor 
@AllArgsConstructor 
@JsonIgnoreProperties(ignoreUnknown = true) 
public class TestRunRequest {
    private Long testId;
    private String suiteName;
    
}