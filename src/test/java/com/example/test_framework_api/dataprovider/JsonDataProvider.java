package com.example.test_framework_api.dataprovider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Reads test data from JSON files.
 * Format: List of Maps (e.g., [{"a":1,"b":2,"expected":3}, ...])
 */
public class JsonDataProvider {

  private static final ObjectMapper mapper = new ObjectMapper();

  @DataProvider(name = "jsonData")
  public static Object[][] provideFromJson() {
    return loadData("src/test/resources/test-data.json");
  }

  private static Object[][] loadData(String path) {
    try {
      List<Map<String, Object>> data = mapper.readValue(
          new File(path),
          new TypeReference<List<Map<String, Object>>>() {
          });
      return data.stream()
          .map(row -> new Object[] { row.get("a"), row.get("b"), row.get("expected") })
          .toArray(Object[][]::new);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load JSON data from " + path, e);
    }
  }
}