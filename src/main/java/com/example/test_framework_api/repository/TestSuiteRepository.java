package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    // NEW FEATURE: Custom queries can be added, e.g., findByName
}