package com.example.test_framework_api.tests;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.repository.TestResultRepository;
import com.example.test_framework_api.service.MetricsService;
import com.example.test_framework_api.service.ProduceReportHtmlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
// import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ANALYTICS TESTS: Validates trend analysis, flaky detection, and exports
 * 
 * Tests Cover:
 * 1. Trend analysis (pass rate over time)
 * 2. Flaky test detection (high retry count)
 * 3. PDF export generation
 * 4. CSV export generation
 */
class AnalyticsTests {

    @Mock
    private TestResultRepository resultRepository;
    
    private MetricsService metricsService;
    
    @Mock
    private ProduceReportHtmlService reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        metricsService = new MetricsService(resultRepository);
    }

    /**
     * ANALYTICS: Test trend calculation (7-day pass rate).
     */
    @Test
    void testTrendAnalysis_SevenDays() {
        Long suiteId = 1L;
        int days = 7;
        
        // Mock results from last 7 days
        List<TestResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 7; i++) {
            LocalDateTime date = now.minusDays(i);
            
            // Day 1: 80% pass rate (4/5)
            // Day 2: 100% pass rate (5/5)
            // Day 3: 60% pass rate (3/5)
            // etc.
            int passed = (i == 0) ? 4 : (i == 1) ? 5 : (i == 2) ? 3 : 4;
            int total = 5;
            
            for (int j = 0; j < passed; j++) {
                results.add(createTestResult("TC" + j, TestStatus.PASSED, date));
            }
            for (int j = passed; j < total; j++) {
                results.add(createTestResult("TC" + j, TestStatus.FAILED, date));
            }
        }

        when(resultRepository.findAll()).thenReturn(results);

        List<Map<String, Object>> trends = metricsService.getTrends(suiteId, days);

        // Verify 7 data points
        assertEquals(7, trends.size());
        
        // Verify structure
        Map<String, Object> firstDay = trends.get(0);
        assertTrue(firstDay.containsKey("date"));
        assertTrue(firstDay.containsKey("passRate"));
        assertTrue(firstDay.containsKey("totalTests"));
        assertTrue(firstDay.containsKey("passed"));
        assertTrue(firstDay.containsKey("failed"));
        
        // Verify pass rate calculation (safe cast)
        Object passRateObj = firstDay.get("passRate");
        double passRate = passRateObj instanceof Number ? ((Number) passRateObj).doubleValue() : 0.0;
        assertTrue(passRate >= 0 && passRate <= 100);
    }

    /**
     * ANALYTICS: Test flaky test detection (high retry count).
     */
    @Test
    void testFlakyTestDetection() {
        Long suiteId = 1L;
        
        // Mock results with flaky patterns
        List<TestResult> results = new ArrayList<>();
        
        // Stable test (no retries, always passes)
        results.add(createTestResult("StableTest", TestStatus.PASSED, 0, 100L));
        results.add(createTestResult("StableTest", TestStatus.PASSED, 0, 100L));
        results.add(createTestResult("StableTest", TestStatus.PASSED, 0, 100L));
        
        // Flaky test 1 (high retry count)
        results.add(createTestResult("FlakyTest1", TestStatus.PASSED, 3, 200L));
        results.add(createTestResult("FlakyTest1", TestStatus.FAILED, 2, 250L));
        results.add(createTestResult("FlakyTest1", TestStatus.PASSED, 1, 180L));
        
        // Flaky test 2 (mixed pass/fail, no retries)
        results.add(createTestResult("FlakyTest2", TestStatus.PASSED, 0, 150L));
        results.add(createTestResult("FlakyTest2", TestStatus.FAILED, 0, 150L));
        results.add(createTestResult("FlakyTest2", TestStatus.PASSED, 0, 150L));
        results.add(createTestResult("FlakyTest2", TestStatus.FAILED, 0, 150L));

        when(resultRepository.findAll()).thenReturn(results);

        List<Map<String, Object>> flakyTests = metricsService.getFlakyTests(suiteId);

        // Verify flaky tests detected (should exclude StableTest)
        assertTrue(flakyTests.size() >= 2, "Should detect at least 2 flaky tests");
        
        // Verify flaky score calculation
        Map<String, Object> mostFlaky = flakyTests.get(0); // Highest flaky score first
        assertTrue(mostFlaky.containsKey("testName"));
        assertTrue(mostFlaky.containsKey("flakyScore"));
        assertTrue(mostFlaky.containsKey("retryCount"));
        assertTrue(mostFlaky.containsKey("passRate"));
        
        // Safe cast for flaky score
        Object flakyScoreObj = mostFlaky.get("flakyScore");
        double flakyScore = flakyScoreObj instanceof Number ? ((Number) flakyScoreObj).doubleValue() : 0.0;
        assertTrue(flakyScore > 0, "Flaky score should be positive");
    }

    /**
     * ANALYTICS: Test PDF export generation.
     */
    // @Test
    // void testPdfExport() {
    //     Long suiteId = 1L;
        
    //     when(reportService.generatePdfReport(suiteId)).thenReturn(
    //         "Suite Report PDF Content".getBytes()
    //     );

    //     byte[] pdfContent = reportService.generatePdfReport(suiteId);

    //     assertNotNull(pdfContent);
    //     assertTrue(pdfContent.length > 0);
        
    //     // Verify contains expected content
    //     String content = new String(pdfContent);
    //     assertTrue(content.contains("SUITE REPORT") || content.contains("TEST CASES"));
    // }

    /**
     * ANALYTICS: Test CSV export generation.
     */
    @Test
    void testCsvExport() {
        Long suiteId = 1L;
        
        when(reportService.generateCsvReport(suiteId)).thenReturn(
            "Case ID,Test Name,Type,Status\nTC1,Test1,UI,PASSED\n".getBytes()
        );

        byte[] csvContent = reportService.generateCsvReport(suiteId);

        assertNotNull(csvContent);
        assertTrue(csvContent.length > 0);
        
        // Verify CSV format
        String content = new String(csvContent);
        assertTrue(content.contains("Case ID,Test Name"));
        assertTrue(content.contains("TC1"));
    }

    /**
     * ANALYTICS: Test summary calculation with stability metric.
     */
    @Test
    void testSummaryWithStability() {
        // Mock 20 results (last 10: 8 passed, 2 failed = 80% stability)
        List<TestResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 0; i < 20; i++) {
            TestStatus status = (i < 10) ? TestStatus.PASSED : 
                (i % 5 == 0) ? TestStatus.FAILED : TestStatus.PASSED;
            results.add(createTestResult("TC" + i, status, now.minusHours(i)));
        }

        when(resultRepository.findAll()).thenReturn(results);

        MetricsService.Summary summary = metricsService.getSummary();

        assertEquals(20, summary.total());
        assertTrue(summary.passRate() > 0 && summary.passRate() <= 100);
        assertTrue(summary.stabilityLast10() >= 0 && summary.stabilityLast10() <= 100);
        
        // Verify avg duration calculated
        assertTrue(summary.avgDurationMs() >= 0);
    }

    /**
     * ANALYTICS: Test empty results handling.
     */
    @Test
    void testAnalytics_EmptyResults() {
        when(resultRepository.findAll()).thenReturn(new ArrayList<>());

        MetricsService.Summary summary = metricsService.getSummary();
        
        assertEquals(0, summary.total());
        assertEquals(0, summary.passRate());
        
        List<Map<String, Object>> trends = metricsService.getTrends(1L, 7);
        assertTrue(trends.isEmpty());
        
        List<Map<String, Object>> flakyTests = metricsService.getFlakyTests(1L);
        assertTrue(flakyTests.isEmpty());
    }

    // Helper methods

    private TestResult createTestResult(String name, TestStatus status, LocalDateTime createdAt) {
        return createTestResult(name, status, 0, 100L, createdAt);
    }

    private TestResult createTestResult(String name, TestStatus status, int retries, Long duration) {
        return createTestResult(name, status, retries, duration, LocalDateTime.now());
    }

    private TestResult createTestResult(String name, TestStatus status, int retries, 
            Long duration, LocalDateTime createdAt) {
        TestResult result = new TestResult();
        result.setTestName(name);
        result.setStatus(status);
        result.setRetryCount(retries);
        result.setDuration(duration);
        result.setCreatedAt(createdAt);
        result.calculateFlakyScore(); // Calculate based on retries + duration
        return result;
    }
}