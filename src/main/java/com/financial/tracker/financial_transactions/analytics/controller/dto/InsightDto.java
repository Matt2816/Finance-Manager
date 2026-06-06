package com.financial.tracker.financial_transactions.analytics.controller.dto;

import com.financial.tracker.financial_transactions.analytics.model.InsightCache;

import java.time.ZonedDateTime;
import java.time.LocalDate;

public record InsightDto(
        Long id,
        String insightType,
        String title,
        String body,
        String severity,
        String payloadJson,
        LocalDate validFrom,
        LocalDate validTo,
        ZonedDateTime generatedAt,
        double rankScore
) {
    public static InsightDto from(InsightCache insight) {
        return new InsightDto(
                insight.getId(),
                insight.getInsightType(),
                insight.getTitle(),
                insight.getBody(),
                insight.getSeverity().name(),
                insight.getPayloadJson(),
                insight.getValidFrom(),
                insight.getValidTo(),
                insight.getGeneratedAt(),
                insight.getRankScore()
        );
    }
}
