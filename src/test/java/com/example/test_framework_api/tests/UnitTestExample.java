package com.example.test_framework_api.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Unit tests (Pyramid base: Fast, cheap, isolated)
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
        assertEquals(expected, a + b);
    }

    // Parallel enabled via config (Team 2)
}