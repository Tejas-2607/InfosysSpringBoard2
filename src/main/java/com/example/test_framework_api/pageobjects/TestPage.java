package com.example.test_framework_api.pageobjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;

public class TestPage {
    private final WebDriver driver;
    private final By titleLocator = By.tagName("h1"); // Now used in validateTitle

    public TestPage(WebDriver driver) {
        this.driver = driver;
    }

    public void navigateToTestPage() {
        driver.get("http://127.0.0.1:5500/testpage.html");
    }

    public void validateTitle() {
        if (driver.findElements(titleLocator).isEmpty() || !driver.getTitle().contains("Test Page")) {
            throw new AssertionError("Title mismatch");
        }
    }

    public void performAction() {
        // Simulate action
        driver.findElement(By.id("test-button")).click();
    }

    public void validateUrl() {
        if (driver.getCurrentUrl().isEmpty()) {
            throw new AssertionError("URL validation failed");
        }
    }
}