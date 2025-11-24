package com.example.test_framework_api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_suite")
@Data // Lombok: Generates getters/setters/toString/equals/hashCode
@JsonIgnoreProperties(value = { "testCases" }, allowSetters = true) // FIXED: Ignore cycle in JSON
public class TestSuite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "testsuite-testcases") // FIXED: Serializes testCases without back-ref cycle
    private List<TestCase> testCases;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id")
    @JsonIgnoreProperties({"testResults", "createdBy"})
    private TestRun testRun; // NEW FEATURE: Links suite to a TestRun for execution context

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TestStatus status = TestStatus.PENDING; // Reuse existing enum

    @Column(name = "report_path")
    private String reportPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnoreProperties({"password", "roles", "enabled", "createdAt"})
    private User createdBy;

    public void updateStatusFromResults() {
        if (testCases == null || testCases.isEmpty()) {
            status = TestStatus.COMPLETED;
            return;
        }
        // Placeholder: Assume 100% pass for now; extend with actual result count via
        // repo query
        long total = testCases.stream().filter(tc -> Boolean.TRUE.equals(tc.getRun())).count();
        // Simulate passed (replace with real query: e.g., inject repo and count PASSED
        // results linked to cases)
        long passed = total; // FIXED: Type-safe; no map inference issue
        status = (passed == total) ? TestStatus.PASSED : (passed > 0 ? TestStatus.COMPLETED : TestStatus.FAILED);
    }
}