package com.financial.tracker.financial_transactions.util;

import com.financial.tracker.financial_transactions.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionFieldParserTest {

    @Test
    void parseAmountValue_stripsCurrency() {
        assertEquals(new BigDecimal("18.06"), TransactionFieldParser.parseAmountValue("$18.06"));
    }

    @Test
    void parseOccurredOn_walletFormat() {
        LocalDate date = TransactionFieldParser.parseOccurredOn(
                "June 27, 2025 at 8:37:02 PM EDT"
        );
        assertEquals(LocalDate.of(2025, 6, 27), date);
    }

    @Test
    void parseOccurredOn_isoFormat() {
        assertEquals(LocalDate.of(2025, 6, 27), TransactionFieldParser.parseOccurredOn("2025-06-27"));
    }

    @Test
    void applyTypedFields_backfillsFromLegacyStrings() {
        Transaction tx = new Transaction();
        tx.setAmount("12.50");
        tx.setTransactionDate("2025-01-15");
        TransactionFieldParser.applyTypedFields(tx);
        assertEquals(new BigDecimal("12.50"), tx.getAmountValue());
        assertEquals(LocalDate.of(2025, 1, 15), tx.getOccurredOn());
        assertEquals("CAD", tx.getCurrency());
    }

    @Test
    void hashTransaction_usesTypedFields() {
        String hash = TransactionFieldParser.hashTransaction(
                "Shop",
                "Shop",
                new BigDecimal("10.00"),
                LocalDate.of(2025, 1, 1),
                "addr"
        );
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void normalizeAmountString_rejectsInvalid() {
        assertNull(TransactionFieldParser.normalizeAmountString(""));
        assertNull(TransactionFieldParser.normalizeAmountString("abc"));
    }
}
