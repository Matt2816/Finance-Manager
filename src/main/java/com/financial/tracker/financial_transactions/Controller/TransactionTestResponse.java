package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.model.Transaction;

public record TransactionTestResponse(
        String status,
        String message,
        String method,
        String clientAddress,
        int bodyLength,
        boolean parseable,
        Transaction preview,
        String bodyPreview,
        WalletNoteTestHints hints
) {
    public record WalletNoteTestHints(
            String postUrl,
            String contentType,
            String exampleBody
    ) {}

    public static TransactionTestResponse getHints(String baseUrl) {
        String postUrl = baseUrl + "/api/transaction/import/wallet-note";
        String example = """
                Name: Test Merchant
                Merchant: Test Merchant
                Amount: $1.00
                Date: January 1, 2026 at 12:00:00 PM EDT
                Location: 1 Main St
                Toronto ON M5V 1A1
                Canada
                """.trim();

        return new TransactionTestResponse(
                "ok",
                "API is reachable. Use POST on this path to test sending a note from your phone.",
                "GET",
                null,
                0,
                false,
                null,
                null,
                new WalletNoteTestHints(postUrl, "text/plain", example)
        );
    }

    public static TransactionTestResponse postResult(
            String clientAddress,
            String body,
            Transaction parsed,
            boolean saved,
            WalletNoteTestHints hints
    ) {
        String previewText = body == null ? "" : body.trim();
        String bodyPreview = previewText.length() > 200
                ? previewText.substring(0, 200) + "…"
                : previewText;

        if (saved) {
            return new TransactionTestResponse(
                    "created",
                    "Test note received and saved to the database.",
                    "POST",
                    clientAddress,
                    body == null ? 0 : body.length(),
                    true,
                    parsed,
                    bodyPreview,
                    hints
            );
        }

        if (parsed != null) {
            return new TransactionTestResponse(
                    "ok",
                    "Test note received and parsed successfully (not saved). Add ?save=true to save.",
                    "POST",
                    clientAddress,
                    body.length(),
                    true,
                    parsed,
                    bodyPreview,
                    hints
            );
        }

        return new TransactionTestResponse(
                "ok",
                body == null || body.isBlank()
                        ? "Connected, but body was empty. Send your Wallet note as text/plain."
                        : "Connected, but note could not be parsed (check Amount and Date fields).",
                "POST",
                clientAddress,
                body == null ? 0 : body.length(),
                false,
                null,
                bodyPreview,
                hints
        );
    }
}
