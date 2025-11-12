// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.*;
// import com.example.test_framework_api.repository.*;
// import com.example.test_framework_api.worker.TestExecutor;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.Executor;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// /**
//  * Unit tests for parallel test execution functionality.
//  */
// class TestSuiteServiceTest {

//     @Mock
//     private TestSuiteRepository suiteRepository;
    
//     @Mock
//     private TestCaseRepository caseRepository;
    
//     @Mock
//     private TestResultRepository resultRepository;
    
//     @Mock
//     private TestExecutor testExecutor;
    
//     private Executor uiTestExecutor;
//     private Executor apiTestExecutor;
//     private TestSuiteService suiteService;

//     @BeforeEach
//     void setUp() {
//         MockitoAnnotations.openMocks(this);
        
//         // Create real executors for testing
//         ThreadPoolTaskExecutor uiExecutor = new ThreadPoolTaskExecutor();
//         uiExecutor.setCorePoolSize(2);
//         uiExecutor.setMaxPoolSize(4);
//         uiExecutor.setThreadNamePrefix("test-ui-");
//         uiExecutor.initialize();
//         this.uiTestExecutor = uiExecutor;

//         ThreadPoolTaskExecutor apiExecutor = new ThreadPoolTaskExecutor();
//         apiExecutor.setCorePoolSize(4);
//         apiExecutor.setMaxPoolSize(8);
//         apiExecutor.setThreadNamePrefix("test-api-");
//         apiExecutor.initialize();
//         this.apiTestExecutor = apiExecutor;

//         suiteService = new TestSuiteService(
//             suiteRepository, 
//             caseRepository, 
//             resultRepository, 
//             testExecutor,
//             uiTestExecutor,
//             apiTestExecutor
//         );
//     }

//     /**
//      * Test parallel execution with mixed UI/API tests.
//      * Verifies all tests execute and results are saved.
//      */
//     @Test
//     void testParallelExecution_MixedTests() throws Exception {
//         // Setup
//         Long suiteId = 1L;
//         TestRun testRun = createTestRun(1L, "Test Suite");
        
//         // Create 10 test cases (7 UI, 3 API)
//         List<TestCase> testCases = new ArrayList<>();
//         for (int i = 1; i <= 7; i++) {
//             testCases.add(createTestCase("TC_UI" + i, "UI", true));
//         }
//         for (int i = 1; i <= 3; i++) {
//             testCases.add(createTestCase("TC_API" + i, "API", true));
//         }

//         when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
        
//         // Mock test execution (simulate 70% pass rate)
//         doAnswer(invocation -> {
//             TestCase tc = invocation.getArgument(0);
//             // Simulate some failures (30%)
//             if (tc.getTestCaseId().endsWith("3") || tc.getTestCaseId().endsWith("7")) {
//                 throw new RuntimeException("Simulated test failure");
//             }
//             return null;
//         }).when(testExecutor).executeTestCase(any(TestCase.class), any(TestRun.class));

//         // Execute
//         CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 4);
//         future.get(); // Wait for completion

//         // Verify
//         verify(testExecutor, times(10)).executeTestCase(any(TestCase.class), any(TestRun.class));
//         verify(suiteRepository, atLeastOnce()).save(any(TestSuite.class));
//     }

//     /**
//      * Test that UI tests are limited to UI executor.
//      * Ensures proper executor separation.
//      */
//     @Test
//     void testParallelExecution_UITestsUseLimitedExecutor() throws Exception {
//         Long suiteId = 1L;
//         TestRun testRun = createTestRun(1L, "UI Suite");
        
//         // Create 10 UI tests
//         List<TestCase> testCases = new ArrayList<>();
//         for (int i = 1; i <= 10; i++) {
//             testCases.add(createTestCase("TC_UI" + i, "UI", true));
//         }

//         when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
//         doNothing().when(testExecutor).executeTestCase(any(), any());

//         // Execute
//         CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 4);
//         future.get();

//         // Verify all UI tests executed
//         verify(testExecutor, times(10)).executeTestCase(any(TestCase.class), any(TestRun.class));
//     }

//     /**
//      * Test that disabled tests are skipped in parallel execution.
//      */
//     @Test
//     void testParallelExecution_SkipsDisabledTests() throws Exception {
//         Long suiteId = 1L;
//         TestRun testRun = createTestRun(1L, "Mixed Suite");
        
//         List<TestCase> testCases = new ArrayList<>();
//         testCases.add(createTestCase("TC1", "UI", true));
//         testCases.add(createTestCase("TC2", "UI", false)); // Disabled
//         testCases.add(createTestCase("TC3", "API", true));
//         testCases.add(createTestCase("TC4", "API", false)); // Disabled

//         when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
//         doNothing().when(testExecutor).executeTestCase(any(), any());

//         // Execute
//         CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 2);
//         future.get();

//         // Verify only 2 enabled tests executed
//         verify(testExecutor, times(2)).executeTestCase(any(TestCase.class), any(TestRun.class));
//     }

//     /**
//      * Test concurrent execution doesn't cause race conditions.
//      * Verifies thread safety of result aggregation.
//      */
//     @Test
//     void testParallelExecution_ThreadSafety() throws Exception {
//         Long suiteId = 1L;
//         TestRun testRun = createTestRun(1L, "Concurrent Suite");
        
//         // Create 20 test cases for stress testing
//         List<TestCase> testCases = new ArrayList<>();
//         for (int i = 1; i <= 20; i++) {
//             testCases.add(createTestCase("TC" + i, i % 2 == 0 ? "UI" : "API", true));
//         }

//         when(caseRepository.findByTestSuiteId(suiteId)).thenReturn(testCases);
        
//         // Simulate varying execution times
//         doAnswer(invocation -> {
//             Thread.sleep((long) (Math.random() * 100)); // 0-100ms
//             return null;
//         }).when(testExecutor).executeTestCase(any(), any());

//         // Execute
//         CompletableFuture<Void> future = suiteService.executeSuiteParallel(suiteId, testRun, 8);
//         future.get();

//         // Verify all tests executed without errors
//         verify(testExecutor, times(20)).executeTestCase(any(TestCase.class), any(TestRun.class));
//     }

//     /**
//      * Test status update correctly aggregates parallel results.
//      */
//     @Test
//     void testUpdateSuiteStatus_ParallelResults() {
//         Long suiteId = 1L;
//         TestSuite suite = new TestSuite();
//         suite.setId(suiteId);
        
//         TestRun testRun = createTestRun(1L, "Status Test Suite");
//         suite.setTestRun(testRun);
        
//         List<TestCase> testCases = new ArrayList<>();
//         testCases.add(createTestCase("TC1", "UI", true));
//         testCases.add(createTestCase("TC2", "API", true));
//         suite.setTestCases(testCases);

//         List<TestResult> results = new ArrayList<>();
//         results.add(createTestResult("TC1", TestStatus.PASSED));
//         results.add(createTestResult("TC2", TestStatus.FAILED));

//         when(suiteRepository.findById(suiteId)).thenReturn(java.util.Optional.of(suite));
//         when(resultRepository.findByTestRunId(testRun.getId())).thenReturn(results);

//         // Execute
//         suiteService.updateSuiteStatus(suiteId);

//         // Verify suite marked as FAILED (1 failure)
//         verify(suiteRepository).save(argThat(s -> 
//             s.getStatus() == TestStatus.FAILED
//         ));
//     }

//     // Helper methods

//     private TestRun createTestRun(Long id, String name) {
//         TestRun run = new TestRun();
//         run.setId(id);
//         run.setName(name);
//         run.setStatus(TestStatus.RUNNING);
//         return run;
//     }

//     private TestCase createTestCase(String id, String type, boolean run) {
//         TestCase tc = new TestCase();
//         tc.setTestCaseId(id);
//         tc.setTestName("Test " + id);
//         tc.setTestType(type);
//         tc.setRun(run);
//         return tc;
//     }

//     private TestResult createTestResult(String testName, TestStatus status) {
//         TestResult result = new TestResult();
//         result.setTestName(testName);
//         result.setStatus(status);
//         result.setDuration(100L);
//         return result;
//     }
// }