package com.example.test_framework_api.tests;

import com.example.test_framework_api.pageobjects.TestPage;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

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
        driver.quit();
    }

    @Step("UI Test Step")
    @Test
    void uiTest() {
        testPage.navigateToTestPage();
        testPage.validateTitle();
        attachScreenshot("After Test"); // Simulated
    }

    @Attachment(value = "Screenshot", type = "image/png")
    public byte[] attachScreenshot(String name) {
        // Simulate screenshot bytes
        return new byte[0];
    }
}