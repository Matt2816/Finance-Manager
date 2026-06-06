package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "spending_forecasts")
public class SpendingForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForecastScope scope;

    @Column(name = "scope_key")
    private String scopeKey;

    @Column(name = "predicted_amount", nullable = false)
    private BigDecimal predictedAmount;

    @Column(name = "lower_bound", nullable = false)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", nullable = false)
    private BigDecimal upperBound;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "generated_at", nullable = false)
    private ZonedDateTime generatedAt;

    @Column(name = "refresh_run_id")
    private Long refreshRunId;

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

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

    public ForecastScope getScope() {
        return scope;
    }

    public void setScope(ForecastScope scope) {
        this.scope = scope;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public BigDecimal getPredictedAmount() {
        return predictedAmount;
    }

    public void setPredictedAmount(BigDecimal predictedAmount) {
        this.predictedAmount = predictedAmount;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public ZonedDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(ZonedDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getRefreshRunId() {
        return refreshRunId;
    }

    public void setRefreshRunId(Long refreshRunId) {
        this.refreshRunId = refreshRunId;
    }
}
