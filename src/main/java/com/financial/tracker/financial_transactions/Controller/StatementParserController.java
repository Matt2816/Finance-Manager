package com.financial.tracker.financial_transactions.Controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;  // Import HSSFWorkbook for .xls files
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/statement")
public class StatementParserController {

    @PostMapping("/upload")
    public ResponseEntity<List<List<String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        List<List<String>> transactions = new ArrayList<>();

        try {
            if (fileName.endsWith(".csv")) {
                transactions = parseCSV(file.getInputStream());
            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                transactions = parseExcel(file.getInputStream(), fileName);
            } else if (fileName.endsWith(".pdf")) {
                transactions = parsePDF(file.getInputStream());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            return ResponseEntity.ok(transactions);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private List<List<String>> parseCSV(InputStream inputStream) throws IOException {
        List<List<String>> records = new ArrayList<>();
        Reader in = new InputStreamReader(inputStream);
        Iterable<CSVRecord> csvRecords = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);

        for (CSVRecord record : csvRecords) {
            List<String> row = new ArrayList<>();
            record.forEach(value -> row.add(value));
            records.add(row);
        }

        return records;
    }

    private List<List<String>> parseExcel(InputStream inputStream, String fileName) throws IOException {
        List<List<String>> records = new ArrayList<>();
        Workbook workbook;

        // Determine the file format
        if (fileName.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream); // OOXML format
        } else if (fileName.endsWith(".xls")) {
            workbook = new HSSFWorkbook(inputStream); // OLE2 format
        } else {
            throw new IllegalArgumentException("Invalid Excel file format");
        }

        Sheet sheet = workbook.getSheetAt(0);
        int startRow = 0; // Initialize with -1, meaning not found
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy"); // Adjust the format based on your data

        // Loop through rows to find the starting point
        for (Row row : sheet) {
            // Ensure the row has enough cells to check
            if (row.getPhysicalNumberOfCells() >= 4) {
                Cell firstCell = row.getCell(0);
                Cell fourthCell = row.getCell(3);

                boolean isDate = false;
                boolean isAmountValid = false;

                // Check if the first cell is a date
                if (firstCell != null) {
                    if (firstCell.getCellType() == CellType.STRING) {
                        String firstCellValue = firstCell.getStringCellValue().trim();
                        try {
                            dateFormat.parse(firstCellValue); // Try parsing the date
                            isDate = true;
                        } catch (ParseException e) {
                            // Not a date, continue to the next row
                        }
                    } else if (firstCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(firstCell)) {
                        isDate = true; // Valid date format
                    }
                }

                // Check if the fourth cell contains a numeric value after removing the $
                if (fourthCell != null) {
                    String fourthCellValue = fourthCell.toString().trim();
                    if (fourthCellValue.startsWith("$")) {
                        fourthCellValue = fourthCellValue.substring(1).trim(); // Remove the $ sign
                    }
                    try {
                        Double.parseDouble(fourthCellValue); // Check if it's a number
                        isAmountValid = true;
                    } catch (NumberFormatException e) {
                        // Not a number
                    }
                }

                // If both conditions are met, set startRow
                if (isDate && isAmountValid) {
                    startRow = row.getRowNum(); // Set startRow to the current row index
                    break; // Exit the loop if both conditions are met
                }
            }
        }

        // Now process the rows starting from startRow to skip header or summary rows
        if (startRow != -1) { // Only proceed if a valid starting row was found
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) { // Start from startRow
                Row row = sheet.getRow(i);
                if (row != null) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        String cellValue = cell.toString().trim();

                        // Handle the case for the third index containing "-$"
                        if (cell.getColumnIndex() == 2 && cellValue.startsWith("-$")) {
                            cellValue = cellValue.substring(2).trim(); // Remove "-$"
                            cellValue = "-" + cellValue; // Keep it as a negative number
                        } else if (cellValue.startsWith("$")) {
                            cellValue = cellValue.substring(1).trim(); // Remove the $ sign
                        }
                        rowData.add(cellValue);
                    }
                    // Add row data only if it contains relevant transaction details
                    if (!rowData.isEmpty() && !rowData.get(0).isEmpty()) {
                        records.add(rowData);
                    }
                }
            }
        }

        workbook.close();
        return records;
    }


    private List<List<String>> parsePDF(InputStream inputStream) throws IOException {
        List<List<String>> records = new ArrayList<>();
        PDDocument document = Loader.loadPDF(inputStream.readAllBytes());
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String text = pdfStripper.getText(document);

        // You may want to split text into lines and parse accordingly
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            List<String> rowData = new ArrayList<>();
            rowData.add(line);
            records.add(rowData);
        }

        document.close();
        return records;
    }
}
