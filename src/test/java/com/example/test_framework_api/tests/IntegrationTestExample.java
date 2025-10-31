package com.example.test_framework_api.tests;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.*; // <-- ADD THIS

@ActiveProfiles("mock")
@SpringBootTest
public class IntegrationTestExample {

    @Value("${external.api.base-url}")
    private String baseUrl;

    @Test
    void apiTest() {
        RestAssured.given()
            .baseUri(baseUrl)
            .when()
            .get("/todos/1")
            .then()
            .statusCode(200)
            .body("title", is(notNullValue())); // OK
    }
}