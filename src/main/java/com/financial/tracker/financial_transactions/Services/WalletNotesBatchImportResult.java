package com.financial.tracker.financial_transactions.Services;

import java.util.List;

public record WalletNotesBatchImportResult(
        int total,
        int created,
        int duplicates,
        int skipped,
        List<WalletNotesBatchItemResult> results
) {}
