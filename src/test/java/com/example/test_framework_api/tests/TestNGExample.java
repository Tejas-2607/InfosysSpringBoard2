package com.example.test_framework_api.tests;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

// TestNG example (for comparison to JUnit)
public class TestNGExample {

    @BeforeMethod
    void setUp() {
        System.out.println("TestNG Setup");
    }

    @AfterMethod
    void tearDown() {
        System.out.println("TestNG Teardown");
    }

    @Test
    void basicTest() {
        Assert.assertEquals(1 + 1, 2);
    }

    @DataProvider(name = "sumData")
    public Object[][] sumData() {
        return new Object[][] {{1, 1, 2}, {2, 3, 5}};
    }

    @Test(dataProvider = "sumData")
    void parameterizedTest(int a, int b, int expected) {
        Assert.assertEquals(a + b, expected);
    }

    // Suites: Use testng.xml for organization (e.g., <suite><test><classes>...</>)
}