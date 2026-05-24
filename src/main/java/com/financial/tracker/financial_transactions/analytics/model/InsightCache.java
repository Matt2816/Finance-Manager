package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "insight_cache")
public class InsightCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "insight_type", nullable = false)
    private String insightType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsightSeverity severity;

    @Column(name = "payload_json", length = 4000)
    private String payloadJson;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "refresh_run_id")
    private Long refreshRunId;

    @Column(name = "rank_score")
    private double rankScore;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInsightType() {
        return insightType;
    }

    public void setInsightType(String insightType) {
        this.insightType = insightType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public InsightSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(InsightSeverity severity) {
        this.severity = severity;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getRefreshRunId() {
        return refreshRunId;
    }

    public void setRefreshRunId(Long refreshRunId) {
        this.refreshRunId = refreshRunId;
    }

    public double getRankScore() {
        return rankScore;
    }

    public void setRankScore(double rankScore) {
        this.rankScore = rankScore;
    }
}
