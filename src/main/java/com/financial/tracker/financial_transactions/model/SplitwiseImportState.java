package com.financial.tracker.financial_transactions.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

@Entity
@Table(name = "splitwise_import_state")
public class SplitwiseImportState {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "last_poll_at")
    private ZonedDateTime lastPollAt;

    @Column(name = "last_success_at")
    private ZonedDateTime lastSuccessAt;

    @Column(name = "last_error", length = 4000)
    private String lastError;

    @Column(name = "splitwise_user_id")
    private Long splitwiseUserId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ZonedDateTime getLastPollAt() {
        return lastPollAt;
    }

    public void setLastPollAt(ZonedDateTime lastPollAt) {
        this.lastPollAt = lastPollAt;
    }

    public ZonedDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(ZonedDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Long getSplitwiseUserId() {
        return splitwiseUserId;
    }

    public void setSplitwiseUserId(Long splitwiseUserId) {
        this.splitwiseUserId = splitwiseUserId;
    }
}
