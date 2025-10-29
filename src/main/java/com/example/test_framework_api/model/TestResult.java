package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "test_result")
@Data  // Lombok for getters/setters
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String testName;

    private String status;

    private Long duration;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Fixed: Only use relationship for FK; no separate scalar field
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")  // Single mapping to physical column
    private TestRun testRun;
}