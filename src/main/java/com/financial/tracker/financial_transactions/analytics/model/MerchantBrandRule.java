package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;

@Entity
@Table(name = "merchant_brand_rules")
public class MerchantBrandRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String pattern;

    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

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

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
