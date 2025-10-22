package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    @Modifying
    @Query("UPDATE TestRun tr SET tr.status = ?2 WHERE tr.id = ?1")
    void updateStatus(Long id, String status);

    Optional<TestRun> findById(Long id);
}