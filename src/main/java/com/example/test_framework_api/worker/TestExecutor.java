package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.service.TestResultService;
import io.restassured.RestAssured;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TestExecutor {

    @Autowired
    private TestResultService resultService;

    public void executeTest(TestRunRequest request) {
        System.out.println("Executing test for ID: " + request.getTestId());
        TestResult result = new TestResult();
        result.setTestName(request.getSuiteName());
        result.setCreatedAt(LocalDateTime.now());

        long startTime = System.currentTimeMillis();
        try {
            

            // Example Integration/API Test (REST-Assured)
            RestAssured.get("https://jsonplaceholder.typicode.com/todos/1").then().statusCode(200);

            // Example UI Test (Selenium)
            WebDriver driver = new ChromeDriver();
            try {
                driver.get("https://www.google.com");
                driver.findElement(By.name("q")).sendKeys("Selenium test");
            } finally {
                driver.quit();
            }

            result.setStatus("PASS");
        } catch (Exception e) {
            result.setStatus("FAIL");
            result.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            result.setDuration(System.currentTimeMillis() - startTime);
            resultService.saveTestResult(result);
        }
    }
}