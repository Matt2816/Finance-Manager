package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.InsightCache;
import org.springframework.data.repository.ListCrudRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    List<InsightCache> findByUserId(Long userId);

    Optional<InsightCache> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    List<InsightCache> findByUserIdAndValidToGreaterThanEqualOrderByRankScoreDesc(Long userId, LocalDate date);

    List<InsightCache> findByUserIdAndInsightTypeStartingWithAndValidToGreaterThanEqualOrderByRankScoreDesc(
            Long userId,
            String prefix,
            LocalDate date
    );

    List<InsightCache> findByUserIdAndInsightTypeAndValidToGreaterThanEqualOrderByRankScoreDesc(
            Long userId,
            String type,
            LocalDate date
    );
}
