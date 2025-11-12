// // src/main/java/com/example/test_framework_api/service/TestResultService.java
// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.repository.TestResultRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class TestResultService {

//     @Autowired
//     private TestResultRepository testResultRepository;

//     public List<TestResult> getAllTestResults() {
//         return testResultRepository.findAll();
//     }

//     public TestResult saveTestResult(TestResult testResult) {
//         return testResultRepository.save(testResult);
//     }
// }

package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestResultService {

    @Autowired
    private TestResultRepository testResultRepository;

    public List<TestResult> getAllTestResults() {
        return testResultRepository.findAll();
    }

    public TestResult saveTestResult(TestResult testResult) {
        // FIXED #2: Ensure results are actually saved with proper foreign key
        if (testResult.getTestRun() == null) {
            System.err.println("WARNING: Attempting to save TestResult without TestRun linkage!");
        }
        TestResult saved = testResultRepository.save(testResult);
        System.out.println("Saved TestResult ID " + saved.getId() + " for test: " + 
            saved.getTestName() + " | Status: " + saved.getStatus());
        return saved;
    }

    /**
     * FIXED #2: New method to find results by run ID and test name
     */
    public List<TestResult> findByTestRunIdAndTestName(Long testRunId, String testName) {
        return testResultRepository.findByTestRunIdAndTestName(testRunId, testName);
    }
}