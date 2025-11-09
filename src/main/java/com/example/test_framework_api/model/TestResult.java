// // src/main/java/com/example/test_framework_api/model/TestResult.java
// package com.example.test_framework_api.model;

// import jakarta.persistence.*;
// import java.time.LocalDateTime;
// import lombok.Data;

// @Entity
// @Table(name = "test_result")
// @Data
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

//     @Column(name = "error_message", length = 1000)
//     private String errorMessage;

//     public Long getId() {
//         return id;
//     }

//     public void setId(Long id) {
//         this.id = id;
//     }

//     public String getTestName() {
//         return testName;
//     }

//     public void setTestName(String testName) {
//         this.testName = testName;
//     }

//     public TestStatus getStatus() {
//         return status;
//     }

//     public void setStatus(TestStatus status) {
//         this.status = status;
//     }

//     public Long getDuration() {
//         return duration;
//     }

//     public void setDuration(Long duration) {
//         this.duration = duration;
//     }

//     public Integer getRetryCount() {
//         return retryCount;
//     }

//     public void setRetryCount(Integer retryCount) {
//         this.retryCount = retryCount;
//     }

//     public String getErrorMessage() {
//         return errorMessage;
//     }

//     public void setErrorMessage(String errorMessage) {
//         this.errorMessage = errorMessage;
//     }

//     public LocalDateTime getCreatedAt() {
//         return createdAt;
//     }

//     public void setCreatedAt(LocalDateTime createdAt) {
//         this.createdAt = createdAt;
//     }

//     public TestRun getTestRun() {
//         return testRun;
//     }

//     public void setTestRun(TestRun testRun) {
//         this.testRun = testRun;
//     }

//     @Column(name = "created_at")
//     private LocalDateTime createdAt = LocalDateTime.now();

//     @ManyToOne
//     @JoinColumn(name = "test_run_id")
//     private TestRun testRun;
// }

// src/main/java/com/example/test_framework_api/model/TestResult.java
package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_result")
@Data // Lombok: Generates getters/setters
public class TestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String testName;

    @Enumerated(EnumType.STRING) // FIXED: Store as String in DB, but use enum
    @Column(nullable = false)
    private TestStatus status; // FIXED: Now enum, not String

    private Long duration;

    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "test_run_id")
    private TestRun testRun;
}