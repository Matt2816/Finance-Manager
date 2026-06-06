package com.financial.tracker.financial_transactions.analytics.trend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.tracker.financial_transactions.analytics.model.InsightCache;
import com.financial.tracker.financial_transactions.analytics.model.InsightSeverity;
import com.financial.tracker.financial_transactions.analytics.model.SnapshotGrain;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.model.SpendingSnapshot;
import com.financial.tracker.financial_transactions.analytics.repo.InsightCacheRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingSnapshotRepository;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpendingTrendAnalysisService {

    private final InsightCacheRepository insightCacheRepository;
    private final NormalizedTransactionRepository normalizedRepository;
    private final SpendingSnapshotRepository snapshotRepository;
    private final SpendingCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public SpendingTrendAnalysisService(
            InsightCacheRepository insightCacheRepository,
            NormalizedTransactionRepository normalizedRepository,
            SpendingSnapshotRepository snapshotRepository,
            SpendingCategoryRepository categoryRepository,
            ObjectMapper objectMapper
    ) {
        this.insightCacheRepository = insightCacheRepository;
        this.normalizedRepository = normalizedRepository;
        this.snapshotRepository = snapshotRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int generateAll(Long refreshRunId, Long userId) {
        List<InsightCache> trends = new ArrayList<>();
        trends.add(movingAverages(refreshRunId, userId));
        trends.add(categoryShare(refreshRunId, userId));
        trends.add(velocity(refreshRunId, userId));
        trends.addAll(seasonality(refreshRunId, userId));
        insightCacheRepository.saveAll(trends);
        return trends.size();
    }

    private InsightCache movingAverages(Long refreshRunId, Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(60);
        List<Object[]> daily = normalizedRepository.sumByDayBetween(userId, start, end);
        BigDecimal last7 = sumLastDays(daily, 7);
        BigDecimal last30 = sumLastDays(daily, 30);
        String body = String.format(
                "7-day spend total: $%s. 30-day spend total: $%s.",
                last7.setScale(2, RoundingMode.HALF_UP),
                last30.setScale(2, RoundingMode.HALF_UP)
        );
        return buildTrend(
                "TREND_MOVING_AVERAGE",
                "Moving averages",
                body,
                Map.of("total7Day", last7, "total30Day", last30),
                last30.doubleValue(),
                refreshRunId,
                userId
        );
    }

    private BigDecimal sumLastDays(List<Object[]> daily, int days) {
        return daily.stream()
                .sorted(Comparator.comparing(row -> (LocalDate) row[0], Comparator.reverseOrder()))
                .limit(days)
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InsightCache categoryShare(Long refreshRunId, Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(3);
        Map<String, BigDecimal> byCategory = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : normalizedRepository.sumByCategoryBetween(userId, start, end)) {
            Long categoryId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (categoryId == null) {
                continue;
            }
            String slug = categoryRepository.findByIdAndUserId(categoryId, userId)
                    .map(SpendingCategory::getSlug)
                    .orElse("unknown");
            byCategory.merge(slug, amount, BigDecimal::add);
            total = total.add(amount);
        }
        Map<String, Double> shares = new HashMap<>();
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> e : byCategory.entrySet()) {
                shares.put(e.getKey(), e.getValue()
                        .divide(total, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue());
            }
        }
        return buildTrend(
                "TREND_CATEGORY_SHARE",
                "Category share (3 months)",
                "Spending share by category over the last 3 months.",
                Map.of("sharesPercent", shares),
                total.doubleValue(),
                refreshRunId,
                userId
        );
    }

    private InsightCache velocity(Long refreshRunId, Long userId) {
        List<Double> monthly = snapshotRepository.findByUserId(userId).stream()
                .filter(s -> s.getGrain() == SnapshotGrain.MONTHLY && s.getCategoryId() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getPeriodStart().withDayOfMonth(1),
                        Collectors.reducing(BigDecimal.ZERO, SpendingSnapshot::getTotalAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue().doubleValue())
                .collect(Collectors.toList());

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < monthly.size(); i++) {
            regression.addData(i, monthly.get(i));
        }
        double slope = monthly.size() >= 2 ? regression.getSlope() : 0;
        String body = slope >= 0
                ? String.format("Monthly spend trend is increasing by about $%.2f per month.", slope)
                : String.format("Monthly spend trend is decreasing by about $%.2f per month.", Math.abs(slope));

        return buildTrend(
                "TREND_VELOCITY",
                "Spend velocity",
                body,
                Map.of("monthlySlope", slope, "monthsAnalyzed", monthly.size()),
                Math.abs(slope),
                refreshRunId,
                userId
        );
    }

    private List<InsightCache> seasonality(Long refreshRunId, Long userId) {
        List<InsightCache> result = new ArrayList<>();
        for (SpendingCategory category : categoryRepository.findByUserId(userId)) {
            if ("uncategorized".equals(category.getSlug())) {
                continue;
            }
            List<SpendingSnapshot> monthly = snapshotRepository.findByUserIdAndGrainAndCategoryId(
                    userId, SnapshotGrain.MONTHLY, category.getId());
            if (monthly.size() < 12) {
                continue;
            }
            double[] byMonth = new double[13];
            int[] counts = new int[13];
            for (SpendingSnapshot s : monthly) {
                int m = s.getPeriodStart().getMonthValue();
                byMonth[m] += s.getTotalAmount().doubleValue();
                counts[m]++;
            }
            double overall = Arrays.stream(byMonth).sum() / Math.max(1, Arrays.stream(counts).sum());
            if (overall <= 0) {
                continue;
            }
            List<Integer> highSeason = new ArrayList<>();
            Map<String, Double> indices = new HashMap<>();
            for (int m = 1; m <= 12; m++) {
                if (counts[m] == 0) {
                    continue;
                }
                double index = (byMonth[m] / counts[m]) / overall;
                indices.put(String.valueOf(m), index);
                if (index > 1.15) {
                    highSeason.add(m);
                }
            }
            if (highSeason.isEmpty()) {
                continue;
            }
            result.add(buildTrend(
                    "TREND_SEASONALITY",
                    category.getDisplayName() + " seasonality",
                    "High-spend months detected for " + category.getDisplayName() + ".",
                    Map.of("categorySlug", category.getSlug(), "seasonalIndices", indices, "highSeasonMonths", highSeason),
                    50,
                    refreshRunId,
                    userId
            ));
        }
        return result;
    }

    private InsightCache buildTrend(
            String type,
            String title,
            String body,
            Map<String, Object> payload,
            double rankScore,
            Long refreshRunId,
            Long userId
    ) {
        InsightCache insight = new InsightCache();
        insight.setUserId(userId);
        insight.setInsightType(type);
        insight.setTitle(title);
        insight.setBody(body);
        insight.setSeverity(InsightSeverity.info);
        try {
            insight.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            insight.setPayloadJson("{}");
        }
        LocalDate today = LocalDate.now();
        insight.setValidFrom(today);
        insight.setValidTo(today.plusMonths(1));
        insight.setGeneratedAt(ZonedDateTime.now());
        insight.setRefreshRunId(refreshRunId);
        insight.setRankScore(rankScore);
        return insight;
    }
}
