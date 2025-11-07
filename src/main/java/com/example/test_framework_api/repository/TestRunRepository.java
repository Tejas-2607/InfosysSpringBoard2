// src/main/java/com/example/test_framework_api/repository/TestRunRepository.java
package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface TestRunRepository extends JpaRepository<TestRun, Long> {
  @Query("SELECT tr.testResults FROM TestRun tr WHERE tr.id = :testRunId")
  List<TestResult> findTestResultsByTestRunId(@Param("testRunId") Long testRunId);
}