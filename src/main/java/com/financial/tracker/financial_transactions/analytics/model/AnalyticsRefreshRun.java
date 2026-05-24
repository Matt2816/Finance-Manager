package com.financial.tracker.financial_transactions.analytics.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "analytics_refresh_runs")
public class AnalyticsRefreshRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefreshRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_log", length = 4000)
    private String errorLog;

    @Column(name = "metadata_json", length = 4000)
    private String metadataJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RefreshRunStatus getStatus() {
        return status;
    }

    public void setStatus(RefreshRunStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
