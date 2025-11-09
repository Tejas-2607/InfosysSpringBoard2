package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.test_framework_api.model.TestStatus;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
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
    List<TestResult> results = repo.findAll();
    long total = results.size();
    long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count(); // FIXED: == instead of
                                                                                            // .equals("PASSED")
    long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count(); // FIXED
    double passRate = total > 0 ? (passed * 100.0 / total) : 0;
    long avgDuration = (long) results.stream().mapToLong(TestResult::getDuration).average().orElse(0.0);
    double stability = (total - failed) * 100.0 / total; // Placeholder

    return new Summary(total, passed, failed, passRate, avgDuration, stability);
  }

  // Trend: pass-rate per day (last 7 days)
  public List<Object[]> getTrend7Days() {
    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
    return repo.findDailyPassRate(weekAgo);
  }
}