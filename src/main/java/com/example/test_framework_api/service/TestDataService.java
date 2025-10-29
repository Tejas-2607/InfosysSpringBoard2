package com.example.test_framework_api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class TestDataService {
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Map<String, Object>> loadTestData(String filePath) {
        try {
            return mapper.readValue(new File(filePath), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data", e);
        }
    }
}