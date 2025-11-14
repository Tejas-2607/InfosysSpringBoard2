package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * ANALYTICS ENHANCED: Additional queries for trends and flaky tests
 */
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

  /**
   * Daily pass rate for trend analysis.
   */
  @Query("SELECT DATE(r.createdAt), " +
      "SUM(CASE WHEN r.status = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / COUNT(r) " +
      "FROM TestResult r WHERE r.createdAt >= :since " +
      "GROUP BY DATE(r.createdAt) ORDER BY DATE(r.createdAt)")
  List<Object[]> findDailyPassRate(@Param("since") LocalDateTime since);

  /**
   * Last 10 test results for stability calculation.
   */
  @Query("SELECT r FROM TestResult r ORDER BY r.testRun.id DESC")
  List<TestResult> findTop10ByOrderByTestRunIdDesc();

  // List<TestResult> findByTestRunId(Long testRunId);
  /**
   * Find results by test run ID.
   */
  @Query("SELECT tr FROM TestResult tr WHERE tr.testRun.id = :runId")
  List<TestResult> findByTestRunId(Long runId);

  List<TestResult> findByTestSuiteId(Long testSuiteId);

  List<TestResult> findByStatus(TestStatus status);

  /**
   * Find results by test run ID and test name.
   */
  @Query("SELECT tr FROM TestResult tr WHERE tr.testRun.id = :runId AND tr.testName = :testName")
  List<TestResult> findByTestRunIdAndTestName(@Param("runId") Long runId, @Param("testName") String testName);

  /**
   * ANALYTICS: Count results by status for a test run.
   * Used for partial failure aggregation.
   */
  @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.testRun.id = :runId AND tr.status = :status")
  long countByRunIdAndStatus(@Param("runId") Long runId, @Param("status") TestStatus status);

  /**
   * ANALYTICS: Find flaky tests (high retry count).
   * Returns tests with retry_count > threshold.
   */
  @Query("SELECT tr FROM TestResult tr WHERE tr.retryCount > :threshold ORDER BY tr.flakyScore DESC")
  List<TestResult> findFlakyTests(@Param("threshold") int threshold);

  /**
   * ANALYTICS: Get average duration for a test name.
   * Used for flaky score calculation.
   */
  @Query("SELECT AVG(tr.duration) FROM TestResult tr WHERE tr.testName = :testName")
  Double findAvgDurationByTestName(@Param("testName") String testName);

  /**
   * ANALYTICS: Find tests with mixed results (both pass and fail).
   * Indicates unstable/flaky tests.
   */
  @Query("SELECT tr.testName, COUNT(DISTINCT tr.status) as statusCount " +
      "FROM TestResult tr " +
      "GROUP BY tr.testName " +
      "HAVING COUNT(DISTINCT tr.status) > 1")
  List<Object[]> findTestsWithMixedResults();

  @Query("SELECT r FROM TestResult r WHERE r.testSuite.id = :suiteId ORDER BY r.createdAt DESC")
  List<TestResult> findLatestBySuiteId(@Param("suiteId") Long suiteId);

  @Query("SELECT r FROM TestResult r WHERE r.createdAt BETWEEN :start AND :end")
  List<TestResult> findByCreatedAtBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
// package com.example.test_framework_api.repository;

// import com.example.test_framework_api.model.TestResult;

// import java.time.LocalDateTime;
// import java.util.List;
// import org.springframework.data.repository.query.Param;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;

// public interface TestResultRepository extends JpaRepository<TestResult, Long>
// {
// @Query("SELECT DATE(r.createdAt), " +
// "SUM(CASE WHEN r.status = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / COUNT(r) " +
// "FROM TestResult r WHERE r.createdAt >= :since " +
// "GROUP BY DATE(r.createdAt) ORDER BY DATE(r.createdAt)")
// List<Object[]> findDailyPassRate(@Param("since") LocalDateTime since);

// @Query("SELECT r FROM TestResult r ORDER BY r.testRun.id DESC")
// List<TestResult> findTop10ByOrderByTestRunIdDesc();

// @Query("SELECT tr FROM TestResult tr WHERE tr.testRun.id = :runId")
// List<TestResult> findByTestRunId(Long runId);

// /**
// * FIXED #2: New method to find results by test run ID and test name
// */
// @Query("SELECT tr FROM TestResult tr WHERE tr.testRun.id = :runId AND
// tr.testName = :testName")
// List<TestResult> findByTestRunIdAndTestName(@Param("runId") Long runId,
// @Param("testName") String testName);
// }