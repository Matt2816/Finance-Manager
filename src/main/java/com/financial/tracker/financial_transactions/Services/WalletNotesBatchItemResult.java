package com.financial.tracker.financial_transactions.Services;

public record WalletNotesBatchItemResult(
        int index,
        String status,
        String message,
        String hash
) {}
