package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.MerchantCategoryRule;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface MerchantCategoryRuleRepository extends ListCrudRepository<MerchantCategoryRule, Long> {
    List<MerchantCategoryRule> findAllByOrderByPriorityAsc();
}
