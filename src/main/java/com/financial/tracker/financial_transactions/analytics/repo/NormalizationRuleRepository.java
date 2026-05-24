package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.NormalizationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NormalizationRuleRepository extends JpaRepository<NormalizationRule, Long> {
    List<NormalizationRule> findAllByEnabledTrueOrderByPriorityAsc();
}
