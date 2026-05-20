package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Transaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WalletNotesParserTest {

    private final WalletNotesParser parser = new WalletNotesParser();

    @Test
    void parsesWalletNoteBlock() {
        String block = """
                Name: Wendys 6758
                Merchant: Wendys 6758
                Amount: $18.06
                Date: June 27, 2025 at 8:37:02\u202fPM EDT
                Location: 370 King St W
                Toronto ON M5V 1J9
                Canada
                """;

        WalletNotesParser.ParseResult result = parser.parse(block);
        assertEquals(1, result.transactions().size());
        assertEquals(0, result.skippedBlocks());

        Transaction transaction = result.transactions().get(0);
        assertEquals("Wendys 6758", transaction.getName());
        assertEquals("Wendys 6758", transaction.getMerchant());
        assertEquals("18.06", transaction.getAmount());
        assertEquals("2025-06-27", transaction.getTransactionDate());
        assertEquals("other", transaction.getCardType());
        assertEquals("370 King St W\nToronto ON M5V 1J9\nCanada", transaction.getAddress());
        assertNotNull(transaction.getHash());
    }

    @Test
    void skipsBlocksWithoutAmount() {
        String block = """
                Name: American Express Cobalt® Card
                Merchant:
                Amount:
                Date: May 16, 2026 at 1:03:11 PM EDT
                Location: Broadview
                Toronto ON M4K 1N1
                Canada
                """;

        WalletNotesParser.ParseResult result = parser.parse(block);
        assertEquals(0, result.transactions().size());
        assertEquals(1, result.skippedBlocks());
    }
}
