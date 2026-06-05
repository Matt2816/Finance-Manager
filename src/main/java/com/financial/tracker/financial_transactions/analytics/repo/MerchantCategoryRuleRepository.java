package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.MerchantCategoryRule;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface MerchantCategoryRuleRepository extends ListCrudRepository<MerchantCategoryRule, Long> {
    List<MerchantCategoryRule> findAllByOrderByPriorityAsc();

    List<MerchantCategoryRule> findByUserIdOrderByPriorityAsc(Long userId);

    Optional<MerchantCategoryRule> findByIdAndUserId(Long id, Long userId);

    List<MerchantCategoryRule> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
