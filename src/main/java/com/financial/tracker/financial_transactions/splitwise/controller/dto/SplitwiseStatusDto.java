package com.financial.tracker.financial_transactions.splitwise.controller.dto;

import com.financial.tracker.financial_transactions.model.SplitwiseImportState;
import com.financial.tracker.financial_transactions.model.SplitwiseSyncRun;
import java.time.Instant;

public record SplitwiseStatusDto(
        String lastStatus,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        Instant lastPollAt,
        Instant lastSuccessAt,
        String lastError,
        int importedCount,
        int updatedCount,
        int deletedCount,
        int skippedCount
) {
    public static SplitwiseStatusDto empty() {
        return new SplitwiseStatusDto("NONE", null, null, null, null, null, 0, 0, 0, 0);
    }

    public static SplitwiseStatusDto from(SplitwiseSyncRun run, SplitwiseImportState state) {
        if (run == null && state == null) {
            return empty();
        }
        return new SplitwiseStatusDto(
                run != null && run.getStatus() != null ? run.getStatus().name() : "NONE",
                run != null ? run.getStartedAt() : null,
                run != null ? run.getFinishedAt() : null,
                state != null ? state.getLastPollAt() : null,
                state != null ? state.getLastSuccessAt() : null,
                state != null ? state.getLastError() : (run != null ? run.getErrorLog() : null),
                run != null ? run.getImportedCount() : 0,
                run != null ? run.getUpdatedCount() : 0,
                run != null ? run.getDeletedCount() : 0,
                run != null ? run.getSkippedCount() : 0
        );
    }
}
