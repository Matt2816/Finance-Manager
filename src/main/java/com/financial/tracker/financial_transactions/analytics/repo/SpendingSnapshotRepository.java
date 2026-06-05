package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.SnapshotGrain;
import com.financial.tracker.financial_transactions.analytics.model.SpendingSnapshot;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpendingSnapshotRepository extends ListCrudRepository<SpendingSnapshot, Long> {
    List<SpendingSnapshot> findByGrainAndPeriodStartBetween(SnapshotGrain grain, LocalDate start, LocalDate end);

    List<SpendingSnapshot> findByGrainAndCategoryId(SnapshotGrain grain, Long categoryId);

    void deleteByPeriodStartBefore(LocalDate cutoff);

    List<SpendingSnapshot> findByUserId(Long userId);

    Optional<SpendingSnapshot> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    List<SpendingSnapshot> findByUserIdAndGrainAndPeriodStartBetween(
            Long userId,
            SnapshotGrain grain,
            LocalDate start,
            LocalDate end);

    List<SpendingSnapshot> findByUserIdAndGrainAndCategoryId(Long userId, SnapshotGrain grain, Long categoryId);

    void deleteByUserIdAndPeriodStartBefore(Long userId, LocalDate cutoff);

    @Query("SELECT s.categoryId, SUM(s.totalAmount), SUM(s.transactionCount) " +
           "FROM SpendingSnapshot s " +
           "WHERE s.grain = 'MONTHLY' AND s.categoryId IS NOT NULL " +
           "GROUP BY s.categoryId")
    List<Object[]> findMonthlyCategorySpending();

    @Query("SELECT s.categoryId, SUM(s.totalAmount), SUM(s.transactionCount) " +
           "FROM SpendingSnapshot s " +
           "WHERE s.grain = 'MONTHLY' AND s.categoryId IS NOT NULL " +
           "AND s.userId = :userId " +
           "GROUP BY s.categoryId")
    List<Object[]> findMonthlyCategorySpending(@Param("userId") Long userId);

    @Query("SELECT s.categoryId, SUM(s.totalAmount), SUM(s.transactionCount) " +
           "FROM SpendingSnapshot s " +
           "WHERE s.grain = 'MONTHLY' AND s.categoryId IS NOT NULL " +
           "AND s.periodStart >= :start AND s.periodStart <= :end " +
           "GROUP BY s.categoryId")
    List<Object[]> findMonthlyCategorySpendingBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT s.categoryId, SUM(s.totalAmount), SUM(s.transactionCount) " +
           "FROM SpendingSnapshot s " +
           "WHERE s.grain = 'MONTHLY' AND s.categoryId IS NOT NULL " +
           "AND s.userId = :userId " +
           "AND s.periodStart >= :start AND s.periodStart <= :end " +
           "GROUP BY s.categoryId")
    List<Object[]> findMonthlyCategorySpendingBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
