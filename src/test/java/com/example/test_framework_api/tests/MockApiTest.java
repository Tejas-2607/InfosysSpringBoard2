package com.example.test_framework_api.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
// import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.*; // <-- ADD THIS
import static org.hamcrest.Matchers.equalTo;

public class MockApiTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        stubFor(get(urlEqualTo("/api/todos/1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "userId": 1,
                        "id": 1,
                        "title": "Mocked Task",
                        "completed": false
                    }
                    """)));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testWithMockApi() {
        RestAssured.given()
            .baseUri("http://localhost:8089")
            .when()
            .get("/api/todos/1")
            .then()
            .statusCode(200)
            .body("title", is(notNullValue()))           // OK
            .body("title", is(equalTo("Mocked Task")))   // FIXED
            .body("completed", is(equalTo(false)));      // FIXED
    }
}