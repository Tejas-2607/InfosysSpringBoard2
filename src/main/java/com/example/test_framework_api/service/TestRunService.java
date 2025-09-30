package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.repository.TestRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestRunService {

  @Autowired
  private TestRunRepository repository;

  public TestRun createTestRun(TestRun testRun) {
    // Business logic: Set initial status, etc.
    testRun.setStatus("PENDING");
    return repository.save(testRun);
  }

  public Optional<TestRun> getTestRunById(Long id) {
    return repository.findById(id);
  }

  public List<TestRun> getAllTestRuns() {
    return repository.findAll();
  }
}