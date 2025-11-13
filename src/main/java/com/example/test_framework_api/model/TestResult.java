package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ANALYTICS ENHANCED: Added flaky score for quick identification
 * Flaky score = retry_count * 10 + (fail_rate * 5) + (duration_seconds)
 */
@Entity
@Table(name = "test_result")
@Data
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;

    private Long duration;

    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "test_run_id")
    private TestRun testRun;

    /**
     * ANALYTICS: Flaky score indicator (higher = more flaky).
     * Calculated based on:
     * - Retry count (high retries = flaky)
     * - Duration (long tests more prone to timeouts)
     * - Failure patterns (tracked by service layer)
     */
    @Column(name = "flaky_score")
    private Double flakyScore = 0.0;

    /**
     * Calculate and set flaky score based on test metrics.
     * Called automatically after save in service layer.
     */
    public void calculateFlakyScore() {
        if (retryCount == null) retryCount = 0;
        if (duration == null) duration = 0L;
        
        // Formula: (retries * 10) + (duration_seconds)
        this.flakyScore = (retryCount * 10.0) + (duration / 1000.0);
    }

    /**
     * Auto-calculate flaky score before persist/update.
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        calculateFlakyScore();
    }
}
// package com.example.test_framework_api.model;

// import jakarta.persistence.*;
// import lombok.Data;
// import java.time.LocalDateTime;

// @Entity
// @Table(name = "test_result")
// @Data // Lombok: Generates getters/setters
// public class TestResult {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false)
//     private String testName;

//     @Enumerated(EnumType.STRING) // FIXED: Store as String in DB, but use enum
//     @Column(nullable = false)
//     private TestStatus status; // FIXED: Now enum, not String

//     private Long duration;

//     private Integer retryCount = 0;

//     @Column(name = "error_message", columnDefinition = "TEXT")
//     private String errorMessage;

//     @Column(name = "created_at")
//     private LocalDateTime createdAt = LocalDateTime.now();

//     @ManyToOne
//     @JoinColumn(name = "test_run_id")
//     private TestRun testRun;
// }