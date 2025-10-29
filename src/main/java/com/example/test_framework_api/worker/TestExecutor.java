package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.pageobjects.TestPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TestExecutor {

    @Step("Execute test on page")
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

        TestPage page = new TestPage(driver); // POM

        try {
            page.navigateToTestPage();
            page.validateTitle();
            page.performAction();
            page.validateUrl();

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