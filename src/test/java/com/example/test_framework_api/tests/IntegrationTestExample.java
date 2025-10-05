package com.example.test_framework_api.tests;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

// Integration tests (Pyramid middle: API interactions)
public class IntegrationTestExample {

    @Test
    void apiTest() {
        // REST-Assured example (Team 4 for API)
        RestAssured.get("https://jsonplaceholder.typicode.com/todos/1")
                .then()
                .statusCode(200)
                .body("title", org.hamcrest.Matchers.notNullValue()); // Assertion
    }
}