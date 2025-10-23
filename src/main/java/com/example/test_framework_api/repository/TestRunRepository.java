package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    Optional<TestRun> findById(Long id);
}