// package com.example.test_framework_api.service;

// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.model.TestSuite;
// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.repository.TestCaseRepository;
// import com.example.test_framework_api.repository.TestResultRepository;
// import com.example.test_framework_api.repository.TestSuiteRepository;
// import com.opencsv.CSVReader;
// import com.opencsv.exceptions.CsvValidationException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.util.ArrayList;
// import java.util.List;

// @Service
// @RequiredArgsConstructor
// public class TestSuiteService {

//     private final TestSuiteRepository suiteRepository;
//     private final TestCaseRepository caseRepository;
//     private final TestResultRepository resultRepository;

//     // NEW FEATURE: Import CSV to create TestSuite with TestCases
//     public TestSuite importFromCsv(MultipartFile file, String suiteName, String description)
//             throws IOException, CsvValidationException {
//         if (file.isEmpty())
//             throw new IllegalArgumentException("CSV file is empty");
//         TestSuite suite = new TestSuite();
//         suite.setName(suiteName);
//         suite.setDescription(description);
//         suite = suiteRepository.save(suite);
//         // if (row.length != 12) {
//         // throw new IllegalArgumentException(
//         // "Row " + (cases.size() + 1) + " has " + row.length + " columns; expected
//         // 12.");
//         // }
//         List<TestCase> cases = new ArrayList<>();
//         try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
//             String[] headers = reader.readNext(); // Skip header
//             if (headers == null || headers.length < 12)
//                 throw new IllegalArgumentException("Invalid header: Expected 12+ columns");
//             String[] row;
//             int rowNum = 1;
//             while ((row = reader.readNext()) != null) {
//                 rowNum++;
//                 if (row.length < 12)
//                     throw new IllegalArgumentException(
//                             "Row " + rowNum + " has " + row.length + " columns; expected 12+"); // FIXED: Validation
//                 TestCase tc = new TestCase();
//                 tc.setTestCaseId(row[0]); // TestCaseID
//                 tc.setTestName(row[1]); // TestName
//                 tc.setTestType(row[2]); // TestType
//                 tc.setUrlEndpoint(row[3]); // URL/Endpoint
//                 // FIXED: Handle combined "click/id" - split if contains "/"
//                 String actionLocator = row[4]; // HTTP Method / Action
//                 if (actionLocator.contains("/")) {
//                     String[] parts = actionLocator.split("/", 2);
//                     tc.setHttpMethodAction(parts[0].trim()); // e.g., "click"
//                     tc.setLocatorType(parts[1].trim()); // e.g., "id"
//                 } else {
//                     tc.setHttpMethodAction(actionLocator);
//                 }
//                 tc.setLocatorType(row.length > 5 ? row[5] : ""); // LocatorType
//                 tc.setLocatorValue(row.length > 6 ? row[6] : ""); // LocatorValue
//                 tc.setInputData(row.length > 7 ? row[7] : ""); // InputData
//                 tc.setExpectedResult(row.length > 8 ? row[8] : ""); // ExpectedResult
//                 tc.setPriority(row.length > 9 ? row[9] : ""); // Priority
//                 // FIXED: Flexible Run parsing for "Yes"/"YES"/"true"
//                 String runStr = row.length > 10 ? row[10] : "false";
//                 tc.setRun("YES".equalsIgnoreCase(runStr) || "Yes".equalsIgnoreCase(runStr)
//                         || Boolean.parseBoolean(runStr));
//                 tc.setDescription(row.length > 11 ? row[11] : ""); // Description
//                 // NEW FEATURE: Multi-Action - If extra column (13+), set actionsJson
//                 if (row.length > 12)
//                     tc.setActionsJson(row[12]);
//                 tc.setTestSuite(suite); // Link to suite
//                 cases.add(tc);
//             }
//         }
//         caseRepository.saveAll(cases);
//         suite.setTestCases(cases);
//         suiteRepository.save(suite);
//         return suite; // Update with bidirectional link
//     }

//     public List<TestSuite> getAllSuites() {
//         return suiteRepository.findAll();
//     }

//     public TestSuite getSuiteById(Long id) {
//         return suiteRepository.findById(id).orElse(null);
//     }

//     public void updateSuiteStatus(Long suiteId) {
//         TestSuite suite = getSuiteById(suiteId);
//         if (suite != null && suite.getTestRun() != null) {
//             Long runId = suite.getTestRun().getId();
//             List<TestResult> results = resultRepository.findByTestRunId(runId);  // FIXED: Now defined, uses custom method
//             long total = suite.getTestCases().stream().filter(tc -> Boolean.TRUE.equals(tc.getRun())).count();
//             long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
//             suite.setStatus(passed == total ? TestStatus.PASSED : (passed > 0 ? TestStatus.COMPLETED : TestStatus.FAILED));
//             suiteRepository.save(suite);
//         }
//     }
// }

package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.repository.TestCaseRepository;
import com.example.test_framework_api.repository.TestResultRepository;
import com.example.test_framework_api.repository.TestSuiteRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestSuiteService {

    private final TestSuiteRepository suiteRepository;
    private final TestCaseRepository caseRepository;
    private final TestResultRepository resultRepository;

    /**
     * FIXED: Always create NEW suite with unique test cases, even if content is identical
     * Each upload gets fresh test_suite_id and test_case records
     */
    @Transactional
    public TestSuite importFromCsv(MultipartFile file, String suiteName, String description)
            throws IOException, CsvValidationException {
        if (file.isEmpty())
            throw new IllegalArgumentException("CSV file is empty");

        // FIXED #1: Always create NEW suite (never reuse old suite_id)
        TestSuite suite = new TestSuite();
        suite.setName(suiteName + " - " + System.currentTimeMillis()); // Unique name with timestamp
        suite.setDescription(description);
        suite.setStatus(TestStatus.PENDING);
        suite = suiteRepository.save(suite); // Save first to get suite_id

        List<TestCase> cases = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length < 12)
                throw new IllegalArgumentException("Invalid CSV: Expected 12+ columns");

            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                rowNum++;
                if (row.length < 12) {
                    System.err.println("Skipping invalid row " + rowNum + " (only " + row.length + " columns)");
                    continue;
                }

                // FIXED #1: Create NEW test case with unique ID for each upload
                TestCase tc = new TestCase();
                tc.setTestCaseId(row[0] + "-" + UUID.randomUUID().toString().substring(0, 8)); // Unique ID
                tc.setTestName(row[1]);
                tc.setTestType(row[2]);
                tc.setUrlEndpoint(row[3]);

                String actionLocator = row[4];
                if (actionLocator.contains("/")) {
                    String[] parts = actionLocator.split("/", 2);
                    tc.setHttpMethodAction(parts[0].trim());
                    tc.setLocatorType(parts[1].trim());
                } else {
                    tc.setHttpMethodAction(actionLocator);
                }

                tc.setLocatorType(row.length > 5 ? row[5] : "");
                tc.setLocatorValue(row.length > 6 ? row[6] : "");
                tc.setInputData(row.length > 7 ? row[7] : "");
                tc.setExpectedResult(row.length > 8 ? row[8] : "");
                tc.setPriority(row.length > 9 ? row[9] : "Medium");

                String runStr = row.length > 10 ? row[10] : "true";
                tc.setRun("YES".equalsIgnoreCase(runStr) || "Yes".equalsIgnoreCase(runStr)
                        || Boolean.parseBoolean(runStr));

                tc.setDescription(row.length > 11 ? row[11] : "");
                if (row.length > 12)
                    tc.setActionsJson(row[12]);

                tc.setTestSuite(suite); // Link to NEW suite
                cases.add(tc);
            }
        }

        // FIXED #1: Save all NEW test cases (even if identical to old ones)
        caseRepository.saveAll(cases);
        suite.setTestCases(cases);
        suiteRepository.save(suite);

        System.out.println("Created NEW suite ID " + suite.getId() + " with " + cases.size() + " test cases");
        return suite;
    }

    public List<TestSuite> getAllSuites() {
        return suiteRepository.findAll();
    }

    public TestSuite getSuiteById(Long id) {
        return suiteRepository.findById(id).orElse(null);
    }

    /**
     * FIXED #2: Update suite status based on actual test_result records
     */
    public void updateSuiteStatus(Long suiteId) {
        TestSuite suite = getSuiteById(suiteId);
        if (suite == null || suite.getTestRun() == null) {
            System.err.println("Cannot update status: suite or testRun is null for ID " + suiteId);
            return;
        }

        Long runId = suite.getTestRun().getId();
        List<TestResult> results = resultRepository.findByTestRunId(runId);

        if (results.isEmpty()) {
            System.err.println("No test results found for run ID " + runId + " (suite " + suiteId + ")");
            suite.setStatus(TestStatus.PENDING); // No results yet
        } else {
            long total = suite.getTestCases().stream().filter(tc -> Boolean.TRUE.equals(tc.getRun())).count();
            long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
            long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();

            if (failed > 0) {
                suite.setStatus(TestStatus.FAILED); // At least one failure
            } else if (passed == total) {
                suite.setStatus(TestStatus.PASSED); // All passed
            } else {
                suite.setStatus(TestStatus.COMPLETED); // Partial completion
            }

            System.out.println("Updated suite " + suiteId + " status: " + suite.getStatus() +
                    " (Passed: " + passed + "/" + total + ", Failed: " + failed + ")");
        }

        suiteRepository.save(suite);
    }
}