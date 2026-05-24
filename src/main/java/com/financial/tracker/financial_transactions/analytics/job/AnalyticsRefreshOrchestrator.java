package com.financial.tracker.financial_transactions.analytics.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.tracker.financial_transactions.analytics.aggregation.SpendingAggregationService;
import com.financial.tracker.financial_transactions.analytics.insight.InsightEngine;
import com.financial.tracker.financial_transactions.analytics.merchant.MerchantIntelligenceEngine;
import com.financial.tracker.financial_transactions.analytics.merchant.MerchantLoyaltyService;
import com.financial.tracker.financial_transactions.analytics.model.AnalyticsRefreshRun;
import com.financial.tracker.financial_transactions.analytics.model.RefreshRunStatus;
import com.financial.tracker.financial_transactions.analytics.normalization.TransactionBackfillService;
import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.analytics.prediction.PredictionEngine;
import com.financial.tracker.financial_transactions.analytics.repo.AnalyticsRefreshRunRepository;
import com.financial.tracker.financial_transactions.analytics.trend.SpendingTrendAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsRefreshOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRefreshOrchestrator.class);

    private final AnalyticsRefreshRunRepository refreshRunRepository;
    private final TransactionBackfillService backfillService;
    private final TransactionNormalizationService normalizationService;
    private final MerchantIntelligenceEngine merchantEngine;
    private final MerchantLoyaltyService loyaltyService;
    private final SpendingAggregationService aggregationService;
    private final InsightEngine insightEngine;
    private final SpendingTrendAnalysisService trendService;
    private final PredictionEngine predictionEngine;
    private final ObjectMapper objectMapper;

    public AnalyticsRefreshOrchestrator(
            AnalyticsRefreshRunRepository refreshRunRepository,
            TransactionBackfillService backfillService,
            TransactionNormalizationService normalizationService,
            MerchantIntelligenceEngine merchantEngine,
            MerchantLoyaltyService loyaltyService,
            SpendingAggregationService aggregationService,
            InsightEngine insightEngine,
            SpendingTrendAnalysisService trendService,
            PredictionEngine predictionEngine,
            ObjectMapper objectMapper
    ) {
        this.refreshRunRepository = refreshRunRepository;
        this.backfillService = backfillService;
        this.normalizationService = normalizationService;
        this.merchantEngine = merchantEngine;
        this.loyaltyService = loyaltyService;
        this.aggregationService = aggregationService;
        this.insightEngine = insightEngine;
        this.trendService = trendService;
        this.predictionEngine = predictionEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalyticsRefreshRun runRefresh() {
        AnalyticsRefreshRun run = new AnalyticsRefreshRun();
        run.setStatus(RefreshRunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = refreshRunRepository.save(run);

        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
            metadata.put("backfill", backfillService.backfillAll());
            metadata.put("normalization", normalizationService.reconcileAll());
            metadata.put("merchant", merchantEngine.processAll());
            metadata.put("loyalty", loyaltyService.calculateAllLoyaltyMetrics());
            metadata.put("snapshots", aggregationService.rebuildSnapshots());
            metadata.put("insights", insightEngine.generateAll(run.getId()));
            metadata.put("trends", trendService.generateAll(run.getId()));
            metadata.put("forecasts", predictionEngine.generateForecasts(run.getId()));

            run.setStatus(RefreshRunStatus.SUCCESS);
            run.setMetadataJson(objectMapper.writeValueAsString(metadata));
            run.setFinishedAt(Instant.now());
            log.info("Analytics refresh completed: {}", metadata);
        } catch (Exception ex) {
            log.error("Analytics refresh failed", ex);
            run.setStatus(RefreshRunStatus.FAILED);
            run.setErrorLog(ex.getMessage());
            run.setFinishedAt(Instant.now());
        }
        return refreshRunRepository.save(run);
    }
}
