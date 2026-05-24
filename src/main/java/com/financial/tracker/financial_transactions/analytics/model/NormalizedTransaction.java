package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "normalized_transactions")
public class NormalizedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Integer transactionId;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionDirection direction;

    @Column(name = "merchant_raw")
    private String merchantRaw;

    @Column(name = "merchant_key")
    private String merchantKey;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "resolved_merchant_id")
    private UUID resolvedMerchantId;

    @Column(name = "is_recurring_generated", nullable = false)
    private boolean recurringGenerated;

    @Column(name = "normalized_at", nullable = false)
    private Instant normalizedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(TransactionDirection direction) {
        this.direction = direction;
    }

    public String getMerchantRaw() {
        return merchantRaw;
    }

    public void setMerchantRaw(String merchantRaw) {
        this.merchantRaw = merchantRaw;
    }

    public String getMerchantKey() {
        return merchantKey;
    }

    public void setMerchantKey(String merchantKey) {
        this.merchantKey = merchantKey;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getResolvedMerchantId() {
        return resolvedMerchantId;
    }

    public void setResolvedMerchantId(UUID resolvedMerchantId) {
        this.resolvedMerchantId = resolvedMerchantId;
    }

    public boolean isRecurringGenerated() {
        return recurringGenerated;
    }

    public void setRecurringGenerated(boolean recurringGenerated) {
        this.recurringGenerated = recurringGenerated;
    }

    public Instant getNormalizedAt() {
        return normalizedAt;
    }

    public void setNormalizedAt(Instant normalizedAt) {
        this.normalizedAt = normalizedAt;
    }
}
