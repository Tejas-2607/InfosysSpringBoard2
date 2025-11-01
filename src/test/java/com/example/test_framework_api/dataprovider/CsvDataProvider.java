package com.example.test_framework_api.dataprovider;

import com.opencsv.CSVReader;
import org.testng.annotations.DataProvider;

import java.io.FileReader;

/**
 * Reads test data from CSV.
 * First row = headers (a,b,expected)
 */
public class CsvDataProvider {

    @DataProvider(name = "csvData")
    public static Object[][] provideFromCsv() {
        return loadCsv("src/test/resources/test-data.csv");
    }

    private static Object[][] loadCsv(String path) {
        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            // Skip header
            reader.readNext();
            return reader.readAll().stream()
                .map(row -> new Object[]{
                    Integer.parseInt(row[0]), // a
                    Integer.parseInt(row[1]), // b
                    Integer.parseInt(row[2])  // expected
                })
                .toArray(Object[][]::new);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load CSV data from " + path, e);
        }
    }
}