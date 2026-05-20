package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;

public record WalletNoteImportResult(
        String status,
        String message,
        Transaction transaction
) {
    public static WalletNoteImportResult created(Transaction transaction) {
        return new WalletNoteImportResult("created", "Transaction saved", transaction);
    }

    public static WalletNoteImportResult duplicate(Transaction transaction) {
        return new WalletNoteImportResult("duplicate", "Transaction already exists", transaction);
    }

    public static WalletNoteImportResult skipped(String message) {
        return new WalletNoteImportResult("skipped", message, null);
    }
}
