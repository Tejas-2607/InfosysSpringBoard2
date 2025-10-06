package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRunRequest;
import com.example.test_framework_api.service.TestResultService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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

        // WebDriver must be managed properly. Using a `finally` block is crucial.
        WebDriver driver = null;
        long startTime = System.currentTimeMillis();

        try {
            // 1. Example Unit Test (Pyramid base)
            Assertions.assertEquals(2, 1 + 1, "Simple unit assertion failed.");

            // 2. Example Integration/API Test (Middle layer, REST-Assured)
            RestAssured.get("https://jsonplaceholder.typicode.com/todos/1")
                .then()
                .statusCode(200);

            // 3. Example UI Test (Top layer, Selenium)
            // Using headless mode is a best practice for automated, non-visual execution.
            // This prevents a browser window from physically opening on the server.
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run browser in the background
            options.addArguments("--no-sandbox"); // Required for running in some environments (like Docker)
            options.addArguments("--disable-dev-shm-usage"); // Overcomes limited resource problems
            options.addArguments("--window-size=1920,1080"); // Set a consistent window size
            
            // NOTE: For this to work, the `chromedriver` executable must be on the system's PATH,
            // or you should use a library like WebDriverManager to handle the driver setup.
            driver = new ChromeDriver(options);

            driver.get("https://www.google.com");
            driver.findElement(By.name("q")).sendKeys("Selenium test");
            
            // It's good practice to complete an action, like submitting the form.
            driver.findElement(By.name("q")).submit();

            // A simple wait to see the result page title
            Thread.sleep(1000); 
            
            Assertions.assertTrue(driver.getTitle().contains("Selenium test"), "Page title did not contain the search term.");

            result.setStatus("PASS");

        } catch (Throwable e) {
            // Catching 'Throwable' is important because assertion failures are Errors, not Exceptions.
            // This ensures both test failures and code errors are caught.
            result.setStatus("FAIL");
            result.setErrorMessage(e.getMessage());
            // Optionally, you could add stack trace information for better debugging:
            // StringWriter sw = new StringWriter();
            // e.printStackTrace(new PrintWriter(sw));
            // result.setErrorMessage(sw.toString());

        } finally {
            // The `finally` block ensures that the browser instance is always closed,
            // even if the test fails. This prevents memory leaks from lingering browser processes.
            if (driver != null) {
                driver.quit();
            }
            long endTime = System.currentTimeMillis();
            // Calculate the actual duration of the test run for accurate metrics.
            result.setDuration(endTime - startTime);
        }

        resultService.saveTestResult(result);
        System.out.println("Execution finished for ID: " + request.getTestId() + " with status: " + result.getStatus());
    }
}

