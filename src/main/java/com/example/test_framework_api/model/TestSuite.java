package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_suite")
@Data  // Lombok: Generates getters/setters/toString/equals/hashCode
public class TestSuite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestCase> testCases;

    @ManyToOne
    @JoinColumn(name = "test_run_id")
    private TestRun testRun;  // NEW FEATURE: Links suite to a TestRun for execution context

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TestStatus status = TestStatus.PENDING;  // Reuse existing enum
}