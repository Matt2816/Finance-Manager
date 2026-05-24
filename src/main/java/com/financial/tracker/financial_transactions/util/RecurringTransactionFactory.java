package com.financial.tracker.financial_transactions.util;

import com.financial.tracker.financial_transactions.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class RecurringTransactionFactory {

    private RecurringTransactionFactory() {
    }

    public static Transaction createExpense(String merchant, double amount, LocalDate occurredOn) {
        Transaction transaction = new Transaction();
        transaction.setMerchant(merchant);
        transaction.setName(merchant);
        BigDecimal amountValue = BigDecimal.valueOf(amount);
        TransactionFieldParser.populateFromRaw(transaction, amountValue.toPlainString(), occurredOn);
        transaction.setCardType("other");
        transaction.setRecurringGenerated(true);
        transaction.setHash(TransactionFieldParser.hashTransaction(
                transaction.getName(),
                transaction.getMerchant(),
                transaction.getAmountValue(),
                transaction.getOccurredOn(),
                ""
        ));
        return transaction;
    }

    public static Transaction createIncome(String source, double amount, LocalDate occurredOn) {
        Transaction transaction = createExpense(source, amount, occurredOn);
        transaction.setName(source);
        return transaction;
    }
}
