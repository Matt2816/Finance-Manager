package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.MerchantAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {

    Optional<MerchantAlias> findByNormalizedName(String normalizedName);

    @Query(value = """
        SELECT ma.*
        FROM merchant_aliases ma
        JOIN merchants m ON ma.merchant_id = m.id
        WHERE ma.normalized_name % :input
        ORDER BY similarity(ma.normalized_name, :input) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<MerchantAlias> findCandidatesByTrigram(@Param("input") String input, @Param("limit") int limit);
}
