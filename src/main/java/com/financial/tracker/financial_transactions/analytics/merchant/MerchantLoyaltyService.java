package com.financial.tracker.financial_transactions.analytics.merchant;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import com.financial.tracker.financial_transactions.analytics.model.MerchantLoyaltyMetrics;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantLoyaltyMetricsRepository;
import com.financial.tracker.financial_transactions.analytics.repo.MerchantRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class MerchantLoyaltyService {

    private final NormalizedTransactionRepository normalizedRepo;
    private final MerchantLoyaltyMetricsRepository loyaltyRepository;
    private final MerchantRepository merchantRepository;
    private final AtomicBoolean calculating = new AtomicBoolean(false);

    public MerchantLoyaltyService(
            NormalizedTransactionRepository normalizedRepo,
            MerchantLoyaltyMetricsRepository loyaltyRepository,
            MerchantRepository merchantRepository
    ) {
        this.normalizedRepo = normalizedRepo;
        this.loyaltyRepository = loyaltyRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public CalculationResult calculateAllLoyaltyMetrics() {
        if (!calculating.compareAndSet(false, true)) {
            throw new IllegalStateException("Loyalty metrics calculation already in progress");
        }
        try {
            List<NormalizedTransaction> transactions = normalizedRepo.findAll();

            Map<String, List<NormalizedTransaction>> transactionsByMerchant = transactions.stream()
                    .filter(t -> t.getMerchantKey() != null && !t.getMerchantKey().isBlank())
                    .collect(Collectors.groupingBy(this::loyaltyGroupKey));

            Map<String, MerchantLoyaltyMetrics> existingByKey = loyaltyRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            MerchantLoyaltyMetrics::getMerchantKey,
                            m -> m,
                            (a, b) -> a.getId() != null && b.getId() != null && a.getId() < b.getId() ? a : b
                    ));

            List<MerchantLoyaltyMetrics> toSave = new ArrayList<>();
            Set<String> processedKeys = new HashSet<>();

            for (Map.Entry<String, List<NormalizedTransaction>> entry : transactionsByMerchant.entrySet()) {
                String key = entry.getKey();
                MerchantLoyaltyMetrics metrics = existingByKey.get(key);
                if (metrics == null) {
                    metrics = new MerchantLoyaltyMetrics();
                    metrics.setMerchantKey(key);
                }
                calculateMetrics(metrics, key, entry.getValue());
                toSave.add(metrics);
                processedKeys.add(key);
            }

            for (MerchantLoyaltyMetrics existing : existingByKey.values()) {
                if (!processedKeys.contains(existing.getMerchantKey())) {
                    loyaltyRepository.delete(existing);
                }
            }

            loyaltyRepository.saveAll(toSave);
            return new CalculationResult(toSave.size());
        } finally {
            calculating.set(false);
        }
    }

    private String loyaltyGroupKey(NormalizedTransaction transaction) {
        if (transaction.getResolvedMerchantId() != null) {
            return transaction.getResolvedMerchantId().toString();
        }
        return transaction.getMerchantKey();
    }

    private MerchantLoyaltyMetrics calculateMetrics(
            MerchantLoyaltyMetrics metrics,
            String merchantKeyOrId,
            List<NormalizedTransaction> transactions
    ) {
        String canonicalName = merchantKeyOrId;
        Long categoryId = null;

        try {
            UUID merchantId = UUID.fromString(merchantKeyOrId);
            Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);
            if (merchantOpt.isPresent()) {
                Merchant merchant = merchantOpt.get();
                canonicalName = merchant.getCanonicalName();
            }
        } catch (IllegalArgumentException e) {
            canonicalName = merchantKeyOrId;
        }

        Optional<NormalizedTransaction> sample = transactions.stream().findFirst();
        if (sample.isPresent() && sample.get().getCategoryId() != null) {
            categoryId = sample.get().getCategoryId();
        }

        metrics.setCanonicalName(canonicalName);
        metrics.setCategoryId(categoryId);
        metrics.setCalculatedAt(LocalDate.now());

        transactions.sort(Comparator.comparing(NormalizedTransaction::getOccurredOn));

        int totalTransactions = transactions.size();
        metrics.setTotalTransactions(totalTransactions);

        BigDecimal totalSpend = transactions.stream()
                .map(NormalizedTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        metrics.setTotalSpend(totalSpend);

        BigDecimal avgTransactionSize = totalTransactions > 0
                ? totalSpend.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        metrics.setAvgTransactionSize(avgTransactionSize);

        LocalDate firstVisit = transactions.get(0).getOccurredOn();
        LocalDate lastVisit = transactions.get(transactions.size() - 1).getOccurredOn();
        metrics.setFirstVisit(firstVisit);
        metrics.setLastVisit(lastVisit);

        if (totalTransactions > 1) {
            long daysBetween = ChronoUnit.DAYS.between(firstVisit, lastVisit);
            double frequency = (double) daysBetween / (totalTransactions - 1);
            metrics.setVisitFrequencyDays(frequency);
        } else {
            metrics.setVisitFrequencyDays(null);
        }

        double loyaltyScore = calculateLoyaltyScore(
                totalTransactions,
                totalSpend,
                metrics.getVisitFrequencyDays(),
                lastVisit
        );
        metrics.setLoyaltyScore(loyaltyScore);

        if (totalTransactions >= 4) {
            int midPoint = totalTransactions / 2;
            List<NormalizedTransaction> firstHalf = transactions.subList(0, midPoint);
            List<NormalizedTransaction> secondHalf = transactions.subList(midPoint, totalTransactions);

            BigDecimal firstHalfSpend = firstHalf.stream()
                    .map(NormalizedTransaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal secondHalfSpend = secondHalf.stream()
                    .map(NormalizedTransaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (firstHalfSpend.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growthRate = secondHalfSpend
                        .subtract(firstHalfSpend)
                        .divide(firstHalfSpend, 4, RoundingMode.HALF_UP);
                metrics.setSpendGrowthRate(growthRate);
            } else {
                metrics.setSpendGrowthRate(null);
            }
        } else {
            metrics.setSpendGrowthRate(null);
        }

        return metrics;
    }

    private double calculateLoyaltyScore(
            int totalTransactions,
            BigDecimal totalSpend,
            Double visitFrequencyDays,
            LocalDate lastVisit
    ) {
        double score = 0.0;

        double frequencyScore = Math.min(40, (totalTransactions / 20.0) * 40);
        score += frequencyScore;

        if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
            double logSpend = Math.log10(totalSpend.doubleValue() + 1);
            double spendScore = Math.min(30, logSpend * 10);
            score += spendScore;
        }

        if (visitFrequencyDays != null && visitFrequencyDays > 0) {
            double consistencyScore = Math.max(0, 20 - (visitFrequencyDays / 7.0) * 5);
            score += Math.min(20, consistencyScore);
        }

        long daysSinceLastVisit = ChronoUnit.DAYS.between(lastVisit, LocalDate.now());
        double recencyScore = Math.max(0, 10 - (daysSinceLastVisit / 30.0) * 10);
        score += recencyScore;

        return Math.min(100, Math.max(0, score));
    }

    public record CalculationResult(int merchantsCalculated) {}
}
