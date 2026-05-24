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
        assertEquals(new java.math.BigDecimal("18.06"), transaction.getAmountValue());
        assertEquals("2025-06-27", transaction.getTransactionDate());
        assertEquals(java.time.LocalDate.of(2025, 6, 27), transaction.getOccurredOn());
        assertEquals("other", transaction.getCardType());
        assertEquals("370 King St W\nToronto ON M5V 1J9\nCanada", transaction.getAddress());
        assertNotNull(transaction.getHash());
    }

    @Test
    void parseSingle_parsesOneBlockWithoutSeparator() {
        String note = """
                Name: Booster Juice
                Merchant: Booster Juice
                Amount: $12.59
                Date: May 18, 2026 at 1:19:33 PM EDT
                Location: 55 Avenue Rd
                Toronto ON M5R 3L2
                Canada
                """;

        Transaction transaction = parser.parseSingle(note);
        assertNotNull(transaction);
        assertEquals("12.59", transaction.getAmount());
        assertEquals("2026-05-18", transaction.getTransactionDate());
    }

    @Test
    void buildFromFields_matchesParsedWalletNote() {
        Transaction fromFields = parser.buildFromFields(
                "Wendys 6758",
                "Wendys 6758",
                "$18.06",
                "June 27, 2025 at 8:37:02 PM EDT",
                "370 King St W\nToronto ON M5V 1J9\nCanada"
        );

        assertNotNull(fromFields);
        assertEquals("Wendys 6758", fromFields.getName());
        assertEquals("18.06", fromFields.getAmount());
        assertEquals("2025-06-27", fromFields.getTransactionDate());
        assertEquals("370 King St W\nToronto ON M5V 1J9\nCanada", fromFields.getAddress());
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
