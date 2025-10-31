// src/main/java/com/example/test_framework_api/repository/TestRunRepository.java
package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {
}