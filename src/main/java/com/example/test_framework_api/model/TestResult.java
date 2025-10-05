package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "test_run_id")
    private TestRun testRun;

    private String testName;
    private String status; // PASS, FAIL, SKIPPED
    private Long duration; // ms
    private String errorMessage;
    private LocalDateTime createdAt;
}