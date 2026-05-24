package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.InsightCache;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface InsightCacheRepository extends ListCrudRepository<InsightCache, Long> {
    List<InsightCache> findByValidToGreaterThanEqualOrderByRankScoreDesc(LocalDate date);

    List<InsightCache> findByInsightTypeStartingWithAndValidToGreaterThanEqualOrderByRankScoreDesc(
            String prefix,
            LocalDate date
    );

    List<InsightCache> findByInsightTypeAndValidToGreaterThanEqualOrderByRankScoreDesc(
            String type,
            LocalDate date
    );

    void deleteAll();
}
