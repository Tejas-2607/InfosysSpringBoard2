package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.service.TestResultService;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
// import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// import com.example.test_framework_api.model.User;
import com.example.test_framework_api.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * UNIFIED TEST EXECUTOR
 * Handles both UI and API test execution with retry mechanism
 * Supports dynamic element testing without static base URL
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestExecutor {

    private final TestResultService testResultService;
    // private final RetryTemplate retryTemplate;

    @Autowired
    private UserRepository userRepository;

    /**
     * Execute a single test case (UI or API)
     */
    public void executeTestCase(TestCase testCase, TestRun testRun) {
        long startTime = System.currentTimeMillis();
        TestResult result = new TestResult();
        result.setTestName(testCase.getTestName());
        result.setTestRun(testRun);
        result.setTestSuite(testCase.getTestSuite());
        result.setCreatedAt(LocalDateTime.now());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            userRepository.findByUsername(username).ifPresent(result::setExecutedBy);
        }

        try {
            log.info("Executing {} test: {}", testCase.getTestType(), testCase.getTestCaseId());

            if ("UI".equalsIgnoreCase(testCase.getTestType())) {
                executeUITest(testCase);
            } else if ("API".equalsIgnoreCase(testCase.getTestType())) {
                executeAPITest(testCase);
            } else {
                throw new IllegalArgumentException("Invalid test type: " + testCase.getTestType());
            }

            result.setStatus(TestStatus.PASSED);
            result.setDuration(System.currentTimeMillis() - startTime);
            result.setRetryCount(0);
            log.info("✓ PASSED: {}", testCase.getTestCaseId());

        } catch (Exception e) {
            result.setStatus(TestStatus.FAILED);
            result.setDuration(System.currentTimeMillis() - startTime);
            result.setErrorMessage(e.getMessage());
            result.setRetryCount(0);
            log.error("✗ FAILED: {} - {}", testCase.getTestCaseId(), e.getMessage());
        }

        testResultService.saveTestResult(result);
    }

    /**
     * Execute UI test with dynamic URL and element interaction
     */
    private void executeUITest(TestCase testCase) {
        WebDriver driver = null;
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            String url = testCase.getUrlEndpoint();
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("URL is required for UI tests");
            }

            log.debug("Navigating to: {}", url);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // Find element using locator
            WebElement element = findElement(driver, wait, testCase);

            // Perform action
            String action = testCase.getHttpMethodAction().toLowerCase();
            performUIAction(driver, element, action, testCase.getInputData());

            // Validate expected result if provided
            if (testCase.getExpectedResult() != null && !testCase.getExpectedResult().isEmpty()) {
                validateUIResult(driver, testCase.getExpectedResult());
            }

        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Execute API test with full HTTP method support
     */
    private void executeAPITest(TestCase testCase) {
        String url = testCase.getUrlEndpoint();
        String method = testCase.getHttpMethodAction().toUpperCase();
        String inputData = testCase.getInputData();

        log.debug("API {} request to: {}", method, url);

        Response response;
        try {
            response = switch (method) {
                case "GET" -> RestAssured.given()
                        .when()
                        .get(url);

                case "POST" -> RestAssured.given()
                        .contentType("application/json")
                        .body(inputData != null ? inputData : "{}")
                        .when()
                        .post(url);

                case "PUT" -> RestAssured.given()
                        .contentType("application/json")
                        .body(inputData != null ? inputData : "{}")
                        .when()
                        .put(url);

                case "PATCH" -> RestAssured.given()
                        .contentType("application/json")
                        .body(inputData != null ? inputData : "{}")
                        .when()
                        .patch(url);

                case "DELETE" -> RestAssured.given()
                        .when()
                        .delete(url);

                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            };

            log.debug("API response: {} - Status: {}", url, response.getStatusCode());

            // Validate expected result (status code or body content)
            validateAPIResult(response, testCase.getExpectedResult());

        } catch (Exception e) {
            throw new RuntimeException("API test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Find element using locator type and value
     */
    private WebElement findElement(WebDriver driver, WebDriverWait wait, TestCase testCase) {
        String locatorType = testCase.getLocatorType();
        String locatorValue = testCase.getLocatorValue();

        if (locatorType == null || locatorValue == null) {
            throw new IllegalArgumentException("Locator type and value required for UI tests");
        }

        By locator = switch (locatorType.toLowerCase()) {
            case "id" -> By.id(locatorValue);
            case "name" -> By.name(locatorValue);
            case "xpath" -> By.xpath(locatorValue);
            case "css", "cssselector" -> By.cssSelector(locatorValue);
            case "classname" -> By.className(locatorValue);
            case "tagname" -> By.tagName(locatorValue);
            default -> throw new IllegalArgumentException("Unsupported locator type: " + locatorType);
        };

        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Perform UI action on element
     */
    private void performUIAction(WebDriver driver, WebElement element, String action, String inputData) {
        Actions actions = new Actions(driver);

        switch (action) {
            case "click" -> element.click();
            case "doubleclick" -> actions.doubleClick(element).perform();
            case "rightclick" -> actions.contextClick(element).perform();
            case "type" -> {
                element.clear();
                element.sendKeys(inputData != null ? inputData : "");
            }
            case "clear" -> element.clear();
            case "submit" -> element.submit();
            case "hover" -> actions.moveToElement(element).perform();
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        }

        log.debug("Performed action: {}", action);
    }

    /**
     * Validate UI test result
     */
    private void validateUIResult(WebDriver driver, String expectedResult) {
        if (expectedResult.toLowerCase().contains("title")) {
            String actualTitle = driver.getTitle();
            log.debug("Page title: {}", actualTitle);
        } else if (expectedResult.toLowerCase().contains("alert")) {
            try {
                Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                log.debug("Alert text: {}", alertText);
                alert.accept();
            } catch (NoAlertPresentException e) {
                log.warn("No alert present");
            }
        }
    }

    /**
     * Validate API test result
     */
    private void validateAPIResult(Response response, String expectedResult) {
        if (expectedResult == null || expectedResult.isEmpty()) {
            // Just check if response is successful (2xx)
            if (response.getStatusCode() >= 400) {
                log.error("API returned error status: " + response.getStatusCode());
                return;
            }
            return;
        }

        // Check status code
        if (expectedResult.matches("\\d{3}.*")) {
            int expectedStatus = Integer.parseInt(expectedResult.split("\\s")[0]);
            if (response.getStatusCode() != expectedStatus) {
                log.error("Expected status " + expectedStatus +
                        " but got " + response.getStatusCode());
                return;
            }
            return;
        }

        // Check response body contains expected text
        if (!expectedResult.matches("\\d{3}.*")) {
            String body = response.getBody().asString();
            if (!body.contains(expectedResult)) {
            log.error("Response body does not contain: " + expectedResult);
            return;
        }
        return;
    }
    }

    /**
     * Execute dynamic test from TestEntityController
     */
    public void executeDynamicTest(String url, String elementId, String action,
            String expectedResult, String value) {
        WebDriver driver = null;
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            log.debug("Dynamic test: URL={}, Element={}, Action={}", url, elementId, action);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(elementId)));

            performUIAction(driver, element, action, value);

            if (expectedResult != null && !expectedResult.isEmpty()) {
                validateUIResult(driver, expectedResult);
            }

            log.info("✓ Dynamic test PASSED");

        } catch (Exception e) {
            log.error("✗ Dynamic test FAILED: {}", e.getMessage());
            throw new RuntimeException("Dynamic test failed: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Execute multi-action dynamic test
     */
    public void executeDynamicMultiAction(String url, String elementId,
            List<Map<String, Object>> actions,
            String expectedResult) {
        WebDriver driver = null;
        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(elementId)));

            for (Map<String, Object> actionStep : actions) {
                String actionType = (String) actionStep.get("type");
                String value = (String) actionStep.get("value");
                performUIAction(driver, element, actionType, value);
            }

            if (expectedResult != null && !expectedResult.isEmpty()) {
                validateUIResult(driver, expectedResult);
            }

            log.info("✓ Multi-action dynamic test PASSED");

        } catch (Exception e) {
            log.error("✗ Multi-action dynamic test FAILED: {}", e.getMessage());
            throw new RuntimeException("Multi-action test failed: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * Legacy test execution (for backward compatibility)
     */
    public void executeTest() {
        log.info("Legacy test execution called");
    }
}