package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.ForecastScope;
import com.financial.tracker.financial_transactions.analytics.model.SpendingForecast;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface SpendingForecastRepository extends ListCrudRepository<SpendingForecast, Long> {
    List<SpendingForecast> findByScopeOrderByForecastDateAsc(ForecastScope scope);

    List<SpendingForecast> findByScopeAndScopeKeyOrderByForecastDateAsc(ForecastScope scope, String scopeKey);

    void deleteAll();
}
