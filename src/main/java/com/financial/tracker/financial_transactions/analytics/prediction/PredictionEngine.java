package com.financial.tracker.financial_transactions.analytics.prediction;

import com.financial.tracker.financial_transactions.analytics.model.ForecastScope;
import com.financial.tracker.financial_transactions.analytics.model.SnapshotGrain;
import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import com.financial.tracker.financial_transactions.analytics.model.SpendingForecast;
import com.financial.tracker.financial_transactions.analytics.model.SpendingSnapshot;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingCategoryRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingForecastRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingSnapshotRepository;
import com.financial.tracker.financial_transactions.model.RecurringExpense;
import com.financial.tracker.financial_transactions.repo.ExpenseRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionEngine {

    private final SpendingSnapshotRepository snapshotRepository;
    private final SpendingForecastRepository forecastRepository;
    private final SpendingCategoryRepository categoryRepository;
    private final ExpenseRepo expenseRepo;

    @Value("${analytics.forecast.horizon-months:3}")
    private int horizonMonths;

    public PredictionEngine(
            SpendingSnapshotRepository snapshotRepository,
            SpendingForecastRepository forecastRepository,
            SpendingCategoryRepository categoryRepository,
            ExpenseRepo expenseRepo
    ) {
        this.snapshotRepository = snapshotRepository;
        this.forecastRepository = forecastRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepo = expenseRepo;
    }

    @Transactional
    public int generateForecasts(Long refreshRunId, Long userId) {
        forecastRepository.deleteByUserId(userId);
        List<SpendingForecast> forecasts = new ArrayList<>();
        forecasts.addAll(forecastScope(userId, ForecastScope.TOTAL, null, refreshRunId));
        for (SpendingCategory category : categoryRepository.findByUserId(userId)) {
            if ("uncategorized".equals(category.getSlug())) {
                continue;
            }
            forecasts.addAll(forecastScope(userId, ForecastScope.CATEGORY, category.getSlug(), refreshRunId));
        }
        forecasts.addAll(forecastRecurring(userId, refreshRunId));
        forecastRepository.saveAll(forecasts);
        return forecasts.size();
    }

    private List<SpendingForecast> forecastScope(
            Long userId,
            ForecastScope scope,
            String scopeKey,
            Long refreshRunId
    ) {
        List<SpendingSnapshot> monthly = loadMonthlySeries(userId, scope, scopeKey);
        double[] series = monthly.stream()
                .mapToDouble(s -> s.getTotalAmount().doubleValue())
                .toArray();

        if (series.length == 0) {
            return List.of();
        }

        List<SpendingForecast> result = new ArrayList<>();
        YearMonth lastMonth = YearMonth.from(monthly.get(monthly.size() - 1).getPeriodStart());

        for (int h = 1; h <= horizonMonths; h++) {
            YearMonth target = lastMonth.plusMonths(h);
            ForecastBlend blend = blendForecast(series, target.getMonthValue());
            BigDecimal predicted = BigDecimal.valueOf(blend.predicted()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lower = BigDecimal.valueOf(blend.lower()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal upper = BigDecimal.valueOf(blend.upper()).setScale(2, RoundingMode.HALF_UP);

            SpendingForecast forecast = new SpendingForecast();
            forecast.setUserId(userId);
            forecast.setForecastDate(target.atDay(1));
            forecast.setScope(scope);
            forecast.setScopeKey(scopeKey);
            forecast.setPredictedAmount(predicted);
            forecast.setLowerBound(lower);
            forecast.setUpperBound(upper);
            forecast.setMethod(blend.method());
            forecast.setConfidence(blend.confidence());
            forecast.setGeneratedAt(Instant.now());
            forecast.setRefreshRunId(refreshRunId);
            result.add(forecast);
        }
        return result;
    }

    private List<SpendingSnapshot> loadMonthlySeries(Long userId, ForecastScope scope, String scopeKey) {
        if (scope == ForecastScope.CATEGORY) {
            return categoryRepository.findBySlugAndUserId(scopeKey, userId)
                    .map(cat -> snapshotRepository.findByUserIdAndGrainAndCategoryId(
                            userId, SnapshotGrain.MONTHLY, cat.getId()))
                    .orElse(List.of())
                    .stream()
                    .sorted(Comparator.comparing(SpendingSnapshot::getPeriodStart))
                    .collect(Collectors.toList());
        }

        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        for (SpendingSnapshot s : snapshotRepository.findByUserId(userId)) {
            if (s.getGrain() != SnapshotGrain.MONTHLY || s.getCategoryId() == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(s.getPeriodStart());
            byMonth.merge(ym, s.getTotalAmount(), BigDecimal::add);
        }
        return byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    SpendingSnapshot s = new SpendingSnapshot();
                    s.setPeriodStart(e.getKey().atDay(1));
                    s.setTotalAmount(e.getValue());
                    return s;
                })
                .collect(Collectors.toList());
    }

    private ForecastBlend blendForecast(double[] series, int targetMonth) {
        int n = series.length;
        double trailingAvg = n >= 3
                ? (series[n - 1] + series[n - 2] + series[n - 3]) / 3.0
                : Arrays.stream(series).average().orElse(0);

        double seasonal = trailingAvg;
        if (n >= 12) {
            double overall = Arrays.stream(series).average().orElse(0);
            double monthSum = 0;
            int monthCount = 0;
            for (int i = 0; i < n; i++) {
                YearMonth ym = YearMonth.now().minusMonths(n - 1L - i);
                if (ym.getMonthValue() == targetMonth) {
                    monthSum += series[i];
                    monthCount++;
                }
            }
            if (monthCount > 0 && overall > 0) {
                double index = (monthSum / monthCount) / overall;
                seasonal = overall * index;
            }
        }

        double holt = trailingAvg;
        if (n >= 6) {
            double[] hw = HoltWintersForecaster.forecast(series, 1, 12);
            holt = hw[0];
        }

        double wSeasonal = n >= 12 ? 0.5 : 0;
        double wHolt = n >= 6 ? 0.3 : 0;
        double wAvg = 1.0 - wSeasonal - wHolt;
        double predicted = wSeasonal * seasonal + wHolt * holt + wAvg * trailingAvg;

        double variance = 0;
        if (n > 1) {
            double mean = Arrays.stream(series).average().orElse(0);
            variance = Arrays.stream(series).map(v -> Math.pow(v - mean, 2)).average().orElse(0);
        }
        double std = Math.sqrt(variance);
        double margin = Math.max(predicted * 0.15, std * 1.5);
        double lower = Math.max(0, predicted - margin);
        double upper = predicted + margin;

        if (n >= 2) {
            double prev = series[n - 1];
            double maxGrowth = prev * 1.5;
            double minGrowth = prev * 0.5;
            predicted = Math.min(Math.max(predicted, minGrowth), maxGrowth);
        }

        String method = "blended";
        double confidence = Math.min(0.95, 0.4 + n * 0.05);
        return new ForecastBlend(predicted, lower, upper, method, confidence);
    }

    private List<SpendingForecast> forecastRecurring(Long userId, Long refreshRunId) {
        double monthlyRecurring = expenseRepo.findByUserId(userId).stream()
                .mapToDouble(RecurringExpense::getAmount)
                .sum();

        List<SpendingForecast> result = new ArrayList<>();
        YearMonth start = YearMonth.now().plusMonths(1);
        for (int h = 0; h < horizonMonths; h++) {
            YearMonth target = start.plusMonths(h);
            SpendingForecast forecast = new SpendingForecast();
            forecast.setUserId(userId);
            forecast.setForecastDate(target.atDay(1));
            forecast.setScope(ForecastScope.TOTAL);
            forecast.setScopeKey("recurring");
            BigDecimal amount = BigDecimal.valueOf(monthlyRecurring).setScale(2, RoundingMode.HALF_UP);
            forecast.setPredictedAmount(amount);
            forecast.setLowerBound(amount);
            forecast.setUpperBound(amount);
            forecast.setMethod("recurring_schedule");
            forecast.setConfidence(0.9);
            forecast.setGeneratedAt(Instant.now());
            forecast.setRefreshRunId(refreshRunId);
            result.add(forecast);
        }
        return result;
    }

    private record ForecastBlend(double predicted, double lower, double upper, String method, double confidence) {}
}
