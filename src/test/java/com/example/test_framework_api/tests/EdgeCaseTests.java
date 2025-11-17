package com.example.test_framework_api.tests;

import com.example.test_framework_api.model.*;
import com.example.test_framework_api.repository.*;
import com.example.test_framework_api.service.TestSuiteService;
import com.example.test_framework_api.service.TestRunService;
import com.example.test_framework_api.worker.TestExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * EDGE CASE TESTS: Validates handling of edge cases in parallel execution
 * 
 * Tests Cover:
 * 1. threads=1 (Sequential fallback)
 * 2. Invalid thread counts (0, negative, >8)
 * 3. Empty suite handling
 * 4. Mixed failure aggregation
 */
class EdgeCaseTests {

    @Mock
    private TestSuiteRepository suiteRepository;

    @Mock
    private TestCaseRepository caseRepository;

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private TestExecutor testExecutor;

    @Mock
    private TestRunService runService;

    private Executor uiTestExecutor;
    private Executor apiTestExecutor;
    private TestSuiteService suiteService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create real executors for testing
        ThreadPoolTaskExecutor uiExecutor = new ThreadPoolTaskExecutor();
        uiExecutor.setCorePoolSize(2);
        uiExecutor.setMaxPoolSize(4);
        uiExecutor.setThreadNamePrefix("test-ui-");
        uiExecutor.initialize();
        this.uiTestExecutor = uiExecutor;

        ThreadPoolTaskExecutor apiExecutor = new ThreadPoolTaskExecutor();
        apiExecutor.setCorePoolSize(4);
        apiExecutor.setMaxPoolSize(8);
        apiExecutor.setThreadNamePrefix("test-api-");
        apiExecutor.initialize();
        this.apiTestExecutor = apiExecutor;

        suiteService = new TestSuiteService(
                suiteRepository,
                caseRepository,
                resultRepository,
                testExecutor,
                uiTestExecutor,
                apiTestExecutor,
                userRepository);
    }

    /**
     * EDGE CASE 1: threads=1 should execute sequentially (no async overhead).
     */
    @Test
    void testSequentialFallback() throws Exception {
        Long suiteId = 1L;
        TestRun testRun = createTestRun(1L, "Sequential Suite");
        testRun.setParallelThreads(1);

        List<TestCase> testCases = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            testCases.add(createTestCase("TC" + i, "UI", true));
        }

        when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
        doNothing().when(testExecutor).executeTestCase(any(), any());

        // Mock results (all passed)
        when(resultRepository.findByTestRunIdAndTestName(any(), any())).thenReturn(
                List.of(createTestResult("TC1", TestStatus.PASSED)));

        CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 1);
        future.get();

        // Verify sequential execution (all 5 tests)
        verify(testExecutor, times(5)).executeTestCase(any(TestCase.class), any(TestRun.class));

        // Verify status update called
        verify(suiteRepository, atLeastOnce()).findById(suiteId);
    }

    /**
     * EDGE CASE 2: Invalid thread counts should default to 1.
     */
    @Test
    void testInvalidThreads_Zero() throws Exception {
        Long suiteId = 1L;
        TestRun testRun = createTestRun(1L, "Invalid Threads Suite");

        List<TestCase> testCases = List.of(createTestCase("TC1", "UI", true));
        when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
        doNothing().when(testExecutor).executeTestCase(any(), any());

        // Pass invalid threads (0)
        CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 0);
        future.get();

        // Should execute (defaults to 1)
        verify(testExecutor, times(1)).executeTestCase(any(), any());
    }

    @Test
    void testInvalidThreads_Negative() throws Exception {
        Long suiteId = 1L;
        TestRun testRun = createTestRun(1L, "Negative Threads Suite");

        List<TestCase> testCases = List.of(createTestCase("TC1", "API", true));
        when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
        doNothing().when(testExecutor).executeTestCase(any(), any());

        CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, -1);
        future.get();

        verify(testExecutor, times(1)).executeTestCase(any(), any());
    }

    @Test
    void testInvalidThreads_Exceed() throws Exception {
        Long suiteId = 1L;
        TestRun testRun = createTestRun(1L, "Exceed Threads Suite");

        List<TestCase> testCases = List.of(createTestCase("TC1", "UI", true));
        when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
        doNothing().when(testExecutor).executeTestCase(any(), any());

        // Pass threads > 8
        CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 10);
        future.get();

        verify(testExecutor, times(1)).executeTestCase(any(), any());
    }

    /**
     * EDGE CASE 3: Empty suite should return immediately with COMPLETE status.
     */
    @Test
    void testEmptySuite() throws Exception {
        Long suiteId = 1L;
        TestRun testRun = createTestRun(1L, "Empty Suite");

        // Mock empty test cases
        when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(new ArrayList<>());

        CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 4);
        future.get();

        // Verify no execution occurred
        verify(testExecutor, never()).executeTestCase(any(), any());

        // Verify status update called (should set COMPLETE)
        verify(suiteRepository, atLeastOnce()).findById(suiteId);
    }

    @Test
    void testAllDisabledSuite() throws Exception {
        Long suiteId = 1L;
        TestRun testRun = createTestRun(1L, "All Disabled Suite");

        // All test cases disabled
        List<TestCase> testCases = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            testCases.add(createTestCase("TC" + i, "UI", false)); // run=false
        }

        when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);

        CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 4);
        future.get();

        // Verify no execution
        verify(testExecutor, never()).executeTestCase(any(), any());
    }

    /**
     * EDGE CASE 4: Mixed failures should result in COMPLETED status (partial
     * success).
     */
    @Test
    void testMixedFailures() throws Exception {
        Long suiteId = 1L;
        TestSuite suite = new TestSuite();
        suite.setId(suiteId);

        TestRun testRun = createTestRun(1L, "Mixed Failures Suite");
        suite.setTestRun(testRun);

        // 5 test cases
        List<TestCase> testCases = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            testCases.add(createTestCase("TC" + i, "UI", true));
        }
        suite.setTestCases(testCases);

        // Mock results: 3 passed, 2 failed (60% pass rate)
        List<TestResult> results = new ArrayList<>();
        results.add(createTestResult("TC1", TestStatus.PASSED));
        results.add(createTestResult("TC2", TestStatus.PASSED));
        results.add(createTestResult("TC3", TestStatus.FAILED));
        results.add(createTestResult("TC4", TestStatus.PASSED));
        results.add(createTestResult("TC5", TestStatus.FAILED));

        when(suiteRepository.findById(suiteId)).thenReturn(java.util.Optional.of(suite));
        when(resultRepository.findByTestRunId(testRun.getId())).thenReturn(results);

        // Execute status update
        suiteService.updateSuiteStatus(suiteId);

        // Verify suite marked as COMPLETED (not PASSED, not FAILED)
        verify(suiteRepository).save(argThat(s -> s.getStatus() == TestStatus.COMPLETED));
    }

    @Test
    void testAllFailures() throws Exception {
        Long suiteId = 1L;
        TestSuite suite = new TestSuite();
        suite.setId(suiteId);

        TestRun testRun = createTestRun(1L, "All Failures Suite");
        suite.setTestRun(testRun);

        List<TestCase> testCases = List.of(
                createTestCase("TC1", "UI", true),
                createTestCase("TC2", "API", true));
        suite.setTestCases(testCases);

        // All failed
        List<TestResult> results = List.of(
                createTestResult("TC1", TestStatus.FAILED),
                createTestResult("TC2", TestStatus.FAILED));

        when(suiteRepository.findById(suiteId)).thenReturn(java.util.Optional.of(suite));
        when(resultRepository.findByTestRunId(testRun.getId())).thenReturn(results);

        suiteService.updateSuiteStatus(suiteId);

        // Verify suite marked as FAILED
        verify(suiteRepository).save(argThat(s -> s.getStatus() == TestStatus.FAILED));
    }

    // Helper methods

    private TestRun createTestRun(Long id, String name) {
        TestRun run = new TestRun();
        run.setId(id);
        run.setName(name);
        run.setStatus(TestStatus.RUNNING);
        return run;
    }

    private TestCase createTestCase(String id, String type, boolean run) {
        TestCase tc = new TestCase();
        tc.setTestCaseId(id);
        tc.setTestName("Test " + id);
        tc.setTestType(type);
        tc.setRun(run);
        return tc;
    }

    private TestResult createTestResult(String testName, TestStatus status) {
        TestResult result = new TestResult();
        result.setTestName(testName);
        result.setStatus(status);
        result.setDuration(100L);
        result.setRetryCount(0);
        return result;
    }
}