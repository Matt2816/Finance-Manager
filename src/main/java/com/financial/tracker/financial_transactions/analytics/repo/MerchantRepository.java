package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
}
