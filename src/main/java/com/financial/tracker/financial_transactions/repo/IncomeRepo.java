package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.RecurringIncome;
import org.springframework.data.repository.ListCrudRepository;
import java.util.List;
import java.util.Optional;

public interface IncomeRepo extends ListCrudRepository<RecurringIncome, Integer> {

    List<RecurringIncome> findByUserId(Long userId);

    Optional<RecurringIncome> findByIdAndUserId(Integer id, Long userId);

    boolean existsByIdAndUserId(Integer id, Long userId);
}
