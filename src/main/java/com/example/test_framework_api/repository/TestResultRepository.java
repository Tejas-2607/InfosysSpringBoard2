// src/main/java/com/example/test_framework_api/repository/TestResultRepository.java
package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestResult;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {
  @Query("SELECT DATE(r.createdAt), " +
      "SUM(CASE WHEN r.status = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / COUNT(r) " +
      "FROM TestResult r WHERE r.createdAt >= :since " +
      "GROUP BY DATE(r.createdAt) ORDER BY DATE(r.createdAt)")
  List<Object[]> findDailyPassRate(@Param("since") LocalDateTime since);

  @Query("SELECT r FROM TestResult r ORDER BY r.testRun.id DESC")
  List<TestResult> findTop10ByOrderByTestRunIdDesc();

  @Query("SELECT tr FROM TestResult tr WHERE tr.testRun.id = :runId")
    List<TestResult> findByTestRunId(Long testRunId);
}