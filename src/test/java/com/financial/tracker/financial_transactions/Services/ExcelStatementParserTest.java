package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.util.TransactionExclusionFilter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelStatementParserTest {

    private final TransactionExclusionFilter exclusionFilter = new TransactionExclusionFilter("PAYMENT RECEIVED - THANK YOU");
    private final ExcelStatementParser parser = new ExcelStatementParser(exclusionFilter);

    @Test
    void parsesExcelStatement_withValidData() throws IOException {
        byte[] excelData = createTestExcelFile();

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(3, result.transactions().size());
        assertEquals(0, result.skippedRows());
        assertEquals(0, result.excludedRows());
    }

    @Test
    void parsesExcelStatement_correctTransactionData() throws IOException {
        byte[] excelData = createTestExcelFile();

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));
        List<Transaction> transactions = result.transactions();

        Transaction first = transactions.get(0);
        assertEquals("Grocery Store", first.getName());
        assertEquals("Grocery Store", first.getMerchant());
        assertEquals("50.25", first.getAmount());
        assertEquals(new BigDecimal("50.25"), first.getAmountValue());
        assertEquals("2025-01-15", first.getTransactionDate());
        assertEquals(LocalDate.of(2025, 1, 15), first.getOccurredOn());
        assertEquals("CAD", first.getCurrency());
        assertNotNull(first.getHash());

        Transaction second = transactions.get(1);
        assertEquals("Gas Station", second.getName());
        assertEquals("45.00", second.getAmount());
        assertEquals("2025-01-16", second.getTransactionDate());

        Transaction third = transactions.get(2);
        assertEquals("Restaurant", third.getName());
        assertEquals("32.50", third.getAmount());
        assertEquals("2025-01-17", third.getTransactionDate());
    }

    @Test
    void skipsRowsWithMissingData() throws IOException {
        byte[] excelData = createExcelFileWithMissingData();

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals(2, result.skippedRows());
        assertEquals(0, result.excludedRows());
    }

    @Test
    void skipsRowsWithInvalidAmount() throws IOException {
        byte[] excelData = createExcelFileWithInvalidAmount();

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals(1, result.skippedRows());
        assertEquals(0, result.excludedRows());
    }

    @Test
    void skipsRowsWithInvalidDate() throws IOException {
        byte[] excelData = createExcelFileWithInvalidDate();

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals(1, result.skippedRows());
        assertEquals(0, result.excludedRows());
    }

    @Test
    void parsesExcelDateFormats_noPeriod() throws IOException {
        byte[] excelData = createExcelFileWithDateFormats("21 May 2026");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals(0, result.excludedRows());
        assertEquals(LocalDate.of(2026, 5, 21), result.transactions().get(0).getOccurredOn());
    }

    @Test
    void parsesExcelDateFormats_withPeriod() throws IOException {
        byte[] excelData = createExcelFileWithDateFormats("17 Apr. 2026");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals(0, result.excludedRows());
        assertEquals(LocalDate.of(2026, 4, 17), result.transactions().get(0).getOccurredOn());
    }

    @Test
    void parsesExcelDateFormats_monthOnlyWithPeriod() throws IOException {
        byte[] excelData = createExcelFileWithDateFormats("Feb. 2026");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals(0, result.excludedRows());
        assertEquals(LocalDate.of(2026, 2, 1), result.transactions().get(0).getOccurredOn());
    }

    @Test
    void parsesAmountWithDollarSign() throws IOException {
        byte[] excelData = createExcelFileWithAmountFormat("$21.11");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals("21.11", result.transactions().get(0).getAmount());
        assertEquals(new BigDecimal("21.11"), result.transactions().get(0).getAmountValue());
    }

    @Test
    void parsesAmountWithoutDollarSign() throws IOException {
        byte[] excelData = createExcelFileWithAmountFormat("21.11");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(1, result.transactions().size());
        assertEquals("21.11", result.transactions().get(0).getAmount());
        assertEquals(new BigDecimal("21.11"), result.transactions().get(0).getAmountValue());
    }

    @Test
    void excludesTransactionsMatchingPattern() throws IOException {
        byte[] excelData = createExcelFileWithDescription("PAYMENT RECEIVED - THANK YOU");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(0, result.transactions().size());
        assertEquals(1, result.excludedRows());
    }

    @Test
    void excludesTransactionsWithPartialMatch() throws IOException {
        byte[] excelData = createExcelFileWithDescription("SOME PAYMENT RECEIVED - THANK YOU HERE");

        ExcelStatementParser.ParseResult result = parser.parse(new ByteArrayInputStream(excelData));

        assertEquals(0, result.transactions().size());
        assertEquals(1, result.excludedRows());
    }

    private byte[] createTestExcelFile() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows (headers at line 12 = index 11)
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row at line 12 (index 11)
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Add data rows starting at line 13 (index 12)
            Row row1 = sheet.createRow(12);
            row1.createCell(0).setCellValue("2025-01-15");
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue("Grocery Store");
            row1.createCell(3).setCellValue(50.25);

            Row row2 = sheet.createRow(13);
            row2.createCell(0).setCellValue("2025-01-16");
            row2.createCell(1).setCellValue("REF002");
            row2.createCell(2).setCellValue("Gas Station");
            row2.createCell(3).setCellValue(45.00);

            Row row3 = sheet.createRow(14);
            row3.createCell(0).setCellValue("2025-01-17");
            row3.createCell(1).setCellValue("REF003");
            row3.createCell(2).setCellValue("Restaurant");
            row3.createCell(3).setCellValue(32.50);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createExcelFileWithMissingData() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Row with missing date
            Row row1 = sheet.createRow(12);
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue("Missing Date");
            row1.createCell(3).setCellValue(10.00);

            // Row with missing amount
            Row row2 = sheet.createRow(13);
            row2.createCell(0).setCellValue("2025-01-16");
            row2.createCell(1).setCellValue("REF002");
            row2.createCell(2).setCellValue("Missing Amount");

            // Valid row
            Row row3 = sheet.createRow(14);
            row3.createCell(0).setCellValue("2025-01-17");
            row3.createCell(1).setCellValue("REF003");
            row3.createCell(2).setCellValue("Valid Transaction");
            row3.createCell(3).setCellValue(20.00);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createExcelFileWithInvalidAmount() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Row with invalid amount (text)
            Row row1 = sheet.createRow(12);
            row1.createCell(0).setCellValue("2025-01-15");
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue("Invalid Amount");
            row1.createCell(3).setCellValue("N/A");

            // Valid row
            Row row2 = sheet.createRow(13);
            row2.createCell(0).setCellValue("2025-01-16");
            row2.createCell(1).setCellValue("REF002");
            row2.createCell(2).setCellValue("Valid Transaction");
            row2.createCell(3).setCellValue(15.00);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createExcelFileWithInvalidDate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Row with invalid date
            Row row1 = sheet.createRow(12);
            row1.createCell(0).setCellValue("Invalid Date");
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue("Invalid Date Transaction");
            row1.createCell(3).setCellValue(25.00);

            // Valid row
            Row row2 = sheet.createRow(13);
            row2.createCell(0).setCellValue("2025-01-16");
            row2.createCell(1).setCellValue("REF002");
            row2.createCell(2).setCellValue("Valid Transaction");
            row2.createCell(3).setCellValue(30.00);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createExcelFileWithDateFormats(String dateValue) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Row with specific date format
            Row row1 = sheet.createRow(12);
            row1.createCell(0).setCellValue(dateValue);
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue("Test Transaction");
            row1.createCell(3).setCellValue(25.00);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createExcelFileWithAmountFormat(String amountValue) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Row with specific amount format
            Row row1 = sheet.createRow(12);
            row1.createCell(0).setCellValue("2025-01-15");
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue("Test Transaction");
            row1.createCell(3).setCellValue(amountValue);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createExcelFileWithDescription(String description) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");

            // Create 12 empty rows
            for (int i = 0; i < 12; i++) {
                sheet.createRow(i);
            }

            // Add header row
            Row headerRow = sheet.createRow(11);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Reference");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Amount");

            // Row with specific description
            Row row1 = sheet.createRow(12);
            row1.createCell(0).setCellValue("2025-01-15");
            row1.createCell(1).setCellValue("REF001");
            row1.createCell(2).setCellValue(description);
            row1.createCell(3).setCellValue(25.00);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
