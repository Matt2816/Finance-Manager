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
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsRefreshOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRefreshOrchestrator.class);

    private final AnalyticsRefreshRunRepository refreshRunRepository;
    private final UserRepository userRepository;
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
            UserRepository userRepository,
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
        this.userRepository = userRepository;
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

    public List<AnalyticsRefreshRun> runRefresh() {
        List<AnalyticsRefreshRun> runs = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            runs.add(runRefresh(user.getId()));
        }
        return runs;
    }

    @Transactional
    public AnalyticsRefreshRun runRefresh(Long userId) {
        AnalyticsRefreshRun run = new AnalyticsRefreshRun();
        run.setUserId(userId);
        run.setStatus(RefreshRunStatus.RUNNING);
        run.setStartedAt(ZonedDateTime.now());
        run = refreshRunRepository.save(run);

        Map<String, Object> metadata = new LinkedHashMap<>();
        try {
            metadata.put("backfill", backfillService.backfillAll(userId));
            metadata.put("normalization", normalizationService.reconcileAll(userId));
            metadata.put("merchant", merchantEngine.processAll(userId));
            metadata.put("loyalty", loyaltyService.calculateAllLoyaltyMetrics(userId));
            metadata.put("snapshots", aggregationService.rebuildSnapshots(userId));
            metadata.put("insights", insightEngine.generateAll(run.getId(), userId));
            metadata.put("trends", trendService.generateAll(run.getId(), userId));
            metadata.put("forecasts", predictionEngine.generateForecasts(run.getId(), userId));

            run.setStatus(RefreshRunStatus.SUCCESS);
            run.setMetadataJson(objectMapper.writeValueAsString(metadata));
            run.setFinishedAt(ZonedDateTime.now());
            log.info("Analytics refresh completed for user {}: {}", userId, metadata);
        } catch (Exception ex) {
            log.error("Analytics refresh failed for user {}", userId, ex);
            run.setStatus(RefreshRunStatus.FAILED);
            run.setErrorLog(ex.getMessage());
            run.setFinishedAt(ZonedDateTime.now());
        }
        return refreshRunRepository.save(run);
    }
}
