package com.example.test_framework_api.dto;

// import java.util.List;

public record MetricsDto(
    long total, long passed, long failed,
    double passRate, double avgDurationMs, double stability
    // List<double[]> trend7Days [dayIndex, passRate]
    
) {}