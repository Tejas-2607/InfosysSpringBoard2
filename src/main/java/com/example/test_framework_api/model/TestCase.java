package com.example.test_framework_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "test_case")
@Data
public class TestCase {
    @Id
    private String testCaseId;  // CSV: TestCaseID (e.g., TC_UI01)

    @Column(nullable = false)
    private String testName;

    @Column(nullable = false)
    private String testType;  // UI or API

    @Column(name = "url_endpoint")
    private String urlEndpoint;

    @Column(name = "http_method_action")
    private String httpMethodAction;  // e.g., GET, POST, click, type

    @Column(name = "locator_type")
    private String locatorType;  // id, name, xpath (for UI)

    @Column(name = "locator_value")
    private String locatorValue;  // e.g., username

    @Column(name = "input_data")
    private String inputData;  // e.g., test_user or JSON

    @Column(name = "expected_result")
    private String expectedResult;  // e.g., 200 OK, Login successful

    private String priority;  // High, Medium, Low

    private Boolean run = true;  // CSV: Run (default true)

    private String description;

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    private TestSuite testSuite;  // NEW FEATURE: Belongs to suite
}