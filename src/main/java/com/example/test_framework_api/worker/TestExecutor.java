package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRunRequest;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TestExecutor {

    public void executeTest(TestRunRequest request) throws Exception {
        System.out.println("Executing test logic for TestRun ID: " + request.getTestId());
        long startTime = System.currentTimeMillis();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().window().maximize();

        try {
            driver.get("http://127.0.0.1:5500/testpage.html");
            System.out.println("Navigated to URL for ID: " + request.getTestId());
            System.out.println("Actual URL: " + driver.getCurrentUrl());
            System.out.println("Actual Title: " + driver.getTitle());

            String expectedTitle = "Test Page";
            String actualTitle = driver.getTitle();
            if (!actualTitle.contains(expectedTitle)) {
                throw new Exception("Title mismatch: expected '" + expectedTitle + "', got '" + actualTitle + "' for ID: " + request.getTestId());
            }
            System.out.println("Title validated for ID: " + request.getTestId());

            Thread.sleep(500); // Simulate action delay
            System.out.println("Performed test action for ID: " + request.getTestId());

            if (driver.getCurrentUrl().isEmpty()) {
                throw new Exception("URL validation failed for ID: " + request.getTestId());
            }
            System.out.println("URL validated for ID: " + request.getTestId());

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Test completed successfully for ID: " + request.getTestId() + " in " + duration + "ms");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Test execution failed for ID " + request.getTestId() + " after " + duration + "ms: " + e.getMessage());
            throw e;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Failed to clean up WebDriver for ID " + request.getTestId() + ": " + e.getMessage());
                }
            }
        }
    }
}