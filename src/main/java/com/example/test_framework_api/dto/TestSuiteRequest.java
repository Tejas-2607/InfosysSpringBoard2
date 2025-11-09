package com.example.test_framework_api.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TestSuiteRequest {
    private MultipartFile csvFile;  // NEW FEATURE: CSV upload
    private String suiteName;
    private String description;
}