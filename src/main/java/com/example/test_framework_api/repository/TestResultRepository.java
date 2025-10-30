// src/main/java/com/example/test_framework_api/repository/TestResultRepository.java
package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {
}