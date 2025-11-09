package com.example.test_framework_api.service;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestSuite;
import com.example.test_framework_api.repository.TestCaseRepository;
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

    // NEW FEATURE: Import CSV to create TestSuite with TestCases
    public TestSuite importFromCsv(MultipartFile file, String suiteName, String description)
            throws IOException, CsvValidationException {
        TestSuite suite = new TestSuite();
        suite.setName(suiteName);
        suite.setDescription(description);
        suite = suiteRepository.save(suite);
        // if (row.length != 12) {
        //     throw new IllegalArgumentException(
        //             "Row " + (cases.size() + 1) + " has " + row.length + " columns; expected 12.");
        // }
        List<TestCase> cases = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            reader.readNext(); // Skip header row
            String[] row;
            while ((row = reader.readNext()) != null) {
                TestCase tc = new TestCase();
                tc.setTestCaseId(row[0]); // TestCaseID
                tc.setTestName(row[1]); // TestName
                tc.setTestType(row[2]); // TestType
                tc.setUrlEndpoint(row[3]); // URL/Endpoint
                tc.setHttpMethodAction(row[4]); // HTTP Method/Action
                tc.setLocatorType(row[5]); // LocatorType
                tc.setLocatorValue(row[6]);// LocatorValue
                tc.setInputData(row[7]); // InputData
                tc.setExpectedResult(row[8]); // ExpectedResult
                tc.setPriority(row[9]); // Priority
                tc.setRun(Boolean.parseBoolean(row[10])); // Run
                tc.setDescription(row[11]);// Description
                tc.setTestSuite(suite); // Link to suite
                cases.add(tc);
            }
        }
        caseRepository.saveAll(cases);
        suite.setTestCases(cases);
        return suiteRepository.save(suite); // Update with bidirectional link
    }

    public List<TestSuite> getAllSuites() {
        return suiteRepository.findAll();
    }

    public TestSuite getSuiteById(Long id) {
        return suiteRepository.findById(id).orElse(null);
    }
}