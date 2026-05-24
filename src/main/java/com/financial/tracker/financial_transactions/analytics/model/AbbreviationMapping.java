package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "abbreviation_mappings")
public class AbbreviationMapping {

    @Id
    @Column(nullable = false)
    private String abbreviation;

    @Column(nullable = false)
    private String expansion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getExpansion() {
        return expansion;
    }

    public void setExpansion(String expansion) {
        this.expansion = expansion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
