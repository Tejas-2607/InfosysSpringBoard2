package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetricsService {

  @Autowired
  private TestResultRepository repo;

  public record Summary(
      long total, long passed, long failed,
      double passRate, double avgDurationMs,
      double stabilityLast10 // % passed in last 10 runs
  ) {
  }

  public Summary getSummary() {
    List<TestResult> all = repo.findAll();
    long total = all.size();
    long passed = all.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
    long failed = total - passed;
    double passRate = total == 0 ? 0 : (passed * 100.0 / total);
    double avgDuration = all.stream()
        .mapToLong(r -> r.getDuration()) // long → no null
        .average()
        .orElse(0.0);
    // Stability: last 10 distinct test runs (by testRun.id)
    List<TestResult> last10 = repo.findTop10ByOrderByTestRunIdDesc();
    long stablePassed = last10.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
    double stability = last10.isEmpty() ? 0 : (stablePassed * 100.0 / last10.size());

    return new Summary(total, passed, failed, passRate, avgDuration, stability);
  }

  // Trend: pass-rate per day (last 7 days)
  public List<Object[]> getTrend7Days() {
    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
    return repo.findDailyPassRate(weekAgo);
  }
}