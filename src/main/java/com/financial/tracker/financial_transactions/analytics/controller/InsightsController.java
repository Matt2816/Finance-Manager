package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.controller.dto.InsightDto;
import com.financial.tracker.financial_transactions.analytics.model.InsightCache;
import com.financial.tracker.financial_transactions.analytics.repo.InsightCacheRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightCacheRepository insightCacheRepository;

    @Value("${analytics.insights.max-results:20}")
    private int maxResults;

    public InsightsController(InsightCacheRepository insightCacheRepository) {
        this.insightCacheRepository = insightCacheRepository;
    }

    @GetMapping
    public List<InsightDto> listInsights(@RequestParam(required = false) String type) {
        LocalDate today = LocalDate.now();
        List<InsightCache> insights;
        if (type != null && !type.isBlank()) {
            insights = insightCacheRepository.findByInsightTypeAndValidToGreaterThanEqualOrderByRankScoreDesc(
                    type, today);
        } else {
            insights = insightCacheRepository.findByValidToGreaterThanEqualOrderByRankScoreDesc(today).stream()
                    .filter(i -> !i.getInsightType().startsWith("TREND_"))
                    .toList();
        }
        return insights.stream()
                .limit(maxResults)
                .map(InsightDto::from)
                .toList();
    }

    @GetMapping("/trends")
    public List<InsightDto> listTrends() {
        LocalDate today = LocalDate.now();
        return insightCacheRepository
                .findByInsightTypeStartingWithAndValidToGreaterThanEqualOrderByRankScoreDesc("TREND_", today)
                .stream()
                .limit(maxResults)
                .map(InsightDto::from)
                .toList();
    }
}
