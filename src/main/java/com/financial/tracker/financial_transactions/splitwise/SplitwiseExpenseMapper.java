package com.financial.tracker.financial_transactions.splitwise;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpenseDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseExpenseUserDto;
import com.financial.tracker.financial_transactions.splitwise.client.dto.SplitwiseUserDto;
import com.financial.tracker.financial_transactions.util.TransactionFieldParser;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@Component
public class SplitwiseExpenseMapper {

    public static final String CARD_TYPE = "splitwise";
    public static final String HASH_PREFIX = "splitwise:";

    public String expenseHash(Long expenseId) {
        return HASH_PREFIX + expenseId;
    }

    public BigDecimal netBalanceForUser(SplitwiseExpenseDto expense, Long splitwiseUserId) {
        if (splitwiseUserId == null || expense.getUsers() == null) {
            return null;
        }
        for (SplitwiseExpenseUserDto share : expense.getUsers()) {
            if (splitwiseUserId.equals(share.getUserId())) {
                return parseDecimal(share.getNetBalance());
            }
        }
        return null;
    }

    public boolean isOwedExpense(SplitwiseExpenseDto expense, Long splitwiseUserId) {
        BigDecimal net = netBalanceForUser(expense, splitwiseUserId);
        return net != null && net.compareTo(BigDecimal.ZERO) < 0;
    }

    public Transaction toTransaction(
            SplitwiseExpenseDto expense,
            Long splitwiseUserId,
            Map<Long, String> groupNamesById
    ) {
        BigDecimal net = netBalanceForUser(expense, splitwiseUserId);
        if (net == null) {
            return null;
        }
        BigDecimal amount = net.abs().setScale(2, RoundingMode.HALF_UP);
        String amountStr = amount.toPlainString();

        LocalDate occurredOn = parseExpenseDate(expense.getDate());
        String groupLabel = resolveGroupName(expense.getGroupId(), groupNamesById);
        String merchant = "Splitwise: " + groupLabel;
        String name = expense.getDescription() != null && !expense.getDescription().isBlank()
                ? expense.getDescription().trim()
                : "Splitwise expense";

        Transaction transaction = new Transaction();
        transaction.setCardType(CARD_TYPE);
        transaction.setHash(expenseHash(expense.getId()));
        transaction.setName(name);
        transaction.setMerchant(merchant);
        transaction.setCurrency(
                expense.getCurrencyCode() != null && !expense.getCurrencyCode().isBlank()
                        ? expense.getCurrencyCode()
                        : TransactionFieldParser.DEFAULT_CURRENCY
        );
        transaction.setAddress(creatorName(expense.getCreatedBy()));
        TransactionFieldParser.populateFromRaw(transaction, amountStr, occurredOn);
        return transaction;
    }

    private static String creatorName(SplitwiseUserDto createdBy) {
        if (createdBy == null) {
            return null;
        }
        String name = createdBy.displayName();
        return name.isBlank() ? null : name;
    }

    private static String resolveGroupName(Long groupId, Map<Long, String> groupNamesById) {
        if (groupId == null) {
            return "Personal";
        }
        return groupNamesById.getOrDefault(groupId, "Group " + groupId);
    }

    private static LocalDate parseExpenseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            if (date.length() >= 10) {
                return LocalDate.parse(date.substring(0, 10));
            }
            return Instant.parse(date).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (Exception ex) {
            return TransactionFieldParser.parseOccurredOn(date);
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
