package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    // NEW FEATURE: Custom queries can be added, e.g., findByName
    List<TestSuite> findByCreatedById(Long userId);

    @Query("SELECT s FROM TestSuite s LEFT JOIN FETCH s.createdBy")
    List<TestSuite> findAllWithCreator();
}