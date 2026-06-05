package com.financial.tracker.financial_transactions.splitwise;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpenseDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpenseUserDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitwiseExpenseMapperTest {

    private SplitwiseExpenseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SplitwiseExpenseMapper();
    }

    @Test
    void isOwedExpenseWhenNetBalanceNegative() {
        SplitwiseExpenseDto expense = expenseWithNetBalance(42L, "-12.50");
        assertTrue(mapper.isOwedExpense(expense, 42L));
    }

    @Test
    void isNotOwedExpenseWhenNetBalancePositive() {
        SplitwiseExpenseDto expense = expenseWithNetBalance(42L, "5.00");
        assertFalse(mapper.isOwedExpense(expense, 42L));
    }

    @Test
    void mapsExpenseToTransaction() {
        SplitwiseExpenseDto expense = expenseWithNetBalance(99L, "-20.00");
        expense.setId(1001L);
        expense.setDescription("Dinner");
        expense.setDate("2024-06-15T18:00:00Z");
        expense.setCurrencyCode("CAD");
        expense.setGroupId(5L);

        SplitwiseUserDto creator = new SplitwiseUserDto();
        creator.setFirstName("Ada");
        creator.setLastName("Lovelace");
        expense.setCreatedBy(creator);

        Transaction tx = mapper.toTransaction(expense, 99L, Map.of(5L, "Roommates"));

        assertNotNull(tx);
        assertEquals("splitwise:1001", tx.getHash());
        assertEquals("Dinner", tx.getName());
        assertEquals("Splitwise: Roommates", tx.getMerchant());
        assertEquals("20.00", tx.getAmount());
        assertEquals(new BigDecimal("20.00"), tx.getAmountValue());
        assertEquals("CAD", tx.getCurrency());
        assertEquals("splitwise", tx.getCardType());
        assertEquals("Ada Lovelace", tx.getAddress());
    }

    private static SplitwiseExpenseDto expenseWithNetBalance(Long userId, String netBalance) {
        SplitwiseExpenseDto expense = new SplitwiseExpenseDto();
        SplitwiseExpenseUserDto share = new SplitwiseExpenseUserDto();
        share.setUserId(userId);
        share.setNetBalance(netBalance);
        expense.setUsers(List.of(share));
        return expense;
    }
}
