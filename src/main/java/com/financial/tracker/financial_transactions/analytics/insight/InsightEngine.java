package com.financial.tracker.financial_transactions.analytics.insight;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InsightEngine {

    private final InsightCacheRepository insightCacheRepository;
    private final SpendingSnapshotRepository snapshotRepository;
    private final SpendingCategoryRepository categoryRepository;
    private final NormalizedTransactionRepository normalizedRepository;
    private final ObjectMapper objectMapper;

    @Value("${analytics.insights.max-results:20}")
    private int maxResults;

    public InsightEngine(
            InsightCacheRepository insightCacheRepository,
            SpendingSnapshotRepository snapshotRepository,
            SpendingCategoryRepository categoryRepository,
            NormalizedTransactionRepository normalizedRepository,
            ObjectMapper objectMapper
    ) {
        this.insightCacheRepository = insightCacheRepository;
        this.snapshotRepository = snapshotRepository;
        this.categoryRepository = categoryRepository;
        this.normalizedRepository = normalizedRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int generateAll(Long refreshRunId) {
        insightCacheRepository.deleteAll();
        List<InsightCache> insights = new ArrayList<>();
        insights.addAll(categoryMonthlyAverages(refreshRunId));
        insights.addAll(topMerchant(refreshRunId));
        insights.addAll(weekdayPattern(refreshRunId));
        insights.addAll(frequencyInsights(refreshRunId));
        insights.addAll(monthOverMonth(refreshRunId));

        insights.sort(Comparator.comparingDouble(InsightCache::getRankScore).reversed());
        if (insights.size() > maxResults * 2) {
            insights = insights.subList(0, maxResults * 2);
        }
        insightCacheRepository.saveAll(insights);
        return insights.size();
    }

    private List<InsightCache> categoryMonthlyAverages(Long refreshRunId) {
        List<InsightCache> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (SpendingCategory category : categoryRepository.findAll()) {
            if ("uncategorized".equals(category.getSlug())) {
                continue;
            }
            List<SpendingSnapshot> monthly = snapshotRepository.findByGrainAndCategoryId(
                    SnapshotGrain.MONTHLY, category.getId());
            List<BigDecimal> lastSix = monthly.stream()
                    .filter(s -> YearMonth.from(s.getPeriodStart()).isBefore(YearMonth.from(now)))
                    .sorted(Comparator.comparing(SpendingSnapshot::getPeriodStart).reversed())
                    .limit(6)
                    .map(SpendingSnapshot::getTotalAmount)
                    .collect(Collectors.toList());
            if (lastSix.size() < 2) {
                continue;
            }
            BigDecimal avg = lastSix.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(lastSix.size()), 2, RoundingMode.HALF_UP);

            String title = category.getDisplayName() + " spending";
            String body = String.format(
                    "%s purchases average $%s/month.",
                    category.getDisplayName(),
                    avg
            );
            result.add(buildInsight(
                    "CATEGORY_MONTHLY_AVG",
                    title,
                    body,
                    InsightSeverity.info,
                    Map.of("categorySlug", category.getSlug(), "monthlyAverage", avg),
                    avg.doubleValue(),
                    refreshRunId
            ));
        }
        return result;
    }

    private List<InsightCache> topMerchant(Long refreshRunId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(90);
        return normalizedRepository.sumByMerchantBetween(start, end).stream()
                .max(Comparator.comparing(row -> (BigDecimal) row[1]))
                .map(row -> {
                    String merchantKey = (String) row[0];
                    BigDecimal total = (BigDecimal) row[1];
                    String title = "Top merchant";
                    String body = String.format(
                            "Most spent at %s ($%s in last 90 days).",
                            merchantKey,
                            total.setScale(2, RoundingMode.HALF_UP)
                    );
                    return List.of(buildInsight(
                            "TOP_MERCHANT",
                            title,
                            body,
                            InsightSeverity.info,
                            Map.of("merchantKey", merchantKey, "total", total),
                            total.doubleValue(),
                            refreshRunId
                    ));
                })
                .orElse(List.of());
    }

    private List<InsightCache> weekdayPattern(Long refreshRunId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(3);
        BigDecimal weekend = BigDecimal.ZERO;
        BigDecimal weekday = BigDecimal.ZERO;
        int weekendDays = 0;
        int weekdayDays = 0;

        for (Object[] row : normalizedRepository.sumByDayBetween(start, end)) {
            LocalDate day = (LocalDate) row[0];
            BigDecimal total = (BigDecimal) row[1];
            DayOfWeek dow = day.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekend = weekend.add(total);
                weekendDays++;
            } else {
                weekday = weekday.add(total);
                weekdayDays++;
            }
        }
        if (weekendDays == 0 || weekdayDays == 0) {
            return List.of();
        }
        double weekendAvg = weekend.divide(BigDecimal.valueOf(weekendDays), 2, RoundingMode.HALF_UP).doubleValue();
        double weekdayAvg = weekday.divide(BigDecimal.valueOf(weekdayDays), 2, RoundingMode.HALF_UP).doubleValue();
        if (weekdayAvg <= 0) {
            return List.of();
        }
        double pct = ((weekendAvg - weekdayAvg) / weekdayAvg) * 100;
        String body = pct >= 0
                ? String.format("You spend %.0f%% more per day on weekends.", pct)
                : String.format("You spend %.0f%% less per day on weekends.", Math.abs(pct));

        return List.of(buildInsight(
                "WEEKDAY_PATTERN",
                "Weekend vs weekday",
                body,
                InsightSeverity.info,
                Map.of("weekendDailyAvg", weekendAvg, "weekdayDailyAvg", weekdayAvg, "percentDifference", pct),
                Math.abs(pct),
                refreshRunId
        ));
    }

    private List<InsightCache> frequencyInsights(Long refreshRunId) {
        List<InsightCache> result = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.withDayOfMonth(1);
        for (SpendingCategory category : categoryRepository.findAll()) {
            if ("uncategorized".equals(category.getSlug())) {
                continue;
            }
            long count = normalizedRepository.countByCategoryIdAndOccurredOnBetween(
                    category.getId(), start, end);
            if (count < 2) {
                continue;
            }
            String body = String.format("%d %s purchases this month.", count, category.getDisplayName().toLowerCase());
            result.add(buildInsight(
                    "FREQUENCY",
                    category.getDisplayName() + " frequency",
                    body,
                    InsightSeverity.info,
                    Map.of("categorySlug", category.getSlug(), "count", count),
                    count,
                    refreshRunId
            ));
        }
        return result;
    }

    private List<InsightCache> monthOverMonth(Long refreshRunId) {
        List<InsightCache> result = new ArrayList<>();
        YearMonth current = YearMonth.now().minusMonths(1);
        YearMonth previous = current.minusMonths(1);

        for (SpendingCategory category : categoryRepository.findAll()) {
            if ("uncategorized".equals(category.getSlug())) {
                continue;
            }
            BigDecimal currentTotal = totalForMonth(category.getId(), current);
            BigDecimal previousTotal = totalForMonth(category.getId(), previous);
            if (previousTotal.compareTo(BigDecimal.ZERO) <= 0 || currentTotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            double pct = currentTotal.subtract(previousTotal)
                    .divide(previousTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            if (Math.abs(pct) < 5) {
                continue;
            }
            String direction = pct > 0 ? "up" : "down";
            String body = String.format(
                    "%s is %s %.0f%% vs prior month.",
                    category.getDisplayName(),
                    direction,
                    Math.abs(pct)
            );
            result.add(buildInsight(
                    "MOM_CHANGE",
                    category.getDisplayName() + " month-over-month",
                    body,
                    pct > 15 ? InsightSeverity.warning : InsightSeverity.info,
                    Map.of("categorySlug", category.getSlug(), "percentChange", pct),
                    Math.abs(pct),
                    refreshRunId
            ));
        }
        return result;
    }

    private BigDecimal totalForMonth(Long categoryId, YearMonth month) {
        return snapshotRepository.findByGrainAndCategoryId(SnapshotGrain.MONTHLY, categoryId).stream()
                .filter(s -> YearMonth.from(s.getPeriodStart()).equals(month))
                .map(SpendingSnapshot::getTotalAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private InsightCache buildInsight(
            String type,
            String title,
            String body,
            InsightSeverity severity,
            Map<String, Object> payload,
            double rankScore,
            Long refreshRunId
    ) {
        InsightCache insight = new InsightCache();
        insight.setInsightType(type);
        insight.setTitle(title);
        insight.setBody(body);
        insight.setSeverity(severity);
        try {
            insight.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            insight.setPayloadJson("{}");
        }
        LocalDate today = LocalDate.now();
        insight.setValidFrom(today);
        insight.setValidTo(today.plusMonths(1));
        insight.setGeneratedAt(Instant.now());
        insight.setRefreshRunId(refreshRunId);
        insight.setRankScore(rankScore);
        return insight;
    }
}
