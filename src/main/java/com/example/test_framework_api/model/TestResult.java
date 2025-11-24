// package com.example.test_framework_api.model;

// import jakarta.persistence.*;
// import lombok.Data;
// import java.time.LocalDateTime;

// /**
//  * FIXED: Added test_suite_id foreign key to link results to suites
//  * This allows fetching results by suite for report generation
//  */
// @Entity
// @Table(name = "test_result")
// @Data
// public class TestResult {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false)
//     private String testName;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private TestStatus status;

//     private Long duration;

//     private Integer retryCount = 0;

//     @Column(name = "error_message", columnDefinition = "TEXT")
//     private String errorMessage;

//     @Column(name = "created_at")
//     private LocalDateTime createdAt = LocalDateTime.now();

//     @ManyToOne
//     @JoinColumn(name = "test_run_id")
//     private TestRun testRun;

//     /**
//      * CRITICAL FIX: Added test_suite_id to link results to suites
//      * Enables fetching results by suite for PDF/CSV/Report generation
//      */
//     @ManyToOne
//     @JoinColumn(name = "test_suite_id")
//     private TestSuite testSuite;

//     /**
//      * ANALYTICS: Flaky score indicator (higher = more flaky).
//      * Calculated based on:
//      * - Retry count (high retries = flaky)
//      * - Duration (long tests more prone to timeouts)
//      * - Failure patterns (tracked by service layer)
//      */
//     @Column(name = "flaky_score")
//     private Double flakyScore = 0.0;

//     /**
//      * Calculate and set flaky score based on test metrics.
//      * Called automatically after save in service layer.
//      */
//     public void calculateFlakyScore() {
//         if (retryCount == null) retryCount = 0;
//         if (duration == null) duration = 0L;
        
//         // Formula: (retries * 10) + (duration_seconds)
//         this.flakyScore = (retryCount * 10.0) + (duration / 1000.0);
//     }

//     /**
//      * Auto-calculate flaky score before persist/update.
//      */
//     @PrePersist
//     @PreUpdate
//     public void prePersist() {
//         calculateFlakyScore();
//     }
// }

package com.example.test_framework_api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_result")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")
    @JsonBackReference(value = "testrun-results")
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_suite_id")
    @JsonBackReference(value = "testsuite-results")
    private TestSuite testSuite;

    @Column(name = "flaky_score")
    private Double flakyScore = 0.0;

    // NEW: Track which user executed this test
    @ManyToOne
    @JoinColumn(name = "executed_by_user_id")
    @JsonIgnoreProperties({"password", "roles", "enabled", "createdAt"})
    private User executedBy;

    public void calculateFlakyScore() {
        if (retryCount == null) retryCount = 0;
        if (duration == null) duration = 0L;
        
        this.flakyScore = (retryCount * 10.0) + (duration / 1000.0);
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        calculateFlakyScore();
    }
}