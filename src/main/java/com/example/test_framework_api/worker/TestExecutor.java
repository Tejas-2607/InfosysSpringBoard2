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
// import lombok.extern.slf4j.Slf4j;
// import org.openqa.selenium.WebElement;
// import org.openqa.selenium.support.ui.WebDriverWait;
// import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.interactions.Actions;
// import org.openqa.selenium.By;
// import org.openqa.selenium.InvalidElementStateException;
// import org.openqa.selenium.JavascriptExecutor;
// import io.restassured.RestAssured;
// import lombok.RequiredArgsConstructor;
// import com.fasterxml.jackson.databind.JsonNode;
// import static io.restassured.RestAssured.given;
// import io.restassured.http.ContentType;
// import io.restassured.response.Response;
// import io.restassured.specification.RequestSpecification;
// import java.time.Duration;
// import java.util.List;

// /**
//  * FIXED: Enhanced TestExecutor with better handling for:
//  * - Google bot detection (realistic browser profile)
//  * - API authentication and rate limiting
//  * - Robust element waits and fallback strategies
//  */
// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class TestExecutor {

//     private final TestResultService testResultService;

//     @Value("${app.base-url:http://127.0.0.1:5500}")
//     private String baseUrl;

//     public void executeTest() {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);

//         try {
//             TestPage page = new TestPage(driver);
//             page.open(baseUrl);
//             page.validateTitle();
//             page.performAction();
//         } catch (Exception e) {
//             log.error("Test execution failed: {}", e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 driver.quit();
//             }
//         }
//     }

//     /**
//      * FIXED: Realistic Chrome options to bypass bot detection
//      */
//     private ChromeOptions createRealisticChromeOptions() {
//         ChromeOptions options = new ChromeOptions();

//         // CRITICAL: Use new headless mode and disable bot detection flags
//         options.addArguments("--headless=new");  // New headless mode (less detectable)
//         options.addArguments("--disable-blink-features=AutomationControlled");  // Hide automation
//         options.addArguments("--disable-dev-shm-usage");
//         options.addArguments("--no-sandbox");
//         options.addArguments("--disable-gpu");
//         options.addArguments("--window-size=1920,1080");
//         options.addArguments("--start-maximized");

//         // FIXED: Realistic user agent (not headless Chrome signature)
//         options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");

//         // Additional stealth options
//         options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
//         options.setExperimentalOption("useAutomationExtension", false);

//         return options;
//     }

//     public void executeDynamicTest(String url, String elementId, String action, String expectedResult) {
//         executeDynamicTest(url, elementId, action, expectedResult, "");
//     }

//     public void executeDynamicTest(String url, String elementId, String action, String expectedResult,
//             String inputValue) {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
//         Actions actions = new Actions(driver);

//         try {
//             driver.get(url);

//             // FIXED: Add stealth JS injection for Google
//             if (url.contains("google.com")) {
//                 injectStealthScripts(driver);
//             }

//             By locator = By.id(elementId);
//             WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
//             element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

//             if (!element.isDisplayed() || !element.isEnabled()) {
//                 throw new IllegalStateException("Element '" + elementId
//                         + "' is not interactable (displayed/enabled).");
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
//                     new Actions(driver).moveToElement(element).contextClick(element).perform();
//                     break;
//                 case "clear":
//                     element.clear();
//                     break;
//                 case "submit":
//                     handleSubmitAction(driver, element, elementId);
//                     break;
//                 case "type":
//                 case "sendkeys":
//                     String textToSend = (expectedResult != null ? expectedResult
//                             : (inputValue != null ? inputValue : "default text"));
//                     element.clear();
//                     element.sendKeys(textToSend);
//                     break;
//                 default:
//                     throw new IllegalArgumentException("Unsupported action: " + action);
//             }

//             if (expectedResult != null) {
//                 if (action.toLowerCase().equals("type") || action.toLowerCase().equals("clear")) {
//                     wait.until(ExpectedConditions.attributeToBe(element, "value", expectedResult));
//                 } else {
//                     wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), expectedResult));
//                 }
//             }
//             log.info("Dynamic test PASSED: {} on {} at {}", action, elementId, url);

//         } catch (Exception e) {
//             log.error("Dynamic test FAILED: {}", e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 driver.quit();
//             }
//         }
//     }

//     /**
//      * FIXED: Inject scripts to hide automation detection
//      */
//     private void injectStealthScripts(WebDriver driver) {
//         JavascriptExecutor js = (JavascriptExecutor) driver;

//         // Hide webdriver property
//         js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

//         // Override Chrome object
//         js.executeScript("window.chrome = {runtime: {}};");

//         // Override permissions
//         js.executeScript(
//             "Object.defineProperty(navigator, 'permissions', {get: () => ({query: () => Promise.resolve({state: 'granted'})})});"
//         );

//         log.debug("Stealth scripts injected for bot detection bypass");
//     }

//     /**
//      * FIXED: Enhanced submit handler with multiple fallback strategies
//      */
//     private void handleSubmitAction(WebDriver driver, WebElement element, String elementId) {
//         String tag = element.getTagName().toLowerCase();

//         if ("form".equals(tag)) {
//             // Strategy 1: Find submit button
//             List<WebElement> submitElements = element
//                     .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
//             if (!submitElements.isEmpty()) {
//                 submitElements.get(0).click();
//                 log.debug("Submitted form {} via inner submit element", elementId);
//                 return;
//             }
//             throw new IllegalArgumentException("No submit button found in form " + elementId);
//         }

//         // Strategy 2: Direct submit for buttons
//         try {
//             if ("button".equals(tag)) {
//                 element.click();
//                 return;
//             }
//             element.submit();
//             return;
//         } catch (InvalidElementStateException ignored) {
//             // Continue to Strategy 3
//         }

//         // Strategy 3: Find ancestor form
//         List<WebElement> ancestorForms = element.findElements(By.xpath("./ancestor::form"));
//         if (!ancestorForms.isEmpty()) {
//             WebElement form = ancestorForms.get(0);
//             List<WebElement> submitElements = form
//                     .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
//             if (!submitElements.isEmpty()) {
//                 submitElements.get(0).click();
//                 log.debug("Submitted via ancestor form for {}", elementId);
//                 return;
//             }
//         }

//         throw new IllegalArgumentException("No form found for submit action on " + elementId);
//     }

//     /**
//      * FIXED: Enhanced test case execution with better API handling
//      */
//     public void executeTestCase(TestCase tc, TestRun run) {
//         TestResult result = new TestResult();
//         result.setTestName(tc.getTestName());
//         result.setTestRun(run);
//         long start = System.currentTimeMillis();

//         try {
//             JsonNode actions = tc.getActions();
//             boolean isMulti = actions != null && actions.isArray() && actions.size() > 0;

//             if ("UI".equals(tc.getTestType())) {
//                 executeUITest(tc, run, result, start, isMulti, actions);
//             } else if ("API".equals(tc.getTestType())) {
//                 executeAPITest(tc, result, isMulti, actions);
//             }

//         } catch (AssertionError ae) {
//             log.error("Assertion failed for {}: {}", tc.getTestName(), ae.getMessage());
//             result.setStatus(TestStatus.FAILED);
//             result.setErrorMessage("AssertionError: " + ae.getMessage());
//         } catch (Exception e) {
//             log.error("Test case execution failed for {}: {}", tc.getTestName(), e.getMessage());
//             String errorMsg = e.getMessage();
//             result.setStatus(TestStatus.FAILED);
//             if (errorMsg != null && errorMsg.length() > 2000) {
//                 errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
//             }
//             result.setErrorMessage(errorMsg);
//         }

//         result.setDuration(System.currentTimeMillis() - start);
//         testResultService.saveTestResult(result);
//         log.info("Test Result Saved: {} - Status: {} (duration: {}ms)", 
//             tc.getTestName(), result.getStatus(), result.getDuration());
//     }

//     /**
//      * FIXED: Separate UI test execution with Google-specific handling
//      */
//     private void executeUITest(TestCase tc, TestRun run, TestResult result, long start, 
//             boolean isMulti, JsonNode actions) {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));  // Longer wait for Google

//         try {
//             driver.get(tc.getUrlEndpoint());

//             // FIXED: Special handling for Google
//             if (tc.getUrlEndpoint().contains("google.com")) {
//                 injectStealthScripts(driver);
//                 Thread.sleep(2000);  // Wait for page load and consent forms

//                 // Handle cookie consent if present
//                 try {
//                     WebElement acceptCookies = driver.findElement(
//                         By.xpath("//button[contains(., 'Accept') or contains(., 'I agree')]")
//                     );
//                     acceptCookies.click();
//                     Thread.sleep(1000);
//                 } catch (Exception ignored) {
//                     log.debug("No cookie consent found or already accepted");
//                 }
//             }

//             if (isMulti) {
//                 for (JsonNode actionNode : actions) {
//                     String type = actionNode.get("type").asText();
//                     String value = actionNode.has("value") ? actionNode.get("value").asText()
//                             : tc.getInputData();
//                     executeDynamicTest(tc.getUrlEndpoint(), tc.getLocatorValue(), type, 
//                         tc.getExpectedResult(), value);
//                 }
//             } else {
//                 By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());

//                 // FIXED: For Google, use name locator with wait
//                 if (tc.getUrlEndpoint().contains("google.com") && "name".equals(tc.getLocatorType())) {
//                     wait = new WebDriverWait(driver, Duration.ofSeconds(15));
//                 }

//                 WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
//                 String action = tc.getHttpMethodAction();

//                 if ("click".equals(action)) {
//                     // FIXED: Scroll into view before clicking (helps with Google)
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
//                     Thread.sleep(500);
//                     element.click();
//                 } else if ("type".equals(action) || "sendKeys".equals(action)) {
//                     element.clear();
//                     element.sendKeys(tc.getInputData());
//                     Thread.sleep(500);  // Allow input to register
//                 } else {
//                     executeDynamicTest(tc.getUrlEndpoint(), tc.getLocatorValue(), action,
//                             tc.getExpectedResult(), tc.getInputData());
//                 }
//             }

//             // FIXED: Enhanced verification
//             String expected = tc.getExpectedResult();
//             boolean match = false;

//             if (expected.contains("typed") || expected.contains("Password typed")) {
//                 By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
//                 WebElement checkElement = driver.findElement(locator);
//                 String actualValue = checkElement.getDomAttribute("value");
//                 match = actualValue != null && actualValue.contains(tc.getInputData());
//                 log.debug("UI Verify: Element value '{}' contains input '{}' ? {}", 
//                     actualValue, tc.getInputData(), match);
//             } else if (expected.contains("Search results") || expected.contains("Redirected")) {
//                 // Wait for navigation
//                 Thread.sleep(2000);
//                 match = driver.getTitle().contains(expected.split(" ")[0])
//                         || driver.getCurrentUrl().contains(expected.toLowerCase());
//                 log.debug("UI Verify: Title/URL '{}' matches expected '{}' ? {}", 
//                     driver.getTitle(), expected, match);
//             } else {
//                 match = driver.findElement(By.tagName("body")).getText().contains(expected);
//                 log.debug("UI Verify: Body text contains '{}' ? {}", expected, match);
//             }

//             result.setStatus(match ? TestStatus.PASSED : TestStatus.FAILED);
//             log.info("UI Test {}: {} (duration: {}ms)", tc.getTestName(), result.getStatus(),
//                     System.currentTimeMillis() - start);
//         } catch (Exception e) {
//             log.error("UI test failed: {}", e.getMessage());
//             throw new RuntimeException(e);
//         } finally {
//             driver.quit();
//         }
//     }

//     /**
//      * FIXED: Enhanced API test execution with authentication support
//      */
//     private void executeAPITest(TestCase tc, TestResult result, boolean isMulti, JsonNode actions) {
//         RestAssured.baseURI = tc.getUrlEndpoint();
//         String method = tc.getHttpMethodAction().toUpperCase();
//         String body = tc.getInputData() != null ? tc.getInputData().trim() : "";
//         String expectedStr = tc.getExpectedResult().split(" ")[0];
//         int expectedCode = Integer.parseInt(expectedStr);

//         try {
//             if (isMulti) {
//                 for (JsonNode actionNode : actions) {
//                     String apiMethod = actionNode.get("type").asText().toUpperCase();
//                     String apiBody = actionNode.has("value") ? actionNode.get("value").asText() : body;
//                     executeApiCall(apiMethod, apiBody, expectedCode, tc.getUrlEndpoint());
//                 }
//             } else {
//                 executeApiCall(method, body, expectedCode, tc.getUrlEndpoint());
//             }
//             result.setStatus(TestStatus.PASSED);
//         } catch (AssertionError ae) {
//             // FIXED: Handle 401/404 gracefully for demo APIs
//             if (ae.getMessage().contains("401") || ae.getMessage().contains("404")) {
//                 log.warn("API {} returned {}, marking as expected for demo API", 
//                     tc.getUrlEndpoint(), ae.getMessage().contains("401") ? "401" : "404");
//                 result.setStatus(TestStatus.FAILED);
//                 result.setErrorMessage("API authentication/endpoint issue: " + ae.getMessage());
//             } else {
//                 throw ae;
//             }
//         }
//     }

//     /**
//      * FIXED: API call with retry logic and authentication handling
//      */
//     private void executeApiCall(String method, String body, int expectedCode, String url) {
//         RequestSpecification request = given()
//                 .contentType(ContentType.JSON)
//                 .accept(ContentType.JSON);

//         // FIXED: Add authentication for APIs that need it
//         if (url.contains("reqres.in")) {
//             // ReqRes doesn't actually need auth for GET, but handles 401 gracefully
//             log.debug("ReqRes API call (may return 401 for rate limiting): {}", url);
//         }

//         Response response;
//         if ("GET".equals(method)) {
//             response = request.when().get();
//         } else if ("POST".equals(method)) {
//             response = request.body(body.isEmpty() ? "{}" : body).when().post();
//         } else {
//             throw new IllegalArgumentException("Unsupported API method: " + method);
//         }

//         // FIXED: Log response for debugging
//         int actualCode = response.getStatusCode();
//         log.debug("API Response: {} (expected: {})", actualCode, expectedCode);

//         // Assert status code
//         response.then().statusCode(expectedCode);
//     }

//     private By getLocator(String type, String value) {
//         if (type == null || value == null)
//             return By.id("default");
//         return switch (type.toLowerCase()) {
//             case "id" -> By.id(value);
//             case "name" -> By.name(value);
//             case "xpath" -> By.xpath(value);
//             case "css" -> By.cssSelector(value);
//             default -> By.id(value);
//         };
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
// import lombok.extern.slf4j.Slf4j;
// import org.openqa.selenium.WebElement;
// import org.openqa.selenium.support.ui.WebDriverWait;
// import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.By;
// import org.openqa.selenium.JavascriptExecutor;
// import io.restassured.RestAssured;
// import lombok.RequiredArgsConstructor;
// import com.fasterxml.jackson.databind.JsonNode;
// import static io.restassured.RestAssured.given;
// import io.restassured.http.ContentType;
// import io.restassured.response.Response;
// import java.util.List;
// import java.time.Duration;

// /**
//  * FIXED ISSUE #3: Enhanced element waits, POST handling, and submit button
//  * clicks
//  */
// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class TestExecutor {

//     private final TestResultService testResultService;

//     @Value("${app.base-url:http://127.0.0.1:5500}")
//     private String baseUrl;

//     public void executeTest() {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);

//         try {
//             TestPage page = new TestPage(driver);
//             page.open(baseUrl);
//             page.validateTitle();
//             page.performAction();
//         } catch (Exception e) {
//             log.error("Test execution failed: {}", e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 driver.quit();
//             }
//         }
//     }

//     private ChromeOptions createRealisticChromeOptions() {
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless=new");
//         options.addArguments("--disable-blink-features=AutomationControlled");
//         options.addArguments("--disable-dev-shm-usage");
//         options.addArguments("--no-sandbox");
//         options.addArguments("--disable-gpu");
//         options.addArguments("--window-size=1920,1080");
//         options.addArguments("--start-maximized");
//         options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
//         options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
//         options.setExperimentalOption("useAutomationExtension", false);
//         return options;
//     }

//     /**
//      * FIXED ISSUE #3: Enhanced test case execution with better element handling
//      */
//     public void executeTestCase(TestCase tc, TestRun run) {
//         TestResult result = new TestResult();
//         result.setTestName(tc.getTestName());
//         result.setTestRun(run);
//         long start = System.currentTimeMillis();

//         try {
//             JsonNode actions = tc.getActions();
//             boolean isMulti = actions != null && actions.isArray() && actions.size() > 0;

//             if ("UI".equals(tc.getTestType())) {
//                 executeUITest(tc, run, result, start, isMulti, actions);
//             } else if ("API".equals(tc.getTestType())) {
//                 executeAPITest(tc, result, isMulti, actions);
//             }

//         } catch (AssertionError ae) {
//             log.error("Assertion failed for {}: {}", tc.getTestName(), ae.getMessage());
//             result.setStatus(TestStatus.FAILED);
//             result.setErrorMessage("AssertionError: " + ae.getMessage());
//         } catch (Exception e) {
//             log.error("Test case execution failed for {}: {}", tc.getTestName(), e.getMessage());
//             result.setStatus(TestStatus.FAILED);
//             String errorMsg = e.getMessage();
//             if (errorMsg != null && errorMsg.length() > 2000) {
//                 errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
//             }
//             result.setErrorMessage(errorMsg);
//         }

//         result.setDuration(System.currentTimeMillis() - start);
//         testResultService.saveTestResult(result);
//         log.info("Test Result Saved: {} - Status: {} (duration: {}ms)",
//                 tc.getTestName(), result.getStatus(), result.getDuration());
//     }

//     /**
//      * FIXED ISSUE #3: Enhanced UI test with better element waits and submit
//      * handling
//      */
//     private void executeUITest(TestCase tc, TestRun run, TestResult result, long start,
//             boolean isMulti, JsonNode actions) {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15)); // FIXED: Longer wait

//         try {
//             driver.get(tc.getUrlEndpoint());

//             // Wait for page load
//             Thread.sleep(2000);

//             // Handle special sites (Google, etc.)
//             if (tc.getUrlEndpoint().contains("google.com")) {
//                 injectStealthScripts(driver);
//                 handleCookieConsent(driver);
//             }

//             By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
//             String action = tc.getHttpMethodAction().toLowerCase();

//             // FIXED ISSUE #3: Enhanced element wait strategy
//             WebElement element = waitForElement(driver, wait, locator);

//             if (isMulti) {
//                 // Execute multiple actions
//                 for (JsonNode actionNode : actions) {
//                     String type = actionNode.get("type").asText();
//                     String value = actionNode.has("value") ? actionNode.get("value").asText() : tc.getInputData();
//                     performAction(driver, wait, element, locator, type, value, tc.getExpectedResult());
//                 }
//             } else {
//                 // Single action
//                 performAction(driver, wait, element, locator, action, tc.getInputData(), tc.getExpectedResult());
//             }

//             // FIXED ISSUE #3: Enhanced verification
//             boolean match = verifyResult(driver, wait, tc, locator);
//             result.setStatus(match ? TestStatus.PASSED : TestStatus.FAILED);

//             log.info("UI Test {}: {} (duration: {}ms)", tc.getTestName(), result.getStatus(),
//                     System.currentTimeMillis() - start);
//         } catch (Exception e) {
//             log.error("UI test failed: {}", e.getMessage());
//             throw new RuntimeException(e);
//         } finally {
//             driver.quit();
//         }
//     }

//     /**
//      * FIXED ISSUE #3: Better element wait with multiple strategies
//      */
//     private WebElement waitForElement(WebDriver driver, WebDriverWait wait, By locator) {
//         try {
//             // Strategy 1: Wait for presence
//             wait.until(ExpectedConditions.presenceOfElementLocated(locator));

//             // Strategy 2: Wait for visibility
//             WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

//             // Strategy 3: Scroll into view
//             ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
//             Thread.sleep(500);

//             // Strategy 4: Wait for clickability
//             element = wait.until(ExpectedConditions.elementToBeClickable(locator));

//             return element;
//         } catch (Exception e) {
//             log.error("Element wait failed for locator: {}", locator);
//             throw e;
//         }
//     }

//     /**
//      * FIXED ISSUE #3: Enhanced action performer with submit button handling
//      */
//     private void performAction(WebDriver driver, WebDriverWait wait, WebElement element, By locator,
//             String action, String inputData, String expectedResult) throws Exception {

//         switch (action.toLowerCase()) {
//             case "click":
//                 // FIXED: Try click with fallback strategies
//                 try {
//                     element.click();
//                 } catch (Exception e) {
//                     log.warn("Regular click failed, trying JS click");
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
//                 }
//                 break;

//             case "submit":
//                 // FIXED ISSUE #3: Enhanced submit handling
//                 handleSubmitAction(driver, wait, element, locator);
//                 break;

//             case "type":
//             case "sendkeys":
//                 element.clear();
//                 Thread.sleep(300);
//                 element.sendKeys(inputData != null ? inputData : expectedResult);
//                 Thread.sleep(500);
//                 break;

//             case "clear":
//                 element.clear();
//                 break;

//             case "doubleclick":
//                 new org.openqa.selenium.interactions.Actions(driver).doubleClick(element).perform();
//                 break;

//             case "rightclick":
//             case "contextclick":
//                 new org.openqa.selenium.interactions.Actions(driver).contextClick(element).perform();
//                 break;

//             default:
//                 throw new IllegalArgumentException("Unsupported action: " + action);
//         }
//     }

//     /**
//      * FIXED ISSUE #3: Robust submit button handler with multiple strategies
//      */
//     private void handleSubmitAction(WebDriver driver, WebDriverWait wait, WebElement element, By locator)
//             throws Exception {
//         String tag = element.getTagName().toLowerCase();

//         // Strategy 1: If it's a submit button, just click it
//         if ("button".equals(tag) || "input".equals(tag)) {
//             String type = element.getAttribute("type");
//             if ("submit".equals(type)) {
//                 try {
//                     element.click();
//                     log.debug("Submit button clicked directly");
//                     return;
//                 } catch (Exception e) {
//                     log.warn("Direct click failed, trying JS click");
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
//                     return;
//                 }
//             }
//         }

//         // Strategy 2: Find submit button in same form
//         try {
//             List<WebElement> forms = element.findElements(By.xpath("./ancestor::form"));
//             if (!forms.isEmpty()) {
//                 WebElement form = forms.get(0);
//                 List<WebElement> submitButtons = form.findElements(
//                         By.cssSelector("button[type='submit'], input[type='submit'], button:not([type])"));
//                 if (!submitButtons.isEmpty()) {
//                     WebElement submitBtn = submitButtons.get(0);
//                     wait.until(ExpectedConditions.elementToBeClickable(submitBtn));

//                     // Scroll into view
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
//                     Thread.sleep(500);

//                     try {
//                         submitBtn.click();
//                         log.debug("Found and clicked submit button in ancestor form");
//                         return;
//                     } catch (Exception e) {
//                         ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
//                         log.debug("Clicked submit button via JS");
//                         return;
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             log.warn("Strategy 2 (ancestor form) failed: {}", e.getMessage());
//         }

//         // Strategy 3: Look for any submit button on page
//         try {
//             List<WebElement> submitButtons = driver.findElements(
//                     By.cssSelector("button[type='submit'], input[type='submit']"));
//             if (!submitButtons.isEmpty()) {
//                 WebElement submitBtn = submitButtons.get(0);
//                 wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
//                 ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
//                 Thread.sleep(500);
//                 submitBtn.click();
//                 log.debug("Found and clicked first submit button on page");
//                 return;
//             }
//         } catch (Exception e) {
//             log.warn("Strategy 3 (page submit button) failed: {}", e.getMessage());
//         }

//         // Strategy 4: Press Enter on the element
//         try {
//             element.sendKeys(org.openqa.selenium.Keys.RETURN);
//             log.debug("Pressed RETURN key on element");
//             return;
//         } catch (Exception e) {
//             log.warn("Strategy 4 (RETURN key) failed: {}", e.getMessage());
//         }

//         throw new IllegalStateException("All submit strategies failed - no submit button found");
//     }

//     /**
//      * FIXED ISSUE #3: Enhanced result verification
//      */
//     private boolean verifyResult(WebDriver driver, WebDriverWait wait, TestCase tc, By locator) throws Exception {
//         String expected = tc.getExpectedResult();

//         if (expected == null || expected.isEmpty()) {
//             return true; // No verification needed
//         }

//         // Wait for potential page changes
//         Thread.sleep(1000);

//         if (expected.contains("typed") || expected.contains("Password typed")) {
//             // Verify input value
//             WebElement checkElement = driver.findElement(locator);
//             String actualValue = checkElement.getDomAttribute("value");
//             boolean match = actualValue != null && actualValue.contains(tc.getInputData());
//             log.debug("Verify: Element value '{}' contains input '{}' ? {}", actualValue, tc.getInputData(), match);
//             return match;
//         } else if (expected.contains("Search results") || expected.contains("Redirected")) {
//             // Verify navigation
//             wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(tc.getUrlEndpoint())));
//             boolean match = driver.getTitle().contains(expected.split(" ")[0])
//                     || driver.getCurrentUrl().toLowerCase().contains(expected.toLowerCase());
//             log.debug("Verify: Title/URL '{}' matches expected '{}' ? {}", driver.getTitle(), expected, match);
//             return match;
//         } else {
//             // Verify page text
//             String bodyText = driver.findElement(By.tagName("body")).getText();
//             boolean match = bodyText.contains(expected);
//             log.debug("Verify: Body text contains '{}' ? {}", expected, match);
//             return match;
//         }
//     }

//     /**
//      * FIXED ISSUE #3: Enhanced API test with proper POST handling
//      */
//     private void executeAPITest(TestCase tc, TestResult result, boolean isMulti, JsonNode actions) {
//         RestAssured.baseURI = tc.getUrlEndpoint();
//         String method = tc.getHttpMethodAction().toUpperCase();
//         String body = tc.getInputData() != null ? tc.getInputData().trim() : "";
//         String expectedStr = tc.getExpectedResult().split(" ")[0];
//         int expectedCode = Integer.parseInt(expectedStr);

//         try {
//             if (isMulti) {
//                 for (JsonNode actionNode : actions) {
//                     String apiMethod = actionNode.get("type").asText().toUpperCase();
//                     String apiBody = actionNode.has("value") ? actionNode.get("value").asText() : body;
//                     executeApiCall(apiMethod, apiBody, expectedCode, tc.getUrlEndpoint());
//                 }
//             } else {
//                 executeApiCall(method, body, expectedCode, tc.getUrlEndpoint());
//             }
//             result.setStatus(TestStatus.PASSED);
//         } catch (AssertionError ae) {
//             if (ae.getMessage().contains("401") || ae.getMessage().contains("404")) {
//                 log.warn("API {} returned {}, marking as failed", tc.getUrlEndpoint(),
//                         ae.getMessage().contains("401") ? "401" : "404");
//                 result.setStatus(TestStatus.FAILED);
//                 result.setErrorMessage("API issue: " + ae.getMessage());
//             } else {
//                 throw ae;
//             }
//         }
//     }

//     /**
//      * FIXED ISSUE #3: Enhanced API call with proper POST body handling
//      */
//     private void executeApiCall(String method, String body, int expectedCode, String url) {
//         log.debug("API Call: {} {} (expected: {})", method, url, expectedCode);

//         Response response;
//         if ("GET".equals(method)) {
//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .when()
//                     .get();
//         } else if ("POST".equals(method)) {
//             // FIXED ISSUE #3: Proper POST with body
//             String requestBody = body.isEmpty() ? "{}" : body;
//             log.debug("POST body: {}", requestBody);

//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .body(requestBody)
//                     .when()
//                     .post();
//         } else if ("PUT".equals(method)) {
//             String requestBody = body.isEmpty() ? "{}" : body;
//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .body(requestBody)
//                     .when()
//                     .put();
//         } else if ("DELETE".equals(method)) {
//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .when()
//                     .delete();
//         } else {
//             throw new IllegalArgumentException("Unsupported API method: " + method);
//         }

//         int actualCode = response.getStatusCode();
//         log.debug("API Response: {} (expected: {})", actualCode, expectedCode);

//         // Log response body for debugging
//         if (actualCode != expectedCode) {
//             log.warn("Status mismatch - Response body: {}", response.getBody().asString());
//         }

//         response.then().statusCode(expectedCode);
//     }

//     private void injectStealthScripts(WebDriver driver) {
//         JavascriptExecutor js = (JavascriptExecutor) driver;
//         js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
//         js.executeScript("window.chrome = {runtime: {}};");
//         log.debug("Stealth scripts injected");
//     }

//     private void handleCookieConsent(WebDriver driver) {
//         try {
//             Thread.sleep(1000);
//             WebElement acceptCookies = driver.findElement(
//                     By.xpath("//button[contains(., 'Accept') or contains(., 'I agree')]"));
//             acceptCookies.click();
//             Thread.sleep(1000);
//             log.debug("Cookie consent accepted");
//         } catch (Exception ignored) {
//             log.debug("No cookie consent found");
//         }
//     }

//     private By getLocator(String type, String value) {
//         if (type == null || value == null)
//             return By.id("default");
//         return switch (type.toLowerCase()) {
//             case "id" -> By.id(value);
//             case "name" -> By.name(value);
//             case "xpath" -> By.xpath(value);
//             case "css" -> By.cssSelector(value);
//             default -> By.id(value);
//         };
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
// import lombok.extern.slf4j.Slf4j;
// import org.openqa.selenium.WebElement;
// import org.openqa.selenium.support.ui.WebDriverWait;
// import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.By;
// import org.openqa.selenium.JavascriptExecutor;
// import io.restassured.RestAssured;
// import lombok.RequiredArgsConstructor;
// import com.fasterxml.jackson.databind.JsonNode;
// import static io.restassured.RestAssured.given;
// import io.restassured.http.ContentType;
// import io.restassured.response.Response;
// import java.util.List;
// import java.time.Duration;

// /**
//  * FIXED ISSUES:
//  * 1. Added missing executeDynamicTest method
//  * 2. Fixed InterruptedException handling in waitForElement
//  * 3. Enhanced element waits, POST handling, and submit button clicks
//  */
// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class TestExecutor {

//     private final TestResultService testResultService;

//     @Value("${app.base-url:http://127.0.0.1:5500}")
//     private String baseUrl;

//     public void executeTest() {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);

//         try {
//             TestPage page = new TestPage(driver);
//             page.open(baseUrl);
//             page.validateTitle();
//             page.performAction();
//         } catch (Exception e) {
//             log.error("Test execution failed: {}", e.getMessage());
//             throw e;
//         } finally {
//             if (driver != null) {
//                 driver.quit();
//             }
//         }
//     }

//     /**
//      * ADDED: Dynamic test execution method called from WorkerListener
//      */
//     public void executeDynamicTest(String url, String elementId, String action,
//             String expectedResult, String inputValue) {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

//         try {
//             log.info("Executing dynamic test: URL={}, Element={}, Action={}", url, elementId, action);

//             driver.get(url);
//             Thread.sleep(2000); // Wait for page load

//             // Handle special sites
//             if (url.contains("google.com")) {
//                 injectStealthScripts(driver);
//                 handleCookieConsent(driver);
//             }

//             // Locate element
//             By locator = By.id(elementId);
//             WebElement element = waitForElement(driver, wait, locator);

//             // Perform action
//             performAction(driver, wait, element, locator, action, inputValue, expectedResult);

//             // Verify result
//             Thread.sleep(1000);
//             if (expectedResult != null && !expectedResult.isEmpty()) {
//                 boolean verified = verifyDynamicResult(driver, expectedResult, elementId);
//                 if (!verified) {
//                     throw new AssertionError("Expected result not met: " + expectedResult);
//                 }
//             }

//             log.info("Dynamic test PASSED: {} on {}", action, elementId);

//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             log.error("Dynamic test interrupted: {}", e.getMessage());
//             throw new RuntimeException("Test interrupted", e);
//         } catch (Exception e) {
//             log.error("Dynamic test FAILED: {}", e.getMessage());
//             throw new RuntimeException("Dynamic test execution failed", e);
//         } finally {
//             driver.quit();
//         }
//     }

//     /**
//      * ADDED: Helper method to verify dynamic test results
//      */
//     private boolean verifyDynamicResult(WebDriver driver, String expectedResult, String elementId) {
//         try {
//             if (expectedResult.contains("typed") || expectedResult.contains("Password typed")) {
//                 WebElement element = driver.findElement(By.id(elementId));
//                 String actualValue = element.getDomAttribute("value");
//                 return actualValue != null && !actualValue.isEmpty();
//             } else if (expectedResult.contains("Redirected") || expectedResult.contains("results")) {
//                 return !driver.getCurrentUrl().equals(driver.getCurrentUrl());
//             } else {
//                 String bodyText = driver.findElement(By.tagName("body")).getText();
//                 return bodyText.contains(expectedResult);
//             }
//         } catch (Exception e) {
//             log.warn("Result verification failed: {}", e.getMessage());
//             return false;
//         }
//     }

//     private ChromeOptions createRealisticChromeOptions() {
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless=new");
//         options.addArguments("--disable-blink-features=AutomationControlled");
//         options.addArguments("--disable-dev-shm-usage");
//         options.addArguments("--no-sandbox");
//         options.addArguments("--disable-gpu");
//         options.addArguments("--window-size=1920,1080");
//         options.addArguments("--start-maximized");
//         options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
//         options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
//         options.setExperimentalOption("useAutomationExtension", false);
//         return options;
//     }

//     /**
//      * Enhanced test case execution with better element handling
//      */
//     public void executeTestCase(TestCase tc, TestRun run) {
//         TestResult result = new TestResult();
//         result.setTestName(tc.getTestName());
//         result.setTestRun(run);
//         long start = System.currentTimeMillis();

//         try {
//             JsonNode actions = tc.getActions();
//             boolean isMulti = actions != null && actions.isArray() && actions.size() > 0;

//             if ("UI".equals(tc.getTestType())) {
//                 executeUITest(tc, run, result, start, isMulti, actions);
//             } else if ("API".equals(tc.getTestType())) {
//                 executeAPITest(tc, result, isMulti, actions);
//             }

//         } catch (AssertionError ae) {
//             log.error("Assertion failed for {}: {}", tc.getTestName(), ae.getMessage());
//             result.setStatus(TestStatus.FAILED);
//             result.setErrorMessage("AssertionError: " + ae.getMessage());
//         } catch (Exception e) {
//             log.error("Test case execution failed for {}: {}", tc.getTestName(), e.getMessage());
//             result.setStatus(TestStatus.FAILED);
//             String errorMsg = e.getMessage();
//             if (errorMsg != null && errorMsg.length() > 2000) {
//                 errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
//             }
//             result.setErrorMessage(errorMsg);
//         }

//         result.setDuration(System.currentTimeMillis() - start);
//         testResultService.saveTestResult(result);
//         log.info("Test Result Saved: {} - Status: {} (duration: {}ms)",
//                 tc.getTestName(), result.getStatus(), result.getDuration());
//     }

//     /**
//      * Enhanced UI test with better element waits and submit handling
//      */
//     private void executeUITest(TestCase tc, TestRun run, TestResult result, long start,
//             boolean isMulti, JsonNode actions) {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = createRealisticChromeOptions();
//         WebDriver driver = new ChromeDriver(options);
//         WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

//         try {
//             driver.get(tc.getUrlEndpoint());

//             // Wait for page load
//             Thread.sleep(2000);

//             // Handle special sites (Google, etc.)
//             if (tc.getUrlEndpoint().contains("google.com")) {
//                 injectStealthScripts(driver);
//                 handleCookieConsent(driver);
//             }

//             By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
//             String action = tc.getHttpMethodAction().toLowerCase();

//             // Enhanced element wait strategy
//             WebElement element = waitForElement(driver, wait, locator);

//             if (isMulti) {
//                 // Execute multiple actions
//                 for (JsonNode actionNode : actions) {
//                     String type = actionNode.get("type").asText();
//                     String value = actionNode.has("value") ? actionNode.get("value").asText() : tc.getInputData();
//                     performAction(driver, wait, element, locator, type, value, tc.getExpectedResult());
//                 }
//             } else {
//                 // Single action
//                 performAction(driver, wait, element, locator, action, tc.getInputData(), tc.getExpectedResult());
//             }

//             // Enhanced verification
//             boolean match = verifyResult(driver, wait, tc, locator);
//             result.setStatus(match ? TestStatus.PASSED : TestStatus.FAILED);

//             log.info("UI Test {}: {} (duration: {}ms)", tc.getTestName(), result.getStatus(),
//                     System.currentTimeMillis() - start);
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             log.error("UI test interrupted: {}", e.getMessage());
//             throw new RuntimeException("UI test interrupted", e);
//         } catch (Exception e) {
//             log.error("UI test failed: {}", e.getMessage());
//             throw new RuntimeException(e);
//         } finally {
//             driver.quit();
//         }
//     }

//     /**
//      * FIXED: Better element wait with proper exception handling for
//      * InterruptedException
//      */
//     private WebElement waitForElement(WebDriver driver, WebDriverWait wait, By locator) {
//         try {
//             // Strategy 1: Wait for presence
//             wait.until(ExpectedConditions.presenceOfElementLocated(locator));

//             // Strategy 2: Wait for visibility
//             WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

//             // Strategy 3: Scroll into view
//             ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
//             Thread.sleep(500);

//             // Strategy 4: Wait for clickability
//             element = wait.until(ExpectedConditions.elementToBeClickable(locator));

//             return element;
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             log.error("Element wait interrupted for locator: {}", locator);
//             throw new RuntimeException("Element wait interrupted", e);
//         } catch (Exception e) {
//             log.error("Element wait failed for locator: {}", locator);
//             throw new RuntimeException("Element wait failed", e);
//         }
//     }

//     /**
//      * Enhanced action performer with submit button handling
//      */
//     private void performAction(WebDriver driver, WebDriverWait wait, WebElement element, By locator,
//             String action, String inputData, String expectedResult) throws Exception {

//         switch (action.toLowerCase()) {
//             case "click":
//                 // Try click with fallback strategies
//                 try {
//                     element.click();
//                 } catch (Exception e) {
//                     log.warn("Regular click failed, trying JS click");
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
//                 }
//                 break;

//             case "submit":
//                 // Enhanced submit handling
//                 handleSubmitAction(driver, wait, element, locator);
//                 break;

//             case "type":
//             case "sendkeys":
//                 element.clear();
//                 Thread.sleep(300);
//                 element.sendKeys(inputData != null ? inputData : expectedResult);
//                 Thread.sleep(500);
//                 break;

//             case "clear":
//                 element.clear();
//                 break;

//             case "doubleclick":
//                 new org.openqa.selenium.interactions.Actions(driver).doubleClick(element).perform();
//                 break;

//             case "rightclick":
//             case "contextclick":
//                 new org.openqa.selenium.interactions.Actions(driver).contextClick(element).perform();
//                 break;

//             default:
//                 throw new IllegalArgumentException("Unsupported action: " + action);
//         }
//     }

//     /**
//      * Robust submit button handler with multiple strategies
//      */
//     private void handleSubmitAction(WebDriver driver, WebDriverWait wait, WebElement element, By locator)
//             throws Exception {
//         String tag = element.getTagName().toLowerCase();

//         // Strategy 1: If it's a submit button, just click it
//         if ("button".equals(tag) || "input".equals(tag)) {
//             String type = element.getDomAttribute("type");
//             if ("submit".equals(type)) {
//                 try {
//                     element.click();
//                     log.debug("Submit button clicked directly");
//                     return;
//                 } catch (Exception e) {
//                     log.warn("Direct click failed, trying JS click");
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
//                     return;
//                 }
//             }
//         }

//         // Strategy 2: Find submit button in same form
//         try {
//             List<WebElement> forms = element.findElements(By.xpath("./ancestor::form"));
//             if (!forms.isEmpty()) {
//                 WebElement form = forms.get(0);
//                 List<WebElement> submitButtons = form.findElements(
//                         By.cssSelector("button[type='submit'], input[type='submit'], button:not([type])"));
//                 if (!submitButtons.isEmpty()) {
//                     WebElement submitBtn = submitButtons.get(0);
//                     wait.until(ExpectedConditions.elementToBeClickable(submitBtn));

//                     // Scroll into view
//                     ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
//                     Thread.sleep(500);

//                     try {
//                         submitBtn.click();
//                         log.debug("Found and clicked submit button in ancestor form");
//                         return;
//                     } catch (Exception e) {
//                         ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
//                         log.debug("Clicked submit button via JS");
//                         return;
//                     }
//                 }
//             }
//         } catch (Exception e) {
//             log.warn("Strategy 2 (ancestor form) failed: {}", e.getMessage());
//         }

//         // Strategy 3: Look for any submit button on page
//         try {
//             List<WebElement> submitButtons = driver.findElements(
//                     By.cssSelector("button[type='submit'], input[type='submit']"));
//             if (!submitButtons.isEmpty()) {
//                 WebElement submitBtn = submitButtons.get(0);
//                 wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
//                 ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
//                 Thread.sleep(500);
//                 submitBtn.click();
//                 log.debug("Found and clicked first submit button on page");
//                 return;
//             }
//         } catch (Exception e) {
//             log.warn("Strategy 3 (page submit button) failed: {}", e.getMessage());
//         }

//         // Strategy 4: Press Enter on the element
//         try {
//             element.sendKeys(org.openqa.selenium.Keys.RETURN);
//             log.debug("Pressed RETURN key on element");
//             return;
//         } catch (Exception e) {
//             log.warn("Strategy 4 (RETURN key) failed: {}", e.getMessage());
//         }

//         throw new IllegalStateException("All submit strategies failed - no submit button found");
//     }

//     /**
//      * Enhanced result verification
//      */
//     private boolean verifyResult(WebDriver driver, WebDriverWait wait, TestCase tc, By locator) throws Exception {
//         String expected = tc.getExpectedResult();

//         if (expected == null || expected.isEmpty()) {
//             return true; // No verification needed
//         }

//         // Wait for potential page changes
//         Thread.sleep(1000);

//         if (expected.contains("typed") || expected.contains("Password typed")) {
//             // Verify input value
//             WebElement checkElement = driver.findElement(locator);
//             String actualValue = checkElement.getDomAttribute("value");
//             boolean match = actualValue != null && actualValue.contains(tc.getInputData());
//             log.debug("Verify: Element value '{}' contains input '{}' ? {}", actualValue, tc.getInputData(), match);
//             return match;
//         } else if (expected.contains("Search results") || expected.contains("Redirected")) {
//             // Verify navigation
//             wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(tc.getUrlEndpoint())));
//             boolean match = driver.getTitle().contains(expected.split(" ")[0])
//                     || driver.getCurrentUrl().toLowerCase().contains(expected.toLowerCase());
//             log.debug("Verify: Title/URL '{}' matches expected '{}' ? {}", driver.getTitle(), expected, match);
//             return match;
//         } else {
//             // Verify page text
//             String bodyText = driver.findElement(By.tagName("body")).getText();
//             boolean match = bodyText.contains(expected);
//             log.debug("Verify: Body text contains '{}' ? {}", expected, match);
//             return match;
//         }
//     }

//     /**
//      * Enhanced API test with proper POST handling
//      */
//     private void executeAPITest(TestCase tc, TestResult result, boolean isMulti, JsonNode actions) {
//         RestAssured.baseURI = tc.getUrlEndpoint();
//         String method = tc.getHttpMethodAction().toUpperCase();
//         String body = tc.getInputData() != null ? tc.getInputData().trim() : "";
//         String expectedStr = tc.getExpectedResult().split(" ")[0];
//         int expectedCode = Integer.parseInt(expectedStr);

//         try {
//             if (isMulti) {
//                 for (JsonNode actionNode : actions) {
//                     String apiMethod = actionNode.get("type").asText().toUpperCase();
//                     String apiBody = actionNode.has("value") ? actionNode.get("value").asText() : body;
//                     executeApiCall(apiMethod, apiBody, expectedCode, tc.getUrlEndpoint());
//                 }
//             } else {
//                 executeApiCall(method, body, expectedCode, tc.getUrlEndpoint());
//             }
//             result.setStatus(TestStatus.PASSED);
//         } catch (AssertionError ae) {
//             if (ae.getMessage().contains("401") || ae.getMessage().contains("404")) {
//                 log.warn("API {} returned {}, marking as failed", tc.getUrlEndpoint(),
//                         ae.getMessage().contains("401") ? "401" : "404");
//                 result.setStatus(TestStatus.FAILED);
//                 result.setErrorMessage("API issue: " + ae.getMessage());
//             } else {
//                 throw ae;
//             }
//         }
//     }

//     /**
//      * Enhanced API call with proper POST body handling
//      */
//     private void executeApiCall(String method, String body, int expectedCode, String url) {
//         log.debug("API Call: {} {} (expected: {})", method, url, expectedCode);

//         Response response;
//         if ("GET".equals(method)) {
//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .when()
//                     .get();
//         } else if ("POST".equals(method)) {
//             // Proper POST with body
//             String requestBody = body.isEmpty() ? "{}" : body;
//             log.debug("POST body: {}", requestBody);

//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .body(requestBody)
//                     .when()
//                     .post();
//         } else if ("PUT".equals(method)) {
//             String requestBody = body.isEmpty() ? "{}" : body;
//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .body(requestBody)
//                     .when()
//                     .put();
//         } else if ("DELETE".equals(method)) {
//             response = given()
//                     .contentType(ContentType.JSON)
//                     .accept(ContentType.JSON)
//                     .when()
//                     .delete();
//         } else {
//             throw new IllegalArgumentException("Unsupported API method: " + method);
//         }

//         int actualCode = response.getStatusCode();
//         log.debug("API Response: {} (expected: {})", actualCode, expectedCode);

//         // Log response body for debugging
//         if (actualCode != expectedCode) {
//             log.warn("Status mismatch - Response body: {}", response.getBody().asString());
//         }

//         response.then().statusCode(expectedCode);
//     }

//     private void injectStealthScripts(WebDriver driver) {
//         JavascriptExecutor js = (JavascriptExecutor) driver;
//         js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
//         js.executeScript("window.chrome = {runtime: {}};");
//         log.debug("Stealth scripts injected");
//     }

//     private void handleCookieConsent(WebDriver driver) {
//         try {
//             Thread.sleep(1000);
//             WebElement acceptCookies = driver.findElement(
//                     By.xpath("//button[contains(., 'Accept') or contains(., 'I agree')]"));
//             acceptCookies.click();
//             Thread.sleep(1000);
//             log.debug("Cookie consent accepted");
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             log.debug("Cookie consent check interrupted");
//         } catch (Exception e) {
//             log.debug("No cookie consent found");
//         }
//     }

//     private By getLocator(String type, String value) {
//         if (type == null || value == null)
//             return By.id("default");
//         return switch (type.toLowerCase()) {
//             case "id" -> By.id(value);
//             case "name" -> By.name(value);
//             case "xpath" -> By.xpath(value);
//             case "css" -> By.cssSelector(value);
//             default -> By.id(value);
//         };
//     }
// }
package com.example.test_framework_api.worker;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import io.restassured.RestAssured;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import static io.restassured.RestAssured.given;
import com.example.test_framework_api.model.TestCase;
import com.example.test_framework_api.model.TestRun;
import com.example.test_framework_api.model.TestResult;
import com.example.test_framework_api.model.TestStatus;
import com.example.test_framework_api.pageobjects.TestPage;
import com.example.test_framework_api.service.TestResultService;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.time.Duration;

/**
 * FIXED ISSUES:
 * 1. Added missing executeDynamicTest method
 * 2. Fixed InterruptedException handling in waitForElement
 * 3. Enhanced element waits, POST handling, and submit button clicks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestExecutor {

    private final TestResultService testResultService;

    @Value("${app.base-url:http://127.0.0.1:5500}")
    private String baseUrl;

    public void executeTest() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = createRealisticChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            TestPage page = new TestPage(driver);
            page.open(baseUrl);
            page.validateTitle();
            page.performAction();
        } catch (Exception e) {
            log.error("Test execution failed: {}", e.getMessage());
            throw e;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * ADDED: Dynamic test execution method called from WorkerListener
     */
    public void executeDynamicTest(String url, String elementId, String action, 
                                   String expectedResult, String inputValue) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = createRealisticChromeOptions();
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            log.info("Executing dynamic test: URL={}, Element={}, Action={}", url, elementId, action);
            
            driver.get(url);
            Thread.sleep(2000); // Wait for page load

            // Handle special sites
            if (url.contains("google.com")) {
                injectStealthScripts(driver);
                handleCookieConsent(driver);
            }

            // Locate element
            By locator = By.id(elementId);
            WebElement element = waitForElement(driver, wait, locator);

            // Perform action
            performAction(driver, wait, element, locator, action, inputValue, expectedResult);

            // Verify result
            Thread.sleep(1000);
            if (expectedResult != null && !expectedResult.isEmpty()) {
                boolean verified = verifyDynamicResult(driver, expectedResult, elementId);
                if (!verified) {
                    throw new AssertionError("Expected result not met: " + expectedResult);
                }
            }

            log.info("Dynamic test PASSED: {} on {}", action, elementId);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Dynamic test interrupted: {}", e.getMessage());
            throw new RuntimeException("Test interrupted", e);
        } catch (Exception e) {
            log.error("Dynamic test FAILED: {}", e.getMessage());
            throw new RuntimeException("Dynamic test execution failed", e);
        } finally {
            driver.quit();
        }
    }

    /**
     * ADDED: Helper method to verify dynamic test results
     */
    private boolean verifyDynamicResult(WebDriver driver, String expectedResult, String elementId) {
        try {
            if (expectedResult.contains("typed") || expectedResult.contains("Password typed")) {
                WebElement element = driver.findElement(By.id(elementId));
                String actualValue = element.getDomAttribute("value");
                return actualValue != null && !actualValue.isEmpty();
            } else if (expectedResult.contains("Redirected") || expectedResult.contains("results")) {
                return !driver.getCurrentUrl().equals(driver.getCurrentUrl());
            } else {
                String bodyText = driver.findElement(By.tagName("body")).getText();
                return bodyText.contains(expectedResult);
            }
        } catch (Exception e) {
            log.warn("Result verification failed: {}", e.getMessage());
            return false;
        }
    }

    private ChromeOptions createRealisticChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
        options.setExperimentalOption("useAutomationExtension", false);
        return options;
    }

    /**
     * Enhanced test case execution with better element handling
     */
    public void executeTestCase(TestCase tc, TestRun run) {
        TestResult result = new TestResult();
        result.setTestName(tc.getTestName());
        result.setTestRun(run);
        result.setTestSuite(tc.getTestSuite()); // CRITICAL: Link result to suite
        long start = System.currentTimeMillis();

        try {
            JsonNode actions = tc.getActions();
            boolean isMulti = actions != null && actions.isArray() && actions.size() > 0;

            if ("UI".equals(tc.getTestType())) {
                executeUITest(tc, run, result, start, isMulti, actions);
            } else if ("API".equals(tc.getTestType())) {
                executeAPITest(tc, result, isMulti, actions);
            }

        } catch (AssertionError ae) {
            log.error("Assertion failed for {}: {}", tc.getTestName(), ae.getMessage());
            result.setStatus(TestStatus.FAILED);
            result.setErrorMessage("AssertionError: " + ae.getMessage());
        } catch (Exception e) {
            log.error("Test case execution failed for {}: {}", tc.getTestName(), e.getMessage());
            result.setStatus(TestStatus.FAILED);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000) + "... (truncated)";
            }
            result.setErrorMessage(errorMsg);
        }

        result.setDuration(System.currentTimeMillis() - start);
        testResultService.saveTestResult(result);
        log.info("Test Result Saved: {} - Status: {} (duration: {}ms)",
                tc.getTestName(), result.getStatus(), result.getDuration());
    }

    /**
     * Enhanced UI test with better element waits and submit handling
     */
    private void executeUITest(TestCase tc, TestRun run, TestResult result, long start,
            boolean isMulti, JsonNode actions) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = createRealisticChromeOptions();
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            driver.get(tc.getUrlEndpoint());

            // Wait for page load
            Thread.sleep(2000);

            // Handle special sites (Google, etc.)
            if (tc.getUrlEndpoint().contains("google.com")) {
                injectStealthScripts(driver);
                handleCookieConsent(driver);
            }

            By locator = getLocator(tc.getLocatorType(), tc.getLocatorValue());
            String action = tc.getHttpMethodAction().toLowerCase();

            // Enhanced element wait strategy
            WebElement element = waitForElement(driver, wait, locator);

            if (isMulti) {
                // Execute multiple actions
                for (JsonNode actionNode : actions) {
                    String type = actionNode.get("type").asText();
                    String value = actionNode.has("value") ? actionNode.get("value").asText() : tc.getInputData();
                    performAction(driver, wait, element, locator, type, value, tc.getExpectedResult());
                }
            } else {
                // Single action
                performAction(driver, wait, element, locator, action, tc.getInputData(), tc.getExpectedResult());
            }

            // Enhanced verification
            boolean match = verifyResult(driver, wait, tc, locator);
            result.setStatus(match ? TestStatus.PASSED : TestStatus.FAILED);

            log.info("UI Test {}: {} (duration: {}ms)", tc.getTestName(), result.getStatus(),
                    System.currentTimeMillis() - start);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("UI test interrupted: {}", e.getMessage());
            throw new RuntimeException("UI test interrupted", e);
        } catch (Exception e) {
            log.error("UI test failed: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
    }

    /**
     * FIXED: Better element wait with proper exception handling for InterruptedException
     */
    private WebElement waitForElement(WebDriver driver, WebDriverWait wait, By locator) {
        try {
            // Strategy 1: Wait for presence
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));

            // Strategy 2: Wait for visibility
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

            // Strategy 3: Scroll into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            Thread.sleep(500);

            // Strategy 4: Wait for clickability
            element = wait.until(ExpectedConditions.elementToBeClickable(locator));

            return element;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Element wait interrupted for locator: {}", locator);
            throw new RuntimeException("Element wait interrupted", e);
        } catch (Exception e) {
            log.error("Element wait failed for locator: {}", locator);
            throw new RuntimeException("Element wait failed", e);
        }
    }

    /**
     * Enhanced action performer with submit button handling
     */
    private void performAction(WebDriver driver, WebDriverWait wait, WebElement element, By locator,
            String action, String inputData, String expectedResult) throws Exception {

        switch (action.toLowerCase()) {
            case "click":
                // Try click with fallback strategies
                try {
                    element.click();
                } catch (Exception e) {
                    log.warn("Regular click failed, trying JS click");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                }
                break;

            case "submit":
                // Enhanced submit handling
                handleSubmitAction(driver, wait, element, locator);
                break;

            case "type":
            case "sendkeys":
                element.clear();
                Thread.sleep(300);
                element.sendKeys(inputData != null ? inputData : expectedResult);
                Thread.sleep(500);
                break;

            case "clear":
                element.clear();
                break;

            case "doubleclick":
                new org.openqa.selenium.interactions.Actions(driver).doubleClick(element).perform();
                break;

            case "rightclick":
            case "contextclick":
                new org.openqa.selenium.interactions.Actions(driver).contextClick(element).perform();
                break;

            default:
                throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    /**
     * Robust submit button handler with multiple strategies
     */
    private void handleSubmitAction(WebDriver driver, WebDriverWait wait, WebElement element, By locator)
            throws Exception {
        String tag = element.getTagName().toLowerCase();

        // Strategy 1: If it's a submit button, just click it
        if ("button".equals(tag) || "input".equals(tag)) {
            String type = element.getDomAttribute("type");
            if ("submit".equals(type)) {
                try {
                    element.click();
                    log.debug("Submit button clicked directly");
                    return;
                } catch (Exception e) {
                    log.warn("Direct click failed, trying JS click");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    return;
                }
            }
        }

        // Strategy 2: Find submit button in same form
        try {
            List<WebElement> forms = element.findElements(By.xpath("./ancestor::form"));
            if (!forms.isEmpty()) {
                WebElement form = forms.get(0);
                List<WebElement> submitButtons = form.findElements(
                        By.cssSelector("button[type='submit'], input[type='submit'], button:not([type])"));
                if (!submitButtons.isEmpty()) {
                    WebElement submitBtn = submitButtons.get(0);
                    wait.until(ExpectedConditions.elementToBeClickable(submitBtn));

                    // Scroll into view
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
                    Thread.sleep(500);

                    try {
                        submitBtn.click();
                        log.debug("Found and clicked submit button in ancestor form");
                        return;
                    } catch (Exception e) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
                        log.debug("Clicked submit button via JS");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Strategy 2 (ancestor form) failed: {}", e.getMessage());
        }

        // Strategy 3: Look for any submit button on page
        try {
            List<WebElement> submitButtons = driver.findElements(
                    By.cssSelector("button[type='submit'], input[type='submit']"));
            if (!submitButtons.isEmpty()) {
                WebElement submitBtn = submitButtons.get(0);
                wait.until(ExpectedConditions.elementToBeClickable(submitBtn));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", submitBtn);
                Thread.sleep(500);
                submitBtn.click();
                log.debug("Found and clicked first submit button on page");
                return;
            }
        } catch (Exception e) {
            log.warn("Strategy 3 (page submit button) failed: {}", e.getMessage());
        }

        // Strategy 4: Press Enter on the element
        try {
            element.sendKeys(org.openqa.selenium.Keys.RETURN);
            log.debug("Pressed RETURN key on element");
            return;
        } catch (Exception e) {
            log.warn("Strategy 4 (RETURN key) failed: {}", e.getMessage());
        }

        throw new IllegalStateException("All submit strategies failed - no submit button found");
    }

    /**
     * Enhanced result verification
     */
    private boolean verifyResult(WebDriver driver, WebDriverWait wait, TestCase tc, By locator) throws Exception {
        String expected = tc.getExpectedResult();

        if (expected == null || expected.isEmpty()) {
            return true; // No verification needed
        }

        // Wait for potential page changes
        Thread.sleep(1000);

        if (expected.contains("typed") || expected.contains("Password typed")) {
            // Verify input value
            WebElement checkElement = driver.findElement(locator);
            String actualValue = checkElement.getDomAttribute("value");
            boolean match = actualValue != null && actualValue.contains(tc.getInputData());
            log.debug("Verify: Element value '{}' contains input '{}' ? {}", actualValue, tc.getInputData(), match);
            return match;
        } else if (expected.contains("Search results") || expected.contains("Redirected")) {
            // Verify navigation
            wait.until(ExpectedConditions.not(ExpectedConditions.urlToBe(tc.getUrlEndpoint())));
            boolean match = driver.getTitle().contains(expected.split(" ")[0])
                    || driver.getCurrentUrl().toLowerCase().contains(expected.toLowerCase());
            log.debug("Verify: Title/URL '{}' matches expected '{}' ? {}", driver.getTitle(), expected, match);
            return match;
        } else {
            // Verify page text
            String bodyText = driver.findElement(By.tagName("body")).getText();
            boolean match = bodyText.contains(expected);
            log.debug("Verify: Body text contains '{}' ? {}", expected, match);
            return match;
        }
    }

    /**
     * Enhanced API test with proper POST handling
     */
    private void executeAPITest(TestCase tc, TestResult result, boolean isMulti, JsonNode actions) {
        RestAssured.baseURI = tc.getUrlEndpoint();
        String method = tc.getHttpMethodAction().toUpperCase();
        String body = tc.getInputData() != null ? tc.getInputData().trim() : "";
        String expectedStr = tc.getExpectedResult().split(" ")[0];
        int expectedCode = Integer.parseInt(expectedStr);

        try {
            if (isMulti) {
                for (JsonNode actionNode : actions) {
                    String apiMethod = actionNode.get("type").asText().toUpperCase();
                    String apiBody = actionNode.has("value") ? actionNode.get("value").asText() : body;
                    executeApiCall(apiMethod, apiBody, expectedCode, tc.getUrlEndpoint());
                }
            } else {
                executeApiCall(method, body, expectedCode, tc.getUrlEndpoint());
            }
            result.setStatus(TestStatus.PASSED);
        } catch (AssertionError ae) {
            if (ae.getMessage().contains("401") || ae.getMessage().contains("404")) {
                log.warn("API {} returned {}, marking as failed", tc.getUrlEndpoint(),
                        ae.getMessage().contains("401") ? "401" : "404");
                result.setStatus(TestStatus.FAILED);
                result.setErrorMessage("API issue: " + ae.getMessage());
            } else {
                throw ae;
            }
        }
    }

    /**
     * Enhanced API call with proper POST body handling
     */
    private void executeApiCall(String method, String body, int expectedCode, String url) {
        log.debug("API Call: {} {} (expected: {})", method, url, expectedCode);

        Response response;
        if ("GET".equals(method)) {
            response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .when()
                    .get();
        } else if ("POST".equals(method)) {
            // Proper POST with body
            String requestBody = body.isEmpty() ? "{}" : body;
            log.debug("POST body: {}", requestBody);

            response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post();
        } else if ("PUT".equals(method)) {
            String requestBody = body.isEmpty() ? "{}" : body;
            response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .put();
        } else if ("DELETE".equals(method)) {
            response = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .when()
                    .delete();
        } else {
            throw new IllegalArgumentException("Unsupported API method: " + method);
        }

        int actualCode = response.getStatusCode();
        log.debug("API Response: {} (expected: {})", actualCode, expectedCode);

        // Log response body for debugging
        if (actualCode != expectedCode) {
            log.warn("Status mismatch - Response body: {}", response.getBody().asString());
        }

        response.then().statusCode(expectedCode);
    }

    private void injectStealthScripts(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        js.executeScript("window.chrome = {runtime: {}};");
        log.debug("Stealth scripts injected");
    }

    private void handleCookieConsent(WebDriver driver) {
        try {
            Thread.sleep(1000);
            WebElement acceptCookies = driver.findElement(
                    By.xpath("//button[contains(., 'Accept') or contains(., 'I agree')]"));
            acceptCookies.click();
            Thread.sleep(1000);
            log.debug("Cookie consent accepted");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Cookie consent check interrupted");
        } catch (Exception e) {
            log.debug("No cookie consent found");
        }
    }

    private By getLocator(String type, String value) {
        if (type == null || value == null)
            return By.id("default");
        return switch (type.toLowerCase()) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "xpath" -> By.xpath(value);
            case "css" -> By.cssSelector(value);
            default -> By.id(value);
        };
    }
}