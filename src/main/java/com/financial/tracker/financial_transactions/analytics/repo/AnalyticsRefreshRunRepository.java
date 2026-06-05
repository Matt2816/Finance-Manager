package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.AnalyticsRefreshRun;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface AnalyticsRefreshRunRepository extends ListCrudRepository<AnalyticsRefreshRun, Long> {
    Optional<AnalyticsRefreshRun> findFirstByOrderByStartedAtDesc();

    Optional<AnalyticsRefreshRun> findFirstByUserIdOrderByStartedAtDesc(Long userId);
}
