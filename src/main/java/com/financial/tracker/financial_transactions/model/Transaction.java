package com.financial.tracker.financial_transactions.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cardtype")
    private String cardType;
    @Column(name = "amount")
    private String amount;
    @Column(name = "amount_value")
    private BigDecimal amountValue;
    @Column(name = "occurred_on")
    private LocalDate occurredOn;
    @Column(name = "currency")
    private String currency;
    @Column(name = "name")
    private String name;
    @Column(name = "merchant")
    private String merchant;
    @Column(name = "transactiondate")
    private String transactionDate;
    @Column(name = "hash")
    private String hash;
    @Column(name = "address")
    private String address;
    @Column(name = "recurring_generated")
    private boolean recurringGenerated;

    @Column(name = "recurring_parent_id")
    private Integer recurringParentId;

    @Column(name = "category_id")
    private Long categoryId;

    public Transaction(String cardType, String amount, String name, String merchant, String transactionDate, String hash, String address) {
        this.cardType = cardType;
        this.amount = amount;
        this.name = name;
        this.merchant = merchant;
        this.transactionDate = transactionDate;
        this.hash = hash;
        this.address = address;
    }

    public Transaction() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountValue() {
        return amountValue;
    }

    public void setAmountValue(BigDecimal amountValue) {
        this.amountValue = amountValue;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isRecurringGenerated() {
        return recurringGenerated;
    }

    public void setRecurringGenerated(boolean recurringGenerated) {
        this.recurringGenerated = recurringGenerated;
    }

    public Integer getRecurringParentId() {
        return recurringParentId;
    }

    public void setRecurringParentId(Integer recurringParentId) {
        this.recurringParentId = recurringParentId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", cardType='" + cardType + '\'' +
                ", amount='" + amount + '\'' +
                ", amountValue=" + amountValue +
                ", occurredOn=" + occurredOn +
                ", name='" + name + '\'' +
                ", merchant='" + merchant + '\'' +
                ", transactionDate='" + transactionDate + '\'' +
                ", hash='" + hash + '\'' +
                ", address='" + address + '\'' +
                ", recurringGenerated=" + recurringGenerated +
                '}';
    }
}
