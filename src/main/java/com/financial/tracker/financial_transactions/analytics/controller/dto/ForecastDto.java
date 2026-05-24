package com.financial.tracker.financial_transactions.analytics.controller.dto;

import com.financial.tracker.financial_transactions.analytics.model.SpendingForecast;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ForecastDto(
        Long id,
        LocalDate forecastDate,
        String scope,
        String scopeKey,
        BigDecimal predictedAmount,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        String method,
        double confidence,
        Instant generatedAt
) {
    public static ForecastDto from(SpendingForecast forecast) {
        return new ForecastDto(
                forecast.getId(),
                forecast.getForecastDate(),
                forecast.getScope().name(),
                forecast.getScopeKey(),
                forecast.getPredictedAmount(),
                forecast.getLowerBound(),
                forecast.getUpperBound(),
                forecast.getMethod(),
                forecast.getConfidence(),
                forecast.getGeneratedAt()
        );
    }
}
