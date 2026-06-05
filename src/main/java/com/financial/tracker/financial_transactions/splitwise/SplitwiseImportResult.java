package com.financial.tracker.financial_transactions.splitwise;

public record SplitwiseImportResult(
        int imported,
        int updated,
        int deleted,
        int skipped
) {
    public static final SplitwiseImportResult EMPTY = new SplitwiseImportResult(0, 0, 0, 0);
}
