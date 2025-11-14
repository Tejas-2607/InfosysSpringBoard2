package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FIXED: Null-safe metrics service with proper error handling
 */
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final TestResultRepository repo;

    public record Summary(
        long total, long passed, long failed,
        double passRate, double avgDurationMs,
        double stabilityLast10
    ) {}

    public Summary getSummary() {
        List<TestResult> results = repo.findAll();
        return calculateSummary(results);
    }

    public Summary getSummaryForSuite(Long suiteId) {
        List<TestResult> results = repo.findAll().stream()
            .filter(r -> r.getTestRun() != null)
            .collect(Collectors.toList());
        
        return calculateSummary(results);
    }

    /**
     * FIXED: Null-safe trend analysis with proper date handling
     */
    public List<Map<String, Object>> getTrends(Long suiteId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        List<TestResult> results = repo.findAll().stream()
            .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(since))
            .collect(Collectors.toList());
        
        if (results.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Group by date
        Map<String, List<TestResult>> byDate = results.stream()
            .collect(Collectors.groupingBy(
                r -> r.getCreatedAt().toLocalDate().toString()
            ));
        
        return byDate.entrySet().stream()
            .map(entry -> {
                String date = entry.getKey();
                List<TestResult> dayResults = entry.getValue();
                long total = dayResults.size();
                long passed = dayResults.stream()
                    .filter(r -> r.getStatus() == TestStatus.PASSED)
                    .count();
                double passRate = total > 0 ? (passed * 100.0 / total) : 0;
                
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", date);
                dataPoint.put("passRate", passRate);
                dataPoint.put("totalTests", total);
                dataPoint.put("passed", passed);
                dataPoint.put("failed", total - passed);
                return dataPoint;
            })
            .sorted((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")))
            .collect(Collectors.toList());
    }

    /**
     * FIXED: Null-safe flaky test detection with proper retry count handling
     */
    public List<Map<String, Object>> getFlakyTests(Long suiteId) {
        List<TestResult> results = repo.findAll();
        
        if (results.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Group by test name
        Map<String, List<TestResult>> byTest = results.stream()
            .collect(Collectors.groupingBy(TestResult::getTestName));
        
        return byTest.entrySet().stream()
            .filter(entry -> {
                List<TestResult> testResults = entry.getValue();
                if (testResults.size() < 2) return false;
                
                // FIXED: Null-safe retry count sum
                long retries = testResults.stream()
                    .mapToInt(r -> r.getRetryCount() != null ? r.getRetryCount() : 0)
                    .sum();
                
                long passes = testResults.stream()
                    .filter(r -> r.getStatus() == TestStatus.PASSED)
                    .count();
                
                long fails = testResults.stream()
                    .filter(r -> r.getStatus() == TestStatus.FAILED)
                    .count();
                
                return retries > 1 || (passes > 0 && fails > 0);
            })
            .map(entry -> {
                String testName = entry.getKey();
                List<TestResult> testResults = entry.getValue();
                
                long totalRuns = testResults.size();
                long passes = testResults.stream()
                    .filter(r -> r.getStatus() == TestStatus.PASSED)
                    .count();
                long fails = testResults.stream()
                    .filter(r -> r.getStatus() == TestStatus.FAILED)
                    .count();
                
                // FIXED: Null-safe retry count
                long retries = testResults.stream()
                    .mapToInt(r -> r.getRetryCount() != null ? r.getRetryCount() : 0)
                    .sum();
                
                // FIXED: Null-safe duration
                double avgDuration = testResults.stream()
                    .mapToLong(r -> r.getDuration() != null ? r.getDuration() : 0L)
                    .average()
                    .orElse(0);
                
                double flakyScore = (retries * 10) + 
                    ((fails * 100.0 / totalRuns) * 5) + 
                    (avgDuration / 1000.0);
                
                Map<String, Object> flakyData = new HashMap<>();
                flakyData.put("testName", testName);
                flakyData.put("totalRuns", totalRuns);
                flakyData.put("passes", passes);
                flakyData.put("fails", fails);
                flakyData.put("retryCount", retries);
                flakyData.put("passRate", totalRuns > 0 ? (passes * 100.0 / totalRuns) : 0);
                flakyData.put("avgDurationMs", avgDuration);
                flakyData.put("flakyScore", flakyScore);
                return flakyData;
            })
            .sorted((a, b) -> Double.compare(
                (Double) b.get("flakyScore"), 
                (Double) a.get("flakyScore")
            ))
            .collect(Collectors.toList());
    }

    public List<Object[]> getTrend7Days() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return repo.findDailyPassRate(weekAgo);
    }

    /**
     * FIXED: Null-safe summary calculation
     */
    private Summary calculateSummary(List<TestResult> results) {
        long total = results.size();
        long passed = results.stream()
            .filter(r -> r.getStatus() == TestStatus.PASSED)
            .count();
        long failed = results.stream()
            .filter(r -> r.getStatus() == TestStatus.FAILED)
            .count();
        
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        
        // FIXED: Null-safe duration calculation
        double avgDuration = results.stream()
            .mapToLong(r -> r.getDuration() != null ? r.getDuration() : 0L)
            .average()
            .orElse(0.0);
        
        // Stability from last 10
        List<TestResult> last10 = results.stream()
            .filter(r -> r.getCreatedAt() != null)
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(10)
            .collect(Collectors.toList());
        
        long last10Passed = last10.stream()
            .filter(r -> r.getStatus() == TestStatus.PASSED)
            .count();
        
        double stability = last10.size() > 0 ? (last10Passed * 100.0 / last10.size()) : 100;
        
        return new Summary(total, passed, failed, passRate, avgDuration, stability);
    }
}
// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.repository.TestResultRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// /**
//  * ANALYTICS ENHANCED: Advanced metrics and insights
//  * - Trend analysis (pass rate over time)
//  * - Flaky test detection (high retry count)
//  * - Suite-specific and global metrics
//  */
// @Service
// @RequiredArgsConstructor
// public class MetricsService {

//     private final TestResultRepository repo;

//     public record Summary(
//         long total, long passed, long failed,
//         double passRate, double avgDurationMs,
//         double stabilityLast10
//     ) {}

//     /**
//      * Global summary across all test results.
//      */
//     public Summary getSummary() {
//         List<TestResult> results = repo.findAll();
//         return calculateSummary(results);
//     }

//     /**
//      * ANALYTICS: Suite-specific summary.
//      * 
//      * @param suiteId Suite ID to analyze
//      * @return Summary metrics for the suite
//      */
//     public Summary getSummaryForSuite(Long suiteId) {
//         // Note: Requires TestResult to link to TestCase to link to TestSuite
//         // For simplicity, using TestRun linkage (suite sets testRun)
//         List<TestResult> results = repo.findAll().stream()
//             .filter(r -> r.getTestRun() != null)
//             .collect(Collectors.toList());
        
//         return calculateSummary(results);
//     }

//     /**
//      * ANALYTICS: Get pass rate trends over time.
//      * 
//      * @param suiteId Suite ID (0 for global)
//      * @param days Number of days to analyze
//      * @return List of trend data points [{date, passRate, totalTests}]
//      */
//     public List<Map<String, Object>> getTrends(Long suiteId, int days) {
//         LocalDateTime since = LocalDateTime.now().minusDays(days);
        
//         // Group results by date
//         List<TestResult> results = repo.findAll().stream()
//             .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(since))
//             .collect(Collectors.toList());
        
//         // Group by date and calculate pass rate
//         Map<String, List<TestResult>> byDate = results.stream()
//             .collect(Collectors.groupingBy(
//                 r -> r.getCreatedAt().toLocalDate().toString()
//             ));
        
//         return byDate.entrySet().stream()
//             .map(entry -> {
//                 String date = entry.getKey();
//                 List<TestResult> dayResults = entry.getValue();
//                 long total = dayResults.size();
//                 long passed = dayResults.stream()
//                     .filter(r -> r.getStatus() == TestStatus.PASSED)
//                     .count();
//                 double passRate = total > 0 ? (passed * 100.0 / total) : 0;
                
//                 // Use explicit HashMap<String, Object> to avoid type inference issues
//                 Map<String, Object> dataPoint = new HashMap<>();
//                 dataPoint.put("date", date);
//                 dataPoint.put("passRate", passRate);
//                 dataPoint.put("totalTests", total);
//                 dataPoint.put("passed", passed);
//                 dataPoint.put("failed", total - passed);
//                 return dataPoint;
//             })
//             .sorted((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")))
//             .collect(Collectors.toList());
//     }

//     /**
//      * ANALYTICS: Detect flaky tests (high retry count or unstable).
//      * 
//      * Flaky criteria:
//      * - Retry count > 1
//      * - High duration variance
//      * - Intermittent failures
//      * 
//      * @param suiteId Suite ID to analyze
//      * @return List of flaky test details
//      */
//     public List<Map<String, Object>> getFlakyTests(Long suiteId) {
//         List<TestResult> results = repo.findAll();
        
//         // Group by test name
//         Map<String, List<TestResult>> byTest = results.stream()
//             .collect(Collectors.groupingBy(TestResult::getTestName));
        
//         return byTest.entrySet().stream()
//             .filter(entry -> {
//                 List<TestResult> testResults = entry.getValue();
//                 if (testResults.size() < 2) return false; // Need history
                
//                 // Check for flakiness
//                 long retries = testResults.stream()
//                     .mapToInt(TestResult::getRetryCount)
//                     .sum();
                
//                 long passes = testResults.stream()
//                     .filter(r -> r.getStatus() == TestStatus.PASSED)
//                     .count();
                
//                 long fails = testResults.stream()
//                     .filter(r -> r.getStatus() == TestStatus.FAILED)
//                     .count();
                
//                 // Flaky if: retries > 1 OR (both passes and fails exist)
//                 return retries > 1 || (passes > 0 && fails > 0);
//             })
//             .map(entry -> {
//                 String testName = entry.getKey();
//                 List<TestResult> testResults = entry.getValue();
                
//                 long totalRuns = testResults.size();
//                 long passes = testResults.stream()
//                     .filter(r -> r.getStatus() == TestStatus.PASSED)
//                     .count();
//                 long fails = testResults.stream()
//                     .filter(r -> r.getStatus() == TestStatus.FAILED)
//                     .count();
//                 long retries = testResults.stream()
//                     .mapToInt(TestResult::getRetryCount)
//                     .sum();
                
//                 double avgDuration = testResults.stream()
//                     .mapToLong(TestResult::getDuration)
//                     .average()
//                     .orElse(0);
                
//                 // Calculate flaky score (higher = more flaky)
//                 double flakyScore = (retries * 10) + 
//                     ((fails * 100.0 / totalRuns) * 5) + 
//                     (avgDuration / 1000.0);
                
//                 // Use explicit HashMap<String, Object>
//                 Map<String, Object> flakyData = new HashMap<>();
//                 flakyData.put("testName", testName);
//                 flakyData.put("totalRuns", totalRuns);
//                 flakyData.put("passes", passes);
//                 flakyData.put("fails", fails);
//                 flakyData.put("retryCount", retries);
//                 flakyData.put("passRate", totalRuns > 0 ? (passes * 100.0 / totalRuns) : 0);
//                 flakyData.put("avgDurationMs", avgDuration);
//                 flakyData.put("flakyScore", flakyScore);
//                 return flakyData;
//             })
//             .sorted((a, b) -> Double.compare(
//                 (Double) b.get("flakyScore"), 
//                 (Double) a.get("flakyScore")
//             ))
//             .collect(Collectors.toList());
//     }

//     /**
//      * Global pass rate trend (7 days).
//      */
//     public List<Object[]> getTrend7Days() {
//         LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
//         return repo.findDailyPassRate(weekAgo);
//     }

//     /**
//      * Helper: Calculate summary from result list.
//      */
//     private Summary calculateSummary(List<TestResult> results) {
//         long total = results.size();
//         long passed = results.stream()
//             .filter(r -> r.getStatus() == TestStatus.PASSED)
//             .count();
//         long failed = results.stream()
//             .filter(r -> r.getStatus() == TestStatus.FAILED)
//             .count();
        
//         double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        
//         double avgDuration = results.stream()
//             .mapToLong(TestResult::getDuration)
//             .average()
//             .orElse(0.0);
        
//         // Stability: % of last 10 results that passed
//         List<TestResult> last10 = results.stream()
//             .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
//             .limit(10)
//             .collect(Collectors.toList());
        
//         long last10Passed = last10.stream()
//             .filter(r -> r.getStatus() == TestStatus.PASSED)
//             .count();
        
//         double stability = last10.size() > 0 ? (last10Passed * 100.0 / last10.size()) : 100;
        
//         return new Summary(total, passed, failed, passRate, avgDuration, stability);
//     }
// }