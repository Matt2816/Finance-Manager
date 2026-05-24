package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.util.TransactionExclusionFilter;
import com.financial.tracker.financial_transactions.util.TransactionFieldParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ExcelStatementParser {

    private static final Logger log = LoggerFactory.getLogger(ExcelStatementParser.class);

    private static final int DATA_START_ROW = 12; // Line 13 (0-indexed)
    private static final int DATE_COL_INDEX = 0; // Column A
    private static final int DESCRIPTION_COL_INDEX = 2; // Column C
    private static final int AMOUNT_COL_INDEX = 3; // Column D

    private final TransactionExclusionFilter exclusionFilter;

    public ExcelStatementParser(TransactionExclusionFilter exclusionFilter) {
        this.exclusionFilter = exclusionFilter;
    }

    public ParseResult parse(InputStream inputStream) throws IOException {
        log.info("Parsing Excel statement");
        List<Transaction> transactions = new ArrayList<>();
        int skippedRows = 0;
        int excludedRows = 0;

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    skippedRows++;
                    continue;
                }

                Transaction transaction = parseRow(row);
                if (transaction != null) {
                    if (exclusionFilter.shouldExclude(transaction)) {
                        excludedRows++;
                    } else {
                        transactions.add(transaction);
                    }
                } else {
                    skippedRows++;
                }
            }
        }

        log.info("Parsed Excel statement: transactions={}, skippedRows={}, excludedRows={}", 
                transactions.size(), skippedRows, excludedRows);
        return new ParseResult(transactions, skippedRows, excludedRows);
    }

    private Transaction parseRow(Row row) {
        String dateStr = getCellValueAsString(row.getCell(DATE_COL_INDEX));
        String description = getCellValueAsString(row.getCell(DESCRIPTION_COL_INDEX));
        String amountStr = getCellValueAsString(row.getCell(AMOUNT_COL_INDEX));

        if (dateStr == null || dateStr.isBlank() || amountStr == null || amountStr.isBlank()) {
            log.info("Skipping row: missing date or amount - date='{}', amount='{}'", dateStr, amountStr);
            return null;
        }

        BigDecimal amountValue = TransactionFieldParser.parseAmountValue(amountStr);
        if (amountValue == null) {
            log.info("Skipping row: invalid amount '{}'", amountStr);
            return null;
        }

        LocalDate occurredOn = TransactionFieldParser.parseOccurredOn(dateStr);
        if (occurredOn == null) {
            log.info("Skipping row: invalid date '{}'", dateStr);
            return null;
        }

        String resolvedName = description == null ? "" : description.trim();
        String resolvedMerchant = resolvedName;

        Transaction transaction = new Transaction();
        transaction.setName(resolvedName);
        transaction.setMerchant(resolvedMerchant);
        transaction.setAmount(amountValue.toPlainString());
        transaction.setAmountValue(amountValue);
        transaction.setOccurredOn(occurredOn);
        transaction.setTransactionDate(TransactionFieldParser.formatTransactionDate(occurredOn));
        transaction.setCurrency(TransactionFieldParser.DEFAULT_CURRENCY);
        transaction.setCardType("other");
        transaction.setAddress("");
        transaction.setRecurringGenerated(false);
        transaction.setHash(TransactionFieldParser.hashTransaction(
                resolvedName,
                resolvedMerchant,
                amountValue,
                occurredOn,
                ""
        ));

        log.info("Parsed transaction: name={}, amount={}, date={}", resolvedName, amountValue, occurredOn);
        return transaction;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> null;
        };
    }

    public record ParseResult(List<Transaction> transactions, int skippedRows, int excludedRows) {}
}
