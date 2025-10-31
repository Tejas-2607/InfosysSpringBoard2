package com.example.test_framework_api.tests;

import com.example.test_framework_api.pageobjects.TestPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

@Epic("UI Automation Framework")
@Feature("Button Click Validation")
public class UITestExample {

    private WebDriver driver;
    private TestPage testPage;

    @BeforeEach
    void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        testPage = new TestPage(driver);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

   
    @Test
    @Story("User clicks test button and page responds")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Validates that the test button is clickable and page title is correct")
    @Link(name = "Test Page", url = "http://127.0.0.1:5500/testpage.html")
    @Issue("TEST-001")
    @TmsLink("TMS-100")
    @Step("Open test page, validate title, and click button")
    void uiTest() {
        testPage.open("http://127.0.0.1:5500");
        testPage.validateTitle();
        testPage.performAction();
        attachScreenshot("After Test"); // Simulated
    }

    @Attachment(value = "Screenshot", type = "image/png")
    public byte[] attachScreenshot(String name) {
        // Simulate screenshot bytes
        return ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
    }
}


// package com.example.test_framework_api.tests;

// import com.example.test_framework_api.pageobjects.TestPage;
// import io.github.bonigarcia.wdm.WebDriverManager;
// import io.qameta.allure.*;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.openqa.selenium.WebDriver;
// import org.openqa.selenium.chrome.ChromeDriver;
// import org.openqa.selenium.chrome.ChromeOptions;

// @Epic("UI Automation Framework")
// @Feature("Button Click Validation")
// public class UITestExample {

//     private WebDriver driver;
//     private TestPage testPage;

//     @BeforeEach
//     void setUp() {
//         WebDriverManager.chromedriver().setup();
//         ChromeOptions options = new ChromeOptions();
//         options.addArguments("--headless");
//         driver = new ChromeDriver(options);
//         testPage = new TestPage(driver);
//     }

//     @AfterEach
//     void tearDown() {
//         if (driver != null) {
//             driver.quit();
//         }
//     }

//     @Test
//     @Story("User clicks test button and page responds")
//     @Severity(SeverityLevel.CRITICAL)
//     @Description("Validates that the test button is clickable and page title is correct")
//     @Link(name = "Test Page", url = "http://127.0.0.1:5500/testpage.html")
//     @Issue("TEST-001")
//     @TmsLink("TMS-100")
//     @Step("Open test page, validate title, and click button")
//     void uiTest() {
//         testPage.open("http://127.0.0.1:5500");
//         testPage.validateTitle();
//         testPage.performAction();
//         attachScreenshot("After Button Click");
//     }

//     @Attachment(value = "Screenshot - {0}", type = "image/png")
//     public byte[] attachScreenshot(String name) {
//         return ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
//     }
// }