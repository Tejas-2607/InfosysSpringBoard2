// package com.example.test_framework_api.model;

// import jakarta.persistence.*;
// import lombok.Data;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;

// /**
//  * Entity representing a test run.
//  * Now tracks parallel execution configuration.
//  */
// @Entity
// @Table(name = "test_run")
// @Data
// public class TestRun {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     private String name;
//     private String reportPath;
    
//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private TestStatus status = TestStatus.PENDING;

//     /**
//      * Number of parallel threads used for execution.
//      * Default: 1 (sequential)
//      * Range: 1-8
//      */
//     @Column(name = "parallel_threads")
//     private Integer parallelThreads = 1;

//     @Column(name = "created_at")
//     private LocalDateTime createdAt = LocalDateTime.now();

//     @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//     private List<TestResult> testResults = new ArrayList<>();
// }
package com.example.test_framework_api.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_run")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String reportPath;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status = TestStatus.PENDING;

    @Column(name = "parallel_threads")
    private Integer parallelThreads = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "testrun-results")
    private List<TestResult> testResults = new ArrayList<>();

    // NEW: Track which user created/executed this test run
    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnoreProperties({"password", "roles", "enabled", "createdAt"})
    private User createdBy;
}