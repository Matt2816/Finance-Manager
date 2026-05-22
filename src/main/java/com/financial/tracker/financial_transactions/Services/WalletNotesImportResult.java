package com.financial.tracker.financial_transactions.Services;

public record WalletNotesImportResult(
        int imported,
        int skippedNoAmount,
        int skippedDuplicates,
        int totalParsed
) {}
