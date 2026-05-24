package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface SpendingCategoryRepository extends ListCrudRepository<SpendingCategory, Long> {
    Optional<SpendingCategory> findBySlug(String slug);
}
