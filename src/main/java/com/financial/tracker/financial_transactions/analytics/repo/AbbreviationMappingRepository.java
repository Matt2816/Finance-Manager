package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.AbbreviationMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbbreviationMappingRepository extends JpaRepository<AbbreviationMapping, String> {
}
