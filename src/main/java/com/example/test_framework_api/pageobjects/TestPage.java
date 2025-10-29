// package com.example.test_framework_api.pageobjects;

// import org.openqa.selenium.WebDriver;
// import org.openqa.selenium.WebElement;
// import org.openqa.selenium.support.FindBy;
// import org.openqa.selenium.support.PageFactory;
// import org.openqa.selenium.support.ui.ExpectedConditions;
// import org.openqa.selenium.support.ui.WebDriverWait;

// import java.time.Duration;

// public class TestPage {

//     private final WebDriver driver;
//     private final WebDriverWait wait;

//     @FindBy(id = "page-header")
//     private WebElement header;

//     @FindBy(id = "test-button")
//     private WebElement testButton;
//     private final String baseUrl = "http://localhost:5500/";

//     public TestPage(WebDriver driver) {
//         this.driver = driver;
//         this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
//         PageFactory.initElements(driver, this);
//     }

//     public void open() {
//         driver.get(baseUrl + "/testpage.html");
//         wait.until(ExpectedConditions.titleIs("My Test Page"));
//     }

//     public void validateTitle() {
//         String actual = driver.getTitle();
//         if (!"My Test Page".equals(actual)) {
//             throw new AssertionError("Title mismatch – expected 'My Test Page' but got '" + actual + "'");
//         }
//     }

//     public void performAction() {
//         open();
//         wait.until(ExpectedConditions.elementToBeClickable(testButton)).click();
//     }
// }

package com.example.test_framework_api.pageobjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class TestPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "page-header")
    private WebElement header;

    @FindBy(id = "test-button")
    private WebElement testButton;

    public TestPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    /** Open the page and WAIT for the title */
    public void open(String baseUrl) {
        String url = baseUrl.endsWith("/") ? baseUrl + "testpage.html" : baseUrl + "/testpage.html";
        driver.get(url);

        // THIS IS THE CRUCIAL LINE THAT WAS MISSING
        wait.until(ExpectedConditions.titleIs("My Test Page"));
    }

    public void validateTitle() {
        String actual = driver.getTitle();
        if (!"My Test Page".equals(actual)) {
            throw new AssertionError("Title mismatch – expected 'My Test Page' but got '" + actual + "'");
        }
    }

    public void performAction() {
        wait.until(ExpectedConditions.elementToBeClickable(testButton)).click();
    }
}