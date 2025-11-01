package com.example.test_framework_api.dataprovider;

import org.apache.poi.ss.usermodel.*;
import org.testng.annotations.DataProvider;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads test data from Excel (.xlsx)
 * Sheet: "Sheet1", First row = headers
 */
public class ExcelDataProvider {

    @DataProvider(name = "excelData")
    public static Object[][] provideFromExcel() {
        return loadExcel("src/test/resources/test-data.xlsx");
    }

    private static Object[][] loadExcel(String path) {
        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(path))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Object[]> data = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header
                Row row = sheet.getRow(i);
                if (row == null) continue;
                data.add(new Object[]{
                    (int) row.getCell(0).getNumericCellValue(),
                    (int) row.getCell(1).getNumericCellValue(),
                    (int) row.getCell(2).getNumericCellValue()
                });
            }
            return data.toArray(new Object[0][]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Excel data from " + path, e);
        }
    }
}