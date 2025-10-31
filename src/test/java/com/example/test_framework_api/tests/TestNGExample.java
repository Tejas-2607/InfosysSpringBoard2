package com.example.test_framework_api.tests;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.*;

@Epic("Unit & Parameterized Tests")
@Feature("Math Operations")
public class TestNGExample {

    @BeforeMethod
    @Step("Setup test environment")
    void setUp() {
        System.out.println("TestNG Setup");
    }

    @AfterMethod
    @Step("Teardown test environment")
    void tearDown() {
        System.out.println("TestNG Teardown");
    }

    @Test
    @Story("Basic arithmetic validation")
    @Severity(SeverityLevel.NORMAL)
    void basicTest() {
        Assert.assertEquals(1 + 1, 2, "1 + 1 should equal 2");
    }

    @DataProvider(name = "sumData")
    public Object[][] sumData() {
        return new Object[][] {{1, 1, 2}, {2, 3, 5}, {10, 20, 30}};
    }

    @Test(dataProvider = "sumData")
    @Story("Parameterized addition test")
    @Severity(SeverityLevel.TRIVIAL)
    @Description("Validates addition with multiple inputs")
    void parameterizedTest(int a, int b, int expected) {
        int result = a + b;
        Assert.assertEquals(result, expected, a + " + " + b + " should be " + expected);
    }
}