package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;

@Entity
@Table(name = "merchant_category_rules")
public class MerchantCategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String pattern;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private int priority;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
