package com.example.test_framework_api.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

// UI tests (Pyramid top: Slow, expensive, use sparingly)
public class UITestExample {

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver(); // WebDriver basics
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    void uiTest() {
        driver.get("https://www.google.com");
        driver.findElement(By.name("q")).sendKeys("Selenium test"); // Locators: name, XPath/CSS possible
        // POM example: Normally use Page Object class, e.g., GooglePage page = new GooglePage(driver); page.search("test");
    }
}