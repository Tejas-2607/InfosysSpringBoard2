package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.service.TestResultService;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.RestAssured;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import jakarta.annotation.PreDestroy;

@Component
public class TestExecutor {

    @Autowired
    private TestResultService resultService;

    private static WebDriver driver; 

    public void executeTest(TestRunRequest request) {
        System.out.println("Executing test for ID: " + request.getTestId());
        TestResult result = new TestResult();
        result.setTestName(request.getSuiteName());
        result.setCreatedAt(LocalDateTime.now());

        long startTime = System.currentTimeMillis();
        try {
            
            if (driver == null) {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless"); 
                driver = new ChromeDriver(options);
            }

            

            
            RestAssured.get("https://jsonplaceholder.typicode.com/todos/1").then().statusCode(200);

            
            driver.get("https://www.google.com");
            driver.findElement(By.name("q")).sendKeys("Selenium test");

            result.setStatus("PASSED");
        } catch (Exception e) {
            result.setStatus("FAIL");
            result.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            result.setDuration(System.currentTimeMillis() - startTime);
            resultService.saveTestResult(result);
            
        }
    }

    
    @PreDestroy
    public void closeDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
}