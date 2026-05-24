package com.financial.tracker.financial_transactions.Services;

public record ExcelStatementImportResult(
        int imported,
        int skippedRows,
        int excludedRows,
        int skippedDuplicates,
        int totalParsed
) {
    @Override
    public String toString() {
        return "ExcelStatementImportResult{" +
                "imported=" + imported +
                ", skippedRows=" + skippedRows +
                ", excludedRows=" + excludedRows +
                ", skippedDuplicates=" + skippedDuplicates +
                ", totalParsed=" + totalParsed +
                '}';
    }
}
