package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, String> {
    List<TestCase> findByTestSuiteId(Long suiteId);  // NEW FEATURE: Fetch cases for execution
}