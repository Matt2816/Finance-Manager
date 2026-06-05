package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.ForecastScope;
import com.financial.tracker.financial_transactions.analytics.model.SpendingForecast;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface SpendingForecastRepository extends ListCrudRepository<SpendingForecast, Long> {
    List<SpendingForecast> findByScopeOrderByForecastDateAsc(ForecastScope scope);

    List<SpendingForecast> findByScopeAndScopeKeyOrderByForecastDateAsc(ForecastScope scope, String scopeKey);

    void deleteAll();

    List<SpendingForecast> findByUserId(Long userId);

    Optional<SpendingForecast> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    List<SpendingForecast> findByUserIdAndScopeOrderByForecastDateAsc(Long userId, ForecastScope scope);

    List<SpendingForecast> findByUserIdAndScopeAndScopeKeyOrderByForecastDateAsc(
            Long userId,
            ForecastScope scope,
            String scopeKey);
}
