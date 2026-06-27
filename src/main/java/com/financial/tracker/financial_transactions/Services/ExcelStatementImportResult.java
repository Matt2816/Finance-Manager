package com.financial.tracker.financial_transactions.Services;

import java.util.List;

public record ExcelStatementImportResult(
        int imported,
        int skippedRows,
        int excludedRows,
        int skippedDuplicates,
        int totalParsed,
        List<ImportedTransaction> importedTransactions
) {
    public record ImportedTransaction(
            String name,
            String merchant,
            String amount,
            String transactionDate
    ) {}

    @Override
    public String toString() {
        return "ExcelStatementImportResult{" +
                "imported=" + imported +
                ", skippedRows=" + skippedRows +
                ", excludedRows=" + excludedRows +
                ", skippedDuplicates=" + skippedDuplicates +
                ", totalParsed=" + totalParsed +
                ", importedTransactions=" + (importedTransactions == null ? 0 : importedTransactions.size()) +
                '}';
    }
}
