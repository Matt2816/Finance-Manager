package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.ExcelStatementImportResult;
import com.financial.tracker.financial_transactions.Services.ExcelStatementImportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.financial.tracker.financial_transactions.config.TestAuthHelper;
import com.financial.tracker.financial_transactions.config.TestSecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.io.ByteArrayOutputStream;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class TransactionControllerExcelImportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExcelStatementImportService excelStatementImportService;

    @BeforeEach
    void setUpAuth() {
        TestAuthHelper.setAuthenticatedUser(1L, "test");
    }

    @AfterEach
    void tearDownAuth() {
        TestAuthHelper.clearAuthentication();
    }

    @Test
    void importExcelStatement_returnsImportResult() throws Exception {
        ExcelStatementImportResult mockResult = new ExcelStatementImportResult(3, 0, 0, 0, 3, List.of());
        when(excelStatementImportService.importFromBytes(any(), anyLong())).thenReturn(mockResult);

        byte[] excelData = createTestExcelFile();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                excelData
        );

        mockMvc.perform(multipart("/api/transaction/import/excel-statement")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(3))
                .andExpect(jsonPath("$.skippedRows").value(0))
                .andExpect(jsonPath("$.excludedRows").value(0))
                .andExpect(jsonPath("$.skippedDuplicates").value(0))
                .andExpect(jsonPath("$.totalParsed").value(3));
    }

    @Test
    void importExcelStatement_withSkippedRows() throws Exception {
        ExcelStatementImportResult mockResult = new ExcelStatementImportResult(2, 1, 0, 0, 3, List.of());
        when(excelStatementImportService.importFromBytes(any(), anyLong())).thenReturn(mockResult);

        byte[] excelData = createTestExcelFile();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                excelData
        );

        mockMvc.perform(multipart("/api/transaction/import/excel-statement")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skippedRows").value(1))
                .andExpect(jsonPath("$.excludedRows").value(0))
                .andExpect(jsonPath("$.skippedDuplicates").value(0))
                .andExpect(jsonPath("$.totalParsed").value(3));
    }

    @Test
    void importExcelStatement_withDuplicates() throws Exception {
        ExcelStatementImportResult mockResult = new ExcelStatementImportResult(1, 0, 0, 2, 3, List.of());
        when(excelStatementImportService.importFromBytes(any(), anyLong())).thenReturn(mockResult);

        byte[] excelData = createTestExcelFile();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                excelData
        );

        mockMvc.perform(multipart("/api/transaction/import/excel-statement")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skippedRows").value(0))
                .andExpect(jsonPath("$.excludedRows").value(0))
                .andExpect(jsonPath("$.skippedDuplicates").value(2))
                .andExpect(jsonPath("$.totalParsed").value(3));
    }

    @Test
    void importExcelStatement_emptyFile_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/transaction/import/excel-statement")
                        .file(file))
                .andExpect(status().isBadRequest());
    }

    private byte[] createTestExcelFile() throws Exception {
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
}
