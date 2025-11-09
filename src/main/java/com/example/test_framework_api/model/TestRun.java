package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_run")
@Data // FIXED: Added @Data for getters/setters (replaces manual ones)
public class TestRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // FIXED: Use for suiteName (setName(suiteName))

    @Enumerated(EnumType.STRING) // FIXED: Store as String in DB, but use enum
    @Column(nullable = false)
    private TestStatus status = TestStatus.PENDING; // FIXED: Enum, not String

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestResult> testResults = new ArrayList<>();
}