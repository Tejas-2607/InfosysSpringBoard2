package com.example.test_framework_api.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Assumptions; // Added for assumptions
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Unit tests (Pyramid base: Fast, cheap, isolated)
// Difference: JUnit 4 (@Test, @Before/@After); JUnit 5 (modular, @BeforeEach/@AfterEach, parameterized); TestNG (@Test, @BeforeMethod/@AfterMethod, groups/dependencies for suites)
public class UnitTestExample {

    @BeforeEach
    void setUp() {
        // Reusable fixture (Best practice)
        System.out.println("Setup for test");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Teardown after test");
    }

    @Test
    void basicTest() {
        // JUnit 5 annotation example
        assertEquals(2, 1 + 1);
    }

    @ParameterizedTest
    @CsvSource({"1,1,2", "2,3,5"}) // Data-driven (Best practice)
    void parameterizedTest(int a, int b, int expected) {
        Assumptions.assumeTrue(a > 0 && b > 0); // Assumptions example (skip if false)
        assertEquals(expected, a + b);
    }

    // Parallel enabled via config (Team 2)
}