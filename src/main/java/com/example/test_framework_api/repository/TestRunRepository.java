package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    @Modifying
    @Transactional  // Ensures transaction for this modifying query
    @Query("UPDATE TestRun t SET t.status = :status WHERE t.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);  // Returns affected rows (0 if none)

    Optional<TestRun> findById(Long id);
}