package com.financial.tracker.financial_transactions.analytics.controller.dto;

import java.math.BigDecimal;

public record CategorySpendingDto(
    Long categoryId,
    String categoryName,
    BigDecimal totalAmount,
    long transactionCount
) {
    public static CategorySpendingDto from(Long categoryId, String categoryName, BigDecimal totalAmount, long transactionCount) {
        return new CategorySpendingDto(categoryId, categoryName, totalAmount, transactionCount);
    }
}
