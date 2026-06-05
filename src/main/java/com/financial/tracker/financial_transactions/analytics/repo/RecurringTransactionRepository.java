package com.financial.tracker.financial_transactions.analytics.repo;

import com.financial.tracker.financial_transactions.analytics.model.RecurringTransaction;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends ListCrudRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByActiveTrue();

    List<RecurringTransaction> findByActiveFalse();

    List<RecurringTransaction> findByDirection(TransactionDirection direction);

    List<RecurringTransaction> findByDirectionAndActiveTrue(TransactionDirection direction);

    List<RecurringTransaction> findByDirectionAndActiveFalse(TransactionDirection direction);

    List<RecurringTransaction> findByUserId(Long userId);

    Optional<RecurringTransaction> findByIdAndUserId(Long id, Long userId);

    List<RecurringTransaction> findByUserIdAndActiveTrue(Long userId);

    List<RecurringTransaction> findByUserIdAndActiveFalse(Long userId);

    List<RecurringTransaction> findByUserIdAndDirection(Long userId, TransactionDirection direction);

    List<RecurringTransaction> findByUserIdAndDirectionAndActiveTrue(Long userId, TransactionDirection direction);

    List<RecurringTransaction> findByUserIdAndDirectionAndActiveFalse(Long userId, TransactionDirection direction);
}
