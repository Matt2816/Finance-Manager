package com.financial.tracker.financial_transactions.splitwise.controller.dto;

import java.util.List;

public record SplitwiseSyncResultDto(
        String status,
        String message,
        int importedCount,
        int updatedCount,
        int deletedCount,
        int skippedCount,
        List<TransactionSummaryDto> imported,
        List<TransactionSummaryDto> updated,
        List<TransactionSummaryDto> deleted,
        List<TransactionSummaryDto> skipped
) {
    public record TransactionSummaryDto(String description, String amount, String date, String groupName, String reason) {}
}
