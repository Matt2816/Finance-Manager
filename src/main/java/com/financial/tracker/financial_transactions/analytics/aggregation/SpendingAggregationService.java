package com.financial.tracker.financial_transactions.analytics.aggregation;

import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.SnapshotGrain;
import com.financial.tracker.financial_transactions.analytics.model.SpendingSnapshot;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.analytics.repo.SpendingSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpendingAggregationService {

    private final NormalizedTransactionRepository normalizedRepo;
    private final SpendingSnapshotRepository snapshotRepository;

    @Value("${analytics.snapshot.months:24}")
    private int snapshotMonths;

    public SpendingAggregationService(
            NormalizedTransactionRepository normalizedRepo,
            SpendingSnapshotRepository snapshotRepository
    ) {
        this.normalizedRepo = normalizedRepo;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public int rebuildSnapshots(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(snapshotMonths).withDayOfMonth(1);
        snapshotRepository.deleteByUserId(userId);

        List<NormalizedTransaction> transactions =
                normalizedRepo.findByUserIdAndOccurredOnBetween(userId, start, end);

        Map<String, SpendingSnapshot> dailyByCategory = new HashMap<>();
        Map<String, SpendingSnapshot> weeklyByCategory = new HashMap<>();
        Map<String, SpendingSnapshot> monthlyByCategory = new HashMap<>();
        Map<String, SpendingSnapshot> merchantMonthly = new HashMap<>();

        for (NormalizedTransaction tx : transactions) {
            if (tx.isRecurringGenerated()) {
                continue;
            }
            BigDecimal amount = tx.getAmount();
            if (tx.getDirection() == TransactionDirection.CREDIT) {
                amount = amount.negate();
            }
            LocalDate day = tx.getOccurredOn();

            accumulate(dailyByCategory, key(day, day, SnapshotGrain.DAILY, tx.getCategoryId(), null),
                    day, day, SnapshotGrain.DAILY, tx.getCategoryId(), null, amount, userId);
            LocalDate weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            accumulate(weeklyByCategory, key(weekStart, weekEnd, SnapshotGrain.WEEKLY, tx.getCategoryId(), null),
                    weekStart, weekEnd, SnapshotGrain.WEEKLY, tx.getCategoryId(), null, amount, userId);

            LocalDate monthStart = day.withDayOfMonth(1);
            LocalDate monthEnd = day.with(TemporalAdjusters.lastDayOfMonth());
            accumulate(monthlyByCategory, key(monthStart, monthEnd, SnapshotGrain.MONTHLY, tx.getCategoryId(), null),
                    monthStart, monthEnd, SnapshotGrain.MONTHLY, tx.getCategoryId(), null, amount, userId);

            if (tx.getMerchantKey() != null) {
                accumulate(merchantMonthly,
                        key(monthStart, monthEnd, SnapshotGrain.MERCHANT_MONTHLY, null, tx.getMerchantKey()),
                        monthStart, monthEnd, SnapshotGrain.MERCHANT_MONTHLY, null, tx.getMerchantKey(), amount, userId);
            }
        }

        List<SpendingSnapshot> all = new ArrayList<>();
        all.addAll(dailyByCategory.values());
        all.addAll(weeklyByCategory.values());
        all.addAll(monthlyByCategory.values());
        all.addAll(merchantMonthly.values());
        ZonedDateTime now = ZonedDateTime.now();
        for (SpendingSnapshot snapshot : all) {
            snapshot.setComputedAt(now);
        }
        snapshotRepository.saveAll(all);
        return all.size();
    }

    private static String key(
            LocalDate start,
            LocalDate end,
            SnapshotGrain grain,
            Long categoryId,
            String merchantKey
    ) {
        return start + "|" + end + "|" + grain + "|" + categoryId + "|" + merchantKey;
    }

    private static void accumulate(
            Map<String, SpendingSnapshot> map,
            String mapKey,
            LocalDate periodStart,
            LocalDate periodEnd,
            SnapshotGrain grain,
            Long categoryId,
            String merchantKey,
            BigDecimal amount,
            Long userId
    ) {
        SpendingSnapshot snapshot = map.computeIfAbsent(mapKey, k -> {
            SpendingSnapshot s = new SpendingSnapshot();
            s.setUserId(userId);
            s.setPeriodStart(periodStart);
            s.setPeriodEnd(periodEnd);
            s.setGrain(grain);
            s.setCategoryId(categoryId);
            s.setMerchantKey(merchantKey);
            s.setTotalAmount(BigDecimal.ZERO);
            s.setTransactionCount(0);
            return s;
        });
        snapshot.setTotalAmount(snapshot.getTotalAmount().add(amount));
        snapshot.setTransactionCount(snapshot.getTransactionCount() + 1);
    }
}
