package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestRunRequest;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TestExecutor {

    private WebDriver driver;

    public TestExecutor() {
        // Configure ChromeDriver with options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode
        options.addArguments("--disable-gpu"); // Disable GPU for stability
        System.setProperty("webdriver.chrome.driver", "path/to/chromedriver"); // Update with actual path
        this.driver = new ChromeDriver(options);
        this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        this.driver.manage().window().maximize();
    }

    public void executeTest(TestRunRequest request) throws Exception {
        System.out.println("Executing test logic for TestRun ID: " + request.getTestId());
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Navigate to the test URL
            driver.get("http://example.com"); // Replace with actual test URL
            System.out.println("Navigated to URL for ID: " + request.getTestId());

            // Step 2: Validate page title
            String expectedTitle = "Example Domain";
            String actualTitle = driver.getTitle();
            if (!actualTitle.contains(expectedTitle)) {
                throw new Exception("Title mismatch: expected '" + expectedTitle + "', got '" + actualTitle
                        + "' for ID: " + request.getTestId());
            }
            System.out.println("Title validated for ID: " + request.getTestId());

            // Step 3: Simulate additional test action (e.g., click or input)
            Thread.sleep(500); // Simulate action delay
            System.out.println("Performed test action for ID: " + request.getTestId());

            // Step 4: Final validation
            if (driver.getCurrentUrl().isEmpty()) {
                throw new Exception("URL validation failed for ID: " + request.getTestId());
            }
            System.out.println("URL validated for ID: " + request.getTestId());

            // Success case
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Test completed successfully for ID: " + request.getTestId() + " in " + duration + "ms");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Test execution failed for ID " + request.getTestId() + " after " + duration + "ms: "
                    + e.getMessage());
            throw e; // Propagate to WorkerListener for status update
        } finally {
            // Cleanup WebDriver (modern approach)
            if (driver != null) {
                try {
                    driver.quit(); // Close and terminate the WebDriver session
                } catch (Exception e) {
                    System.err.println(
                            "Failed to clean up WebDriver for ID " + request.getTestId() + ": " + e.getMessage());
                }
            }
        }
    }

    // Optional: Add a method to reinitialize driver if needed (part of original
    // complexity)
    public void reinitializeDriver() {
        if (driver != null) {
            driver.quit();
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu");
        System.setProperty("webdriver.chrome.driver",
                "C:\\Users\\HP\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        this.driver = new ChromeDriver(options);
        this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        System.out.println("Reinitialized WebDriver for new test session");
    }
}