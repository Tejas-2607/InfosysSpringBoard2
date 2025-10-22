package com.example.test_framework_api.repository;

import com.example.test_framework_api.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    @Modifying
    @Query("UPDATE TestRun t SET t.status = :status WHERE t.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // Other methods (from service usage)
    Optional<TestRun> findById(Long id);
    List<TestRun> findAll();
}