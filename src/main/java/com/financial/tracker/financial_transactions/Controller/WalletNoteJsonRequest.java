package com.financial.tracker.financial_transactions.Controller;

public record WalletNoteJsonRequest(
        String name,
        String merchant,
        String amount,
        String date,
        String location
) {}
