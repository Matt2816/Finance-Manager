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

    void deleteByTransactionId(Integer transactionId);

    List<NormalizedTransaction> findByOccurredOnBetween(LocalDate start, LocalDate end);

    List<NormalizedTransaction> findByCategoryId(Long categoryId);

    Optional<NormalizedTransaction> findByTransactionIdAndUserId(Integer transactionId, Long userId);

    void deleteByTransactionIdAndUserId(Integer transactionId, Long userId);

    List<NormalizedTransaction> findByUserId(Long userId);

    Optional<NormalizedTransaction> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    List<NormalizedTransaction> findByUserIdAndOccurredOnBetween(Long userId, LocalDate start, LocalDate end);

    List<NormalizedTransaction> findByUserIdAndCategoryId(Long userId, Long categoryId);

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
            SELECT n.categoryId, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.userId = :userId
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.recurringGenerated = false
            GROUP BY n.categoryId
            """)
    List<Object[]> sumByCategoryBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

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
            SELECT n.merchantKey, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.userId = :userId
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.merchantKey IS NOT NULL
            GROUP BY n.merchantKey
            """)
    List<Object[]> sumByMerchantBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

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
            SELECT n.occurredOn, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.userId = :userId
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.recurringGenerated = false
            GROUP BY n.occurredOn
            ORDER BY n.occurredOn
            """)
    List<Object[]> sumByDayBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

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

    @Query("""
            SELECT FUNCTION('YEAR', n.occurredOn), FUNCTION('MONTH', n.occurredOn),
                   n.categoryId, SUM(n.amount), COUNT(n)
            FROM NormalizedTransaction n
            WHERE n.occurredOn BETWEEN :start AND :end
              AND n.userId = :userId
              AND n.direction = com.financial.tracker.financial_transactions.analytics.model.TransactionDirection.DEBIT
              AND n.recurringGenerated = false
            GROUP BY FUNCTION('YEAR', n.occurredOn), FUNCTION('MONTH', n.occurredOn), n.categoryId
            """)
    List<Object[]> sumByMonthAndCategory(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    long countByCategoryIdAndOccurredOnBetween(Long categoryId, LocalDate start, LocalDate end);

    long countByUserIdAndCategoryIdAndOccurredOnBetween(Long userId, Long categoryId, LocalDate start, LocalDate end);
}
