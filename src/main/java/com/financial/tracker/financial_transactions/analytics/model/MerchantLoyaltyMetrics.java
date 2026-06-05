package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "merchant_loyalty_metrics")
public class MerchantLoyaltyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "merchant_key", nullable = false)
    private String merchantKey;

    @Column(name = "canonical_name", nullable = false)
    private String canonicalName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "total_transactions", nullable = false)
    private int totalTransactions;

    @Column(name = "total_spend", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSpend;

    @Column(name = "avg_transaction_size", nullable = false, precision = 19, scale = 2)
    private BigDecimal avgTransactionSize;

    @Column(name = "first_visit")
    private LocalDate firstVisit;

    @Column(name = "last_visit")
    private LocalDate lastVisit;

    @Column(name = "visit_frequency_days")
    private Double visitFrequencyDays;

    @Column(name = "loyalty_score", nullable = false)
    private double loyaltyScore;

    @Column(name = "spend_growth_rate", precision = 19, scale = 4)
    private BigDecimal spendGrowthRate;

    @Column(name = "calculated_at", nullable = false)
    private LocalDate calculatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public void setMerchantKey(String merchantKey) {
        this.merchantKey = merchantKey;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(BigDecimal totalSpend) {
        this.totalSpend = totalSpend;
    }

    public BigDecimal getAvgTransactionSize() {
        return avgTransactionSize;
    }

    public void setAvgTransactionSize(BigDecimal avgTransactionSize) {
        this.avgTransactionSize = avgTransactionSize;
    }

    public LocalDate getFirstVisit() {
        return firstVisit;
    }

    public void setFirstVisit(LocalDate firstVisit) {
        this.firstVisit = firstVisit;
    }

    public LocalDate getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(LocalDate lastVisit) {
        this.lastVisit = lastVisit;
    }

    public Double getVisitFrequencyDays() {
        return visitFrequencyDays;
    }

    public void setVisitFrequencyDays(Double visitFrequencyDays) {
        this.visitFrequencyDays = visitFrequencyDays;
    }

    public double getLoyaltyScore() {
        return loyaltyScore;
    }

    public void setLoyaltyScore(double loyaltyScore) {
        this.loyaltyScore = loyaltyScore;
    }

    public BigDecimal getSpendGrowthRate() {
        return spendGrowthRate;
    }

    public void setSpendGrowthRate(BigDecimal spendGrowthRate) {
        this.spendGrowthRate = spendGrowthRate;
    }

    public LocalDate getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDate calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
