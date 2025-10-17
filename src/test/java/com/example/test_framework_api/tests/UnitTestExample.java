package com.example.test_framework_api.tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitTestExample {

    @BeforeEach
    void setUp() {
        System.out.println("Setup for test");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Teardown after test");
    }

    @Test
    void basicTest() {
        assertEquals(2, 1 + 1);
    }

    @ParameterizedTest
    @CsvSource({"1,1,2", "2,3,5"}) // Data-driven (Best practice)
    void parameterizedTest(int a, int b, int expected) {
        Assumptions.assumeTrue(a > 0 && b > 0); // Assumptions example (skip if false)
        assertEquals(expected, a + b);
    }

}