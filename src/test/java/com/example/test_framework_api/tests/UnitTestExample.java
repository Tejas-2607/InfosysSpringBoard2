package com.example.test_framework_api.tests;

import com.example.test_framework_api.service.TestDataService;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev") // Multi-env
public class UnitTestExample {


    @BeforeEach
    void setUp() {
        System.out.println("Setup for test");
    }

    @Step("Basic Unit Test")
    @Test
    void basicTest() {
        assertEquals(2, 1 + 1);
    }

    static Stream<Object[]> externalData() {
        // In real: autowired service
        TestDataService service = new TestDataService(); // Fallback mock
        List<Map<String, Object>> data = service.loadTestData("src/test/resources/test-data.json");
        return data.stream().map(d -> new Object[]{d.get("a"), d.get("b"), d.get("expected")});
    }

    @ParameterizedTest
    @MethodSource("externalData")
    void parameterizedTest(int a, int b, int expected) {
        assertEquals(expected, a + b);
    }
}