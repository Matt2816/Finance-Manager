package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.MerchantBrandRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantBrandRuleRepository extends JpaRepository<MerchantBrandRule, Long> {
    List<MerchantBrandRule> findAllByOrderByPriorityAsc();
}
