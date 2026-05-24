package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.MerchantLoyaltyMetrics;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantLoyaltyMetricsRepository extends ListCrudRepository<MerchantLoyaltyMetrics, Long> {
    List<MerchantLoyaltyMetrics> findAllByOrderByLoyaltyScoreDesc();
    List<MerchantLoyaltyMetrics> findByCategoryIdOrderByLoyaltyScoreDesc(Long categoryId);
    Optional<MerchantLoyaltyMetrics> findByMerchantKey(String merchantKey);
}
