package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.SpendingCategory;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface SpendingCategoryRepository extends ListCrudRepository<SpendingCategory, Long> {
    Optional<SpendingCategory> findBySlug(String slug);

    Optional<SpendingCategory> findBySlugAndUserId(String slug, Long userId);

    long countByUserId(Long userId);

    List<SpendingCategory> findByUserId(Long userId);

    Optional<SpendingCategory> findByIdAndUserId(Long id, Long userId);
}
