package com.example.test_framework_api.dataprovider;

import io.restassured.RestAssured;
import org.testng.annotations.DataProvider;
import io.restassured.common.mapper.TypeRef;
import java.util.List;
import java.util.Map;

/**
 * Fetches test data from external API.
 * Example: GET https://jsonplaceholder.typicode.com/posts?_limit=3
 */
public class ApiDataProvider {

  @DataProvider(name = "apiData")
  public static Object[][] provideFromApi() {
    List<Map<String, Object>> posts = RestAssured
        .given()
        .baseUri("https://jsonplaceholder.typicode.com")
        .when()
        .get("/posts?_limit=3")
        .then()
        .extract()
        .body()
        .as(new TypeRef<List<Map<String, Object>>>() {
        });

    return posts.stream()
        .map(p -> new Object[] { p.get("userId"), p.get("id"), p.get("title").toString().length() })
        .toArray(Object[][]::new);
  }
}