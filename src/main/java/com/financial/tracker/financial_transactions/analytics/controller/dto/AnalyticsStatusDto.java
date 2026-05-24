package com.financial.tracker.financial_transactions.analytics.controller.dto;

import com.financial.tracker.financial_transactions.analytics.model.AnalyticsRefreshRun;

import java.time.Instant;

public record AnalyticsStatusDto(
        String lastStatus,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        String lastError,
        String lastMetadataJson
) {
    public static AnalyticsStatusDto from(AnalyticsRefreshRun run) {
        if (run == null) {
            return new AnalyticsStatusDto("NONE", null, null, null, null);
        }
        return new AnalyticsStatusDto(
                run.getStatus().name(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorLog(),
                run.getMetadataJson()
        );
    }
}
