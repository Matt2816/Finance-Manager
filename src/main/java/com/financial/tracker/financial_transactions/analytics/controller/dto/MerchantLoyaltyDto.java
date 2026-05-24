package com.financial.tracker.financial_transactions.analytics.controller.dto;

import com.financial.tracker.financial_transactions.analytics.model.MerchantLoyaltyMetrics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MerchantLoyaltyDto(
        Long id,
        String merchantKey,
        String canonicalName,
        Long categoryId,
        int totalTransactions,
        BigDecimal totalSpend,
        BigDecimal avgTransactionSize,
        LocalDate firstVisit,
        LocalDate lastVisit,
        Double visitFrequencyDays,
        double loyaltyScore,
        BigDecimal spendGrowthRate,
        LocalDate calculatedAt
) {
    public static MerchantLoyaltyDto from(MerchantLoyaltyMetrics metrics) {
        return new MerchantLoyaltyDto(
                metrics.getId(),
                metrics.getMerchantKey(),
                metrics.getCanonicalName(),
                metrics.getCategoryId(),
                metrics.getTotalTransactions(),
                metrics.getTotalSpend(),
                metrics.getAvgTransactionSize(),
                metrics.getFirstVisit(),
                metrics.getLastVisit(),
                metrics.getVisitFrequencyDays(),
                metrics.getLoyaltyScore(),
                metrics.getSpendGrowthRate(),
                metrics.getCalculatedAt()
        );
    }
}
