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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestSuiteService {

    private final TestSuiteRepository suiteRepository;
    private final TestCaseRepository caseRepository;
    private final TestResultRepository resultRepository;

    // NEW FEATURE: Import CSV to create TestSuite with TestCases
    public TestSuite importFromCsv(MultipartFile file, String suiteName, String description)
            throws IOException, CsvValidationException {
        if (file.isEmpty())
            throw new IllegalArgumentException("CSV file is empty");
        TestSuite suite = new TestSuite();
        suite.setName(suiteName);
        suite.setDescription(description);
        suite = suiteRepository.save(suite);
        // if (row.length != 12) {
        // throw new IllegalArgumentException(
        // "Row " + (cases.size() + 1) + " has " + row.length + " columns; expected
        // 12.");
        // }
        List<TestCase> cases = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] headers = reader.readNext(); // Skip header
            if (headers == null || headers.length < 12)
                throw new IllegalArgumentException("Invalid header: Expected 12+ columns");
            String[] row;
            int rowNum = 1;
            while ((row = reader.readNext()) != null) {
                rowNum++;
                if (row.length < 12)
                    throw new IllegalArgumentException(
                            "Row " + rowNum + " has " + row.length + " columns; expected 12+"); // FIXED: Validation
                TestCase tc = new TestCase();
                tc.setTestCaseId(row[0]); // TestCaseID
                tc.setTestName(row[1]); // TestName
                tc.setTestType(row[2]); // TestType
                tc.setUrlEndpoint(row[3]); // URL/Endpoint
                // FIXED: Handle combined "click/id" - split if contains "/"
                String actionLocator = row[4]; // HTTP Method / Action
                if (actionLocator.contains("/")) {
                    String[] parts = actionLocator.split("/", 2);
                    tc.setHttpMethodAction(parts[0].trim()); // e.g., "click"
                    tc.setLocatorType(parts[1].trim()); // e.g., "id"
                } else {
                    tc.setHttpMethodAction(actionLocator);
                }
                tc.setLocatorType(row.length > 5 ? row[5] : ""); // LocatorType
                tc.setLocatorValue(row.length > 6 ? row[6] : ""); // LocatorValue
                tc.setInputData(row.length > 7 ? row[7] : ""); // InputData
                tc.setExpectedResult(row.length > 8 ? row[8] : ""); // ExpectedResult
                tc.setPriority(row.length > 9 ? row[9] : ""); // Priority
                // FIXED: Flexible Run parsing for "Yes"/"YES"/"true"
                String runStr = row.length > 10 ? row[10] : "false";
                tc.setRun("YES".equalsIgnoreCase(runStr) || "Yes".equalsIgnoreCase(runStr)
                        || Boolean.parseBoolean(runStr));
                tc.setDescription(row.length > 11 ? row[11] : ""); // Description
                // NEW FEATURE: Multi-Action - If extra column (13+), set actionsJson
                if (row.length > 12)
                    tc.setActionsJson(row[12]);
                tc.setTestSuite(suite); // Link to suite
                cases.add(tc);
            }
        }
        caseRepository.saveAll(cases);
        suite.setTestCases(cases);
        suiteRepository.save(suite);
        return suite; // Update with bidirectional link
    }

    public List<TestSuite> getAllSuites() {
        return suiteRepository.findAll();
    }

    public TestSuite getSuiteById(Long id) {
        return suiteRepository.findById(id).orElse(null);
    }

    public void updateSuiteStatus(Long suiteId) {
        TestSuite suite = getSuiteById(suiteId);
        if (suite != null && suite.getTestRun() != null) {
            Long runId = suite.getTestRun().getId();
            List<TestResult> results = resultRepository.findByTestRunId(runId);  // FIXED: Now defined, uses custom method
            long total = suite.getTestCases().stream().filter(tc -> Boolean.TRUE.equals(tc.getRun())).count();
            long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
            suite.setStatus(passed == total ? TestStatus.PASSED : (passed > 0 ? TestStatus.COMPLETED : TestStatus.FAILED));
            suiteRepository.save(suite);
        }
    }
}