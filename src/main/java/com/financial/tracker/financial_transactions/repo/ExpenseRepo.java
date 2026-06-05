package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.RecurringExpense;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepo extends ListCrudRepository<RecurringExpense, Integer> {

    List<RecurringExpense> findByUserId(Long userId);

    Optional<RecurringExpense> findByIdAndUserId(Integer id, Long userId);

    boolean existsByIdAndUserId(Integer id, Long userId);
}
