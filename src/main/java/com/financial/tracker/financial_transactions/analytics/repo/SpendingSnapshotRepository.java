package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.SnapshotGrain;
import com.financial.tracker.financial_transactions.analytics.model.SpendingSnapshot;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface SpendingSnapshotRepository extends ListCrudRepository<SpendingSnapshot, Long> {
    List<SpendingSnapshot> findByGrainAndPeriodStartBetween(SnapshotGrain grain, LocalDate start, LocalDate end);

    List<SpendingSnapshot> findByGrainAndCategoryId(SnapshotGrain grain, Long categoryId);

    void deleteByPeriodStartBefore(LocalDate cutoff);

    @Query("SELECT s.categoryId, SUM(s.totalAmount), SUM(s.transactionCount) " +
           "FROM SpendingSnapshot s " +
           "WHERE s.grain = 'MONTHLY' AND s.categoryId IS NOT NULL " +
           "GROUP BY s.categoryId")
    List<Object[]> findMonthlyCategorySpending();
}
