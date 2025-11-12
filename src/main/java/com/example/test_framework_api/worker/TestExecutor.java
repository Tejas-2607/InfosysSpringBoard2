// package com.example.test_framework_api.worker;

// import com.example.test_framework_api.model.TestRunRequest;
// import com.example.test_framework_api.pageobjects.TestPage;
// import io.github.bonigarcia.wdm.WebDriverManager;
// import io.qameta.allure.Step;
// import org.openqa.selenium.WebDriver;
// import org.openqa.selenium.chrome.ChromeDriver;
// import org.openqa.selenium.chrome.ChromeOptions;
// import org.springframework.stereotype.Component;
// import org.springframework.beans.factory.annotation.Value;
// import java.time.Duration;

// @Component
// public class TestExecutor {
//     @Value("${app.base-url:http://localhost:8080}")
//     private String baseUrl;

//     @Step("Execute test on page")
//     public void executeTest(TestRunRequest request) throws Exception {
//         System.out.println("Executing test logic for TestRun ID: " + request.getTestId());
//         long startTime = System.currentTimeMillis();

//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless");
//         options.addArguments("--disable-gpu");
//         WebDriver driver = new ChromeDriver(options);
//         driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
//         driver.manage().window().maximize();

//         TestPage page = new TestPage(driver); // POM

//         try {
//              page.open(baseUrl);
//             page.validateTitle();
//             page.performAction();

//             long duration = System.currentTimeMillis() - startTime;
//             System.out.println("Test completed successfully for ID: " + request.getTestId() + " in " + duration + "ms");
//         } catch (Exception e) {
//             long duration = System.currentTimeMillis() - startTime;
//             System.err.println("Test execution failed for ID " + request.getTestId() + " after " + duration + "ms: "
//                     + e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 try {
//                     driver.quit();
//                 } catch (Exception e) {
//                     System.err.println(
//                             "Failed to clean up WebDriver for ID " + request.getTestId() + ": " + e.getMessage());
//                 }
//             }
//         }
//     }
// }

// package com.example.test_framework_api.worker;

// import com.example.test_framework_api.model.TestCase;
// import com.example.test_framework_api.model.TestRun;
// import com.example.test_framework_api.model.TestResult;
// import com.example.test_framework_api.model.TestStatus;
// import com.example.test_framework_api.pageobjects.TestPage;
// import com.example.test_framework_api.service.TestResultService;
// import io.github.bonigarcia.wdm.WebDriverManager;
// import org.openqa.selenium.WebDriver;
// import org.openqa.selenium.chrome.ChromeDriver;
// import org.openqa.selenium.chrome.ChromeOptions;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;
// // import org.openqa.selenium.*;
// import org.openqa.selenium.WebElement;
// import org.openqa.selenium.support.ui.WebDriverWait;
// import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.interactions.Actions;
// import org.openqa.selenium.By;
// import org.openqa.selenium.InvalidElementStateException;
// import io.restassured.RestAssured;
// import lombok.RequiredArgsConstructor;
// import com.fasterxml.jackson.databind.JsonNode;
// // import com.fasterxml.jackson.databind.ObjectMapper;
// import static io.restassured.RestAssured.given;
// import io.restassured.http.ContentType;
// import io.restassured.response.Response;
// import java.time.Duration;
// import java.util.List;

// @Component
// @RequiredArgsConstructor
// public class TestExecutor {

//     private final TestResultService testResultService;

//     @Value("http://127.0.0.1:5500")
//     private String baseUrl;

//     public void executeTest() {
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
//         WebDriver driver = new ChromeDriver(options);

//         try {
//             TestPage page = new TestPage(driver);
//             page.open(baseUrl); // waits for title
//             page.validateTitle(); // now passes
//             page.performAction(); // button is found
//         } catch (Exception e) {
//             System.err.println("Test execution failed: " + e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 try {
//                     driver.quit();
//                 } catch (Exception quitEx) {
//                     System.err.println("Error quitting driver: " + quitEx.getMessage());
//                 }
//             }
//         }
//     }

//     public void executeDynamicTest(String url, String elementId, String action, String expectedResult) {
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless");
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//         Actions actions = new Actions(driver);

//         try {
//             driver.get(url);
//             WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(elementId)));
//             if (!element.isDisplayed() || !element.isEnabled()) {
//                 throw new IllegalStateException("Element '" + elementId
//                         + "' is not interactable (displayed/enabled). Ensure it's a button/input.");
//             }
//             switch (action.toLowerCase()) {
//                 case "click":
//                     element.click();
//                     break;
//                 case "doubleclick":
//                     actions.doubleClick(element).perform();
//                     break;
//                 case "rightclick":
//                 case "contextclick":
//                     // For rightclick/hover, ensure it's a valid target
//                     if ("rightclick".equals(action.toLowerCase()) || "hover".equals(action.toLowerCase())) {
//                         new Actions(driver).moveToElement(element).contextClick(element).perform(); // Use moveToElement
//                                                                                                     // first
//                     }
//                     break;
//                 case "clear":
//                     element.clear();
//                     break;
//                 case "submit":
//                     String tag = element.getTagName().toLowerCase();
//                     if ("form".equals(tag)) {
//                         // If targeting form, find and click inner submit button/input
//                         List<WebElement> submitElements = element
//                                 .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
//                         if (!submitElements.isEmpty()) {
//                             submitElements.get(0).click();
//                             System.out.println("Submitted form " + elementId + " via inner submit element");
//                         } else {
//                             throw new IllegalArgumentException("No submit button/input found inside form " + elementId);
//                         }
//                     } else {
//                         // Try submit on nested element (input/button)
//                         try {
//                             if ("button".equals(tag)) {
//                                 // Buttons submit better via click
//                                 element.click();
//                             } else {
//                                 element.submit();
//                             }
//                         } catch (InvalidElementStateException ignored) {
//                             // Fallback: Find ancestor form and submit via its inner submit element
//                             List<WebElement> ancestorForms = element.findElements(By.xpath("./ancestor::form"));
//                             if (!ancestorForms.isEmpty()) {
//                                 WebElement form = ancestorForms.get(0);
//                                 List<WebElement> submitElements = form
//                                         .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
//                                 if (!submitElements.isEmpty()) {
//                                     submitElements.get(0).click();
//                                     System.out.println("Submitted via ancestor form's submit element for " + elementId);
//                                 } else {
//                                     throw new IllegalArgumentException(
//                                             "No submit button/input found in ancestor form for " + elementId);
//                                 }
//                             } else {
//                                 throw new IllegalArgumentException("No form found for submit action on " + elementId
//                                         + ". Ensure it's nested in a <form> or target the form id directly.");
//                             }
//                         }
//                     }
//                     break;
//                 case "type":
//                     // FIXED: Added "type" case — use expectedResult as input text
//                     element.sendKeys(expectedResult != null ? expectedResult : "default text");
//                     break;
//                 default:
//                     throw new IllegalArgumentException("Unsupported action: " + action);
//             }

//             // Verify result if provided
//             if (expectedResult != null) {
//                 if (action.toLowerCase().equals("type") || action.toLowerCase().equals("clear")) {
//                     // For type/clear, check element value
//                     wait.until(ExpectedConditions.attributeToBe(element, "value", expectedResult));
//                 } else {
//                     // For others, check body text
//                     wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), expectedResult));
//                 }
//             }
//             System.out.println("Dynamic test PASSED: " + action + " on " + elementId + " at " + url);

//         } catch (Exception e) {
//             System.err.println("Dynamic test FAILED: " + e.getMessage());
//             throw e;
//         } finally {
//             driver.quit();
//         }
//     }

//     public void executeTestCase(TestCase tc, TestRun run) {
//         TestResult result = new TestResult();
//         result.setTestName(tc.getTestName());
//         result.setTestRun(run);
//         long start = System.currentTimeMillis();

//         try {
//             if ("UI".equals(tc.getTestType())) {
//                 // UI Execution with Selenium
//                 WebDriver driver = new ChromeDriver(); // Assume setup via WebDriverManager
//                 driver.get(tc.getUrlEndpoint());
//                 By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
//                 WebElement element = driver.findElement(locator);

//                 String action = tc.getHttpMethodAction();
//                 if ("click".equals(action)) {
//                     element.click();
//                 } else if ("type".equals(action) || "sendKeys".equals(action)) {
//                     element.sendKeys(tc.getInputData());
//                 } // Add more: clear, hover, etc.

//                 // Verify expected
//                 String actual = driver.getPageSource().contains(tc.getExpectedResult()) ? "Match" : "No Match";
//                 result.setStatus("Match".equals(actual) ? TestStatus.PASSED : TestStatus.FAILED);
//                 driver.quit();
//             } else if ("API".equals(tc.getTestType())) {
//                 // API Execution with RestAssured
//                 RestAssured.baseURI = tc.getUrlEndpoint();
//                 String method = tc.getHttpMethodAction().toUpperCase();
//                 if ("GET".equals(method)) {
//                     given().when().get().then().statusCode(Integer.parseInt(tc.getExpectedResult().split(" ")[0]));
//                 } else if ("POST".equals(method)) {
//                     given().body(tc.getInputData()).when().post().then()
//                             .statusCode(Integer.parseInt(tc.getExpectedResult().split(" ")[0]));
//                 }
//                 result.setStatus(TestStatus.PASSED); // Assume success if no exception
//             }
//         } catch (Exception e) {
//             result.setStatus(TestStatus.FAILED);
//             result.setErrorMessage(e.getMessage()); // NEW FEATURE: Capture errors
//         }

//         result.setDuration(System.currentTimeMillis() - start);
//         testResultService.saveTestResult(result); // Save result
//     }

//     private By getLocator(String type, String value) { // NEW FEATURE: Dynamic locator
//         return switch (type) {
//             case "id" -> By.id(value);
//             case "name" -> By.name(value);
//             case "xpath" -> By.xpath(value);
//             default -> By.id(value);
//         };
//     }
// }

// @Component
// @RequiredArgsConstructor
// public class TestExecutor {

//     private final TestResultService testResultService;

//     @Value("${app.base-url:http://127.0.0.1:5500}")
//     private String baseUrl;

//     /**
//      * Executes a static test (e.g., fixed page interactions).
//      * This is for legacy/single-run tests; not used in suite execution.
//      */
//     public void executeTest() {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
//         WebDriver driver = new ChromeDriver(options);

//         try {
//             TestPage page = new TestPage(driver);
//             page.open(baseUrl); // waits for title
//             page.validateTitle(); // now passes
//             page.performAction(); // button is found
//         } catch (Exception e) {
//             System.err.println("Test execution failed: " + e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 try {
//                     driver.quit();
//                 } catch (Exception quitEx) {
//                     System.err.println("Error quitting driver: " + quitEx.getMessage());
//                 }
//             }
//         }
//     }

//     public void executeDynamicTest(String url, String elementId, String action, String expectedResult) {
//         executeDynamicTest(url, elementId, action, expectedResult, ""); // FIXED: Default inputValue
//     }

//     /**
//      * Executes dynamic UI test for a single action (used in multi-action loops or
//      * standalone).
//      * Supports click, type, submit, etc., with waits and fallbacks.
//      * FIXED: Removed undefined 'value'—use expectedResult; added optional
//      * inputValue param.
//      */
//     public void executeDynamicTest(String url, String elementId, String action, String expectedResult,
//             String inputValue) { // FIXED: Added inputValue param
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless");
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//         Actions actions = new Actions(driver);

//         try {
//             driver.get(url);
//             By locator = By.id(elementId); // FIXED: Simple ID; extend with getLocator if needed
//             WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
//             if (!element.isDisplayed() || !element.isEnabled()) {
//                 throw new IllegalStateException("Element '" + elementId
//                         + "' is not interactable (displayed/enabled). Ensure it's a button/input.");
//             }
//             switch (action.toLowerCase()) {
//                 case "click":
//                     element.click();
//                     break;
//                 case "doubleclick":
//                     actions.doubleClick(element).perform();
//                     break;
//                 case "rightclick":
//                 case "contextclick":
//                     if ("rightclick".equals(action.toLowerCase()) || "hover".equals(action.toLowerCase())) {
//                         new Actions(driver).moveToElement(element).contextClick(element).perform();
//                     }
//                     break;
//                 case "clear":
//                     element.clear();
//                     break;
//                 case "submit":
//                     String tag = element.getTagName().toLowerCase();
//                     if ("form".equals(tag)) {
//                         List<WebElement> submitElements = element
//                                 .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
//                         if (!submitElements.isEmpty()) {
//                             submitElements.get(0).click();
//                             System.out.println("Submitted form " + elementId + " via inner submit element");
//                         } else {
//                             throw new IllegalArgumentException("No submit button/input found inside form " + elementId);
//                         }
//                     } else {
//                         try {
//                             if ("button".equals(tag)) {
//                                 element.click();
//                             } else {
//                                 element.submit();
//                             }
//                         } catch (InvalidElementStateException ignored) {
//                             List<WebElement> ancestorForms = element.findElements(By.xpath("./ancestor::form"));
//                             if (!ancestorForms.isEmpty()) {
//                                 WebElement form = ancestorForms.get(0);
//                                 List<WebElement> submitElements = form
//                                         .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
//                                 if (!submitElements.isEmpty()) {
//                                     submitElements.get(0).click();
//                                     System.out.println("Submitted via ancestor form's submit element for " + elementId);
//                                 } else {
//                                     throw new IllegalArgumentException(
//                                             "No submit button/input found in ancestor form for " + elementId);
//                                 }
//                             } else {
//                                 throw new IllegalArgumentException("No form found for submit action on " + elementId
//                                         + ". Ensure it's nested in a <form> or target the form id directly.");
//                             }
//                         }
//                     }
//                     break;
//                 case "type":
//                 case "sendkeys":
//                     // FIXED: Use expectedResult or inputValue; no 'value' var
//                     String textToSend = (expectedResult != null ? expectedResult
//                             : (inputValue != null ? inputValue : "default text"));
//                     element.sendKeys(textToSend);
//                     break;
//                 default:
//                     throw new IllegalArgumentException("Unsupported action: " + action);
//             }

//             // Verify result if provided
//             if (expectedResult != null) {
//                 if (action.toLowerCase().equals("type") || action.toLowerCase().equals("clear")) {
//                     wait.until(ExpectedConditions.attributeToBe(element, "value", expectedResult));
//                 } else {
//                     wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), expectedResult));
//                 }
//             }
//             System.out.println("Dynamic test PASSED: " + action + " on " + elementId + " at " + url);

//         } catch (Exception e) {
//             System.err.println("Dynamic test FAILED: " + e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 driver.quit();
//             }
//         }
//     }

//     /**
//      * Core method for executing a TestCase from suite (single or multi-action).
//      * Merges: Basic UI/API from first snippet; dynamic switch from second;
//      * multi-action loop from third.
//      * Calls executeDynamicTest for UI actions (with inputValue); RestAssured for
//      * API.
//      */
//     public void executeTestCase(TestCase tc, TestRun run) {
//         TestResult result = new TestResult();
//         result.setTestName(tc.getTestName());
//         result.setTestRun(run);
//         long start = System.currentTimeMillis();

//         try {
//             JsonNode actions = tc.getActions(); // Parse multi-actions (third snippet)
//             boolean isMulti = actions != null && actions.isArray() && actions.size() > 0;

//             if ("UI".equals(tc.getTestType())) {
//                 WebDriverManager.chromedriver().setup();
//                 WebDriver driver = new ChromeDriver();
//                 try {
//                     driver.get(tc.getUrlEndpoint());

//                     if (isMulti) {
//                         // Multi-Action: Loop (third snippet)
//                         for (JsonNode actionNode : actions) {
//                             String type = actionNode.get("type").asText();
//                             String value = actionNode.has("value") ? actionNode.get("value").asText()
//                                     : tc.getInputData();
//                             executeDynamicTest(tc.getUrlEndpoint(), tc.getLocatorValue(), type, tc.getExpectedResult(),
//                                     value); // FIXED: Pass inputValue
//                         }
//                     } else {
//                         // Single action (first snippet)
//                         By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
//                         WebElement element = driver.findElement(locator);
//                         String action = tc.getHttpMethodAction();
//                         if ("click".equals(action)) {
//                             element.click();
//                         } else if ("type".equals(action) || "sendKeys".equals(action)) {
//                             element.sendKeys(tc.getInputData());
//                         } else {
//                             // Fallback to dynamic (second snippet)
//                             executeDynamicTest(tc.getUrlEndpoint(), tc.getLocatorValue(), action,
//                                     tc.getExpectedResult(), tc.getInputData());
//                         }
//                     }

//                     // Verify (first/second/third merge: pageSource + element text)
//                     String pageText = driver.getPageSource();
//                     String bodyText = driver.findElement(By.tagName("body")).getText();
//                     String actual = (pageText.contains(tc.getExpectedResult())
//                             || bodyText.contains(tc.getExpectedResult())) ? "Match" : "No Match";
//                     result.setStatus("Match".equals(actual) ? TestStatus.PASSED : TestStatus.FAILED);
//                 } finally {
//                     driver.quit();
//                 }
//             } else if ("API".equals(tc.getTestType())) {
//                 RestAssured.baseURI = tc.getUrlEndpoint();
//                 String method = tc.getHttpMethodAction().toUpperCase();
//                 String body = tc.getInputData().trim();
//                 String expectedStr = tc.getExpectedResult().split(" ")[0];
//                 int expectedCode = Integer.parseInt(expectedStr);
//                 if (isMulti) {
//                     for (JsonNode actionNode : actions) {
//                         String apiMethod = actionNode.get("type").asText().toUpperCase();
//                         String apiBody = actionNode.has("value") ? actionNode.get("value").asText() : body;
//                         executeApiCall(apiMethod, apiBody, expectedCode);
//                     }
//                 } else {
//                     // Single (first snippet)
//                     if ("GET".equals(method)) {
//                         given().when().get().then().statusCode(expectedCode);
//                     } else if ("POST".equals(method)) {
//                         Response response = given()
//                                 .contentType(ContentType.JSON)
//                                 .body(body.isEmpty() ? "{}" : body)
//                                 .when().post();
//                         response.then().statusCode(expectedCode);
//                     }
//                 }
//                 result.setStatus(TestStatus.PASSED); // Assume success
//             }
//         } catch (AssertionError ae) {
//             // FIXED: Catch assertion (e.g., 401 != 200) — set FAILED, log
//             System.err.println("API assertion failed for " + tc.getTestName() + ": " + ae.getMessage());
//             result.setStatus(TestStatus.FAILED);
//             result.setErrorMessage("AssertionError: " + ae.getMessage() + " (URL: " + tc.getUrlEndpoint() + ")");
//         } catch (Exception e) {
//             String errorMsg = e.getMessage();
//             result.setStatus(TestStatus.FAILED);
//             if (errorMsg != null && errorMsg.length() > 2000) { // FIXED: Optional truncate
//                 errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
//             }
//             result.setErrorMessage(errorMsg);
//             result.setStatus(TestStatus.FAILED);
//         }

//         result.setDuration(System.currentTimeMillis() - start);
//         testResultService.saveTestResult(result);
//     }

//     /**
//      * Helper for single API call (used in multi).
//      * FIXED: Added expectedResult param for status check.
//      */
//     private void executeApiCall(String method, String body, int expectedCode) {
//         if ("GET".equals(method)) {
//             given().when().get().then().statusCode(expectedCode);
//         } else if ("POST".equals(method)) {
//             given()
//                     .contentType(ContentType.JSON) // FIXED: JSON header
//                     .body(body.isEmpty() ? "{}" : body)
//                     .when().post()
//                     .then().statusCode(expectedCode);
//         } else {
//             throw new IllegalArgumentException("Unsupported API method: " + method);
//         }
//     }

//     /**
//      * Dynamic locator factory (first snippet).
//      */
//     private By getLocator(String type, String value) {
//         if (type == null || value == null)
//             return By.id("default");
//         return switch (type.toLowerCase()) {
//             case "id" -> By.id(value);
//             case "name" -> By.name(value);
//             case "xpath" -> By.xpath(value);
//             default -> By.id(value);
//         };
//     }
// }

package com.example.test_framework_api.worker;

import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.pageobjects.TestPage;
import com.example.test_framework_api.service.TestResultService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import io.restassured.RestAssured;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestExecutor {

    private final TestResultService testResultService;

    @Value("${app.base-url:http://127.0.0.1:5500}")
    private String baseUrl;

    public void executeTest() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);

        try {
            TestPage page = new TestPage(driver);
            page.open(baseUrl);
            page.validateTitle();
            page.performAction();
        } catch (Exception e) {
            System.err.println("Test execution failed: " + e.getMessage());
            throw e;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception quitEx) {
                    System.err.println("Error quitting driver: " + quitEx.getMessage());
                }
            }
        }
    }

    public void executeDynamicTest(String url, String elementId, String action, String expectedResult) {
        executeDynamicTest(url, elementId, action, expectedResult, "");
    }

    public void executeDynamicTest(String url, String elementId, String action, String expectedResult,
            String inputValue) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Actions actions = new Actions(driver);

        try {
            driver.get(url);
            By locator = By.id(elementId);
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator)); // FIXED: Presence
                                                                                                   // wait
            element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            if (!element.isDisplayed() || !element.isEnabled()) {
                throw new IllegalStateException("Element '" + elementId
                        + "' is not interactable (displayed/enabled). Ensure it's a button/input.");
            }
            switch (action.toLowerCase()) {
                case "click":
                    element.click();
                    break;
                case "doubleclick":
                    actions.doubleClick(element).perform();
                    break;
                case "rightclick":
                case "contextclick":
                    if ("rightclick".equals(action.toLowerCase()) || "hover".equals(action.toLowerCase())) {
                        new Actions(driver).moveToElement(element).contextClick(element).perform();
                    }
                    break;
                case "clear":
                    element.clear();
                    break;
                case "submit":
                    String tag = element.getTagName().toLowerCase();
                    if ("form".equals(tag)) {
                        List<WebElement> submitElements = element
                                .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
                        if (!submitElements.isEmpty()) {
                            submitElements.get(0).click();
                            System.out.println("Submitted form " + elementId + " via inner submit element");
                        } else {
                            throw new IllegalArgumentException("No submit button/input found inside form " + elementId);
                        }
                    } else {
                        try {
                            if ("button".equals(tag)) {
                                element.click();
                            } else {
                                element.submit();
                            }
                        } catch (InvalidElementStateException ignored) {
                            List<WebElement> ancestorForms = element.findElements(By.xpath("./ancestor::form"));
                            if (!ancestorForms.isEmpty()) {
                                WebElement form = ancestorForms.get(0);
                                List<WebElement> submitElements = form
                                        .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
                                if (!submitElements.isEmpty()) {
                                    submitElements.get(0).click();
                                    System.out.println("Submitted via ancestor form's submit element for " + elementId);
                                } else {
                                    throw new IllegalArgumentException(
                                            "No submit button/input found in ancestor form for " + elementId);
                                }
                            } else {
                                throw new IllegalArgumentException("No form found for submit action on " + elementId
                                        + ". Ensure it's nested in a <form> or target the form id directly.");
                            }
                        }
                    }
                    break;
                case "type":
                case "sendkeys":
                    String textToSend = (expectedResult != null ? expectedResult
                            : (inputValue != null ? inputValue : "default text"));
                    element.clear();
                    element.sendKeys(textToSend);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported action: " + action);
            }

            if (expectedResult != null) {
                if (action.toLowerCase().equals("type") || action.toLowerCase().equals("clear")) {
                    wait.until(ExpectedConditions.attributeToBe(element, "value", expectedResult));
                } else {
                    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), expectedResult));
                }
            }
            System.out.println("Dynamic test PASSED: " + action + " on " + elementId + " at " + url);

        } catch (Exception e) {
            System.err.println("Dynamic test FAILED: " + e.getMessage());
            throw e;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * FIXED #2 & #3: Core test case execution with proper result saving and error
     * handling
     */
    public void executeTestCase(TestCase tc, TestRun run) {
        TestResult result = new TestResult();
        result.setTestName(tc.getTestName());
        result.setTestRun(run);
        long start = System.currentTimeMillis();

        try {
            JsonNode actions = tc.getActions();
            boolean isMulti = actions != null && actions.isArray() && actions.size() > 0;

            if ("UI".equals(tc.getTestType())) {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
                WebDriver driver = new ChromeDriver(options);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(4)); // FIXED: Longer wait for load
                By locator = null;
                try {
                    driver.get(tc.getUrlEndpoint());

                    if (isMulti) {
                        for (JsonNode actionNode : actions) {
                            String type = actionNode.get("type").asText();
                            String value = actionNode.has("value") ? actionNode.get("value").asText()
                                    : tc.getInputData();
                            executeDynamicTest(tc.getUrlEndpoint(), tc.getLocatorValue(), type, tc.getExpectedResult(),
                                    value);
                        }
                    } else {
                        locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
                        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator)); // FIXED:
                                                                                                                 // Visibility
                                                                                                                 // wait
                        String action = tc.getHttpMethodAction();
                        if ("click".equals(action)) {
                            element.click();
                        } else if ("type".equals(action) || "sendKeys".equals(action)) {
                            element.clear();
                            element.sendKeys(tc.getInputData());
                        } else {
                            executeDynamicTest(tc.getUrlEndpoint(), tc.getLocatorValue(), action,
                                    tc.getExpectedResult(), tc.getInputData());
                        }
                    }

                    // FIXED: Enhanced verification
                    String expected = tc.getExpectedResult();
                    boolean match = false;
                    if (expected.contains("typed") || expected.contains("Password typed")) {
                        WebElement checkElement = driver.findElement(locator);
                        String actualValue = checkElement.getDomAttribute("value"); // FIXED: getDomAttribute
                        match = actualValue != null && actualValue.contains(tc.getInputData());
                        log.debug("UI Verify: Element value '{}' contains input '{}' ? {}", actualValue,
                                tc.getInputData(), match);
                    } else if (expected.contains("Search results") || expected.contains("Redirected")) {
                        match = driver.getTitle().contains(expected.split(" ")[0])
                                || driver.getCurrentUrl().contains(expected.toLowerCase());
                        log.debug("UI Verify: Title/URL '{}' matches expected '{}' ? {}", driver.getTitle(), expected,
                                match);
                    } else {
                        match = driver.findElement(By.tagName("body")).getText().contains(expected);
                        log.debug("UI Verify: Body text contains '{}' ? {}", expected, match);
                    }
                    result.setStatus(match ? TestStatus.PASSED : TestStatus.FAILED);
                    log.info("UI Test {}: {} (duration: {}ms)", tc.getTestName(), result.getStatus(),
                            System.currentTimeMillis() - start);
                } finally {
                    driver.quit();
                }

            } else if ("API".equals(tc.getTestType())) {
                RestAssured.baseURI = tc.getUrlEndpoint();
                String method = tc.getHttpMethodAction().toUpperCase();
                String body = tc.getInputData() != null ? tc.getInputData().trim() : "";
                String expectedStr = tc.getExpectedResult().split(" ")[0];
                int expectedCode = Integer.parseInt(expectedStr);

                if (isMulti) {
                    for (JsonNode actionNode : actions) {
                        String apiMethod = actionNode.get("type").asText().toUpperCase();
                        String apiBody = actionNode.has("value") ? actionNode.get("value").asText() : body;
                        executeApiCall(apiMethod, apiBody, expectedCode);
                    }
                } else {
                    if ("GET".equals(method)) {
                        given().when().get().then().statusCode(expectedCode);
                    } else if ("POST".equals(method)) {
                        Response response = given()
                                .contentType(ContentType.JSON)
                                .body(body.isEmpty() ? "{}" : body)
                                .when().post();
                        response.then().statusCode(expectedCode);
                    }
                }
                result.setStatus(TestStatus.PASSED);
            }

        } catch (AssertionError ae) {
            // FIXED #3: Don't let assertion errors crash the system
            System.err.println("API assertion failed for " + tc.getTestName() + ": " + ae.getMessage());
            result.setStatus(TestStatus.FAILED);
            result.setErrorMessage("AssertionError: " + ae.getMessage() + " (URL: " + tc.getUrlEndpoint() + ")");
        } catch (Exception e) {
            // FIXED #3: Catch all exceptions, mark as failed, continue
            String errorMsg = e.getMessage();
            result.setStatus(TestStatus.FAILED);
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
            }
            result.setErrorMessage(errorMsg);
            System.err.println("Test case execution failed for " + tc.getTestName() + ": " + errorMsg);
        }

        result.setDuration(System.currentTimeMillis() - start);

        // FIXED #2: Always save result to database
        testResultService.saveTestResult(result);
        log.info("Test Result Saved: {} - Status: {} (duration: {}ms)", tc.getTestName(), result.getStatus(), result.getDuration());
    }

    private void executeApiCall(String method, String body, int expectedCode) {
        if ("GET".equals(method)) {
            given().when().get().then().statusCode(expectedCode);
        } else if ("POST".equals(method)) {
            given()
                    .contentType(ContentType.JSON)
                    .body(body.isEmpty() ? "{}" : body)
                    .when().post()
                    .then().statusCode(expectedCode);
        } else {
            throw new IllegalArgumentException("Unsupported API method: " + method);
        }
    }

    private By getLocator(String type, String value) {
        if (type == null || value == null)
            return By.id("default");
        return switch (type.toLowerCase()) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "xpath" -> By.xpath(value);
            default -> By.id(value);
        };
    }
}
