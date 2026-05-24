package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NormalizedTransactionRepository extends ListCrudRepository<NormalizedTransaction, Long> {

    Optional<NormalizedTransaction> findByTransactionId(Integer transactionId);

    List<NormalizedTransaction> findByOccurredOnBetween(LocalDate start, LocalDate end);

    @Query("""
            SELECT n.categoryId, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.recurringGenerated = false
            GROUP BY n.categoryId
            """)
    List<Object[]> sumByCategoryBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT n.merchantKey, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.merchantKey IS NOT NULL
            GROUP BY n.merchantKey
            """)
    List<Object[]> sumByMerchantBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT n.occurredOn, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.recurringGenerated = false
            GROUP BY n.occurredOn
            ORDER BY n.occurredOn
            """)
    List<Object[]> sumByDayBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT FUNCTION('YEAR', n.occurredOn), FUNCTION('MONTH', n.occurredOn),
                   n.categoryId, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.recurringGenerated = false
            GROUP BY FUNCTION('YEAR', n.occurredOn), FUNCTION('MONTH', n.occurredOn), n.categoryId
            """)
    List<Object[]> sumByMonthAndCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);

    long countByCategoryIdAndOccurredOnBetween(Long categoryId, LocalDate start, LocalDate end);
}
