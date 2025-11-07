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

package com.example.test_framework_api.worker;

import com.example.test_framework_api.pageobjects.TestPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
// import org.openqa.selenium.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;

import java.time.Duration;
import java.util.List;

@Component
public class TestExecutor {

    @Value("http://127.0.0.1:5500")
    private String baseUrl;

    public void executeTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(options);

        try {
            TestPage page = new TestPage(driver);
            page.open(baseUrl); // waits for title
            page.validateTitle(); // now passes
            page.performAction(); // button is found
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
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Actions actions = new Actions(driver);

        try {
            driver.get(url);
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(elementId)));
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
                    // For rightclick/hover, ensure it's a valid target
                    if ("rightclick".equals(action.toLowerCase()) || "hover".equals(action.toLowerCase())) {
                        new Actions(driver).moveToElement(element).contextClick(element).perform(); // Use moveToElement
                                                                                                    // first
                    }
                    break;
                case "clear":
                    element.clear();
                    break;
                case "submit":
                    String tag = element.getTagName().toLowerCase();
                    if ("form".equals(tag)) {
                        // If targeting form, find and click inner submit button/input
                        List<WebElement> submitElements = element
                                .findElements(By.cssSelector("button[type='submit'], input[type='submit']"));
                        if (!submitElements.isEmpty()) {
                            submitElements.get(0).click();
                            System.out.println("Submitted form " + elementId + " via inner submit element");
                        } else {
                            throw new IllegalArgumentException("No submit button/input found inside form " + elementId);
                        }
                    } else {
                        // Try submit on nested element (input/button)
                        try {
                            if ("button".equals(tag)) {
                                // Buttons submit better via click
                                element.click();
                            } else {
                                element.submit();
                            }
                        } catch (InvalidElementStateException ignored) {
                            // Fallback: Find ancestor form and submit via its inner submit element
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
                    // FIXED: Added "type" case — use expectedResult as input text
                    element.sendKeys(expectedResult != null ? expectedResult : "default text");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported action: " + action);
            }

            // Verify result if provided
            if (expectedResult != null) {
                if (action.toLowerCase().equals("type") || action.toLowerCase().equals("clear")) {
                    // For type/clear, check element value
                    wait.until(ExpectedConditions.attributeToBe(element, "value", expectedResult));
                } else {
                    // For others, check body text
                    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), expectedResult));
                }
            }
            System.out.println("Dynamic test PASSED: " + action + " on " + elementId + " at " + url);

        } catch (Exception e) {
            System.err.println("Dynamic test FAILED: " + e.getMessage());
            throw e;
        } finally {
            driver.quit();
        }
    }
}