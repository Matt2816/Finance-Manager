package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.RecurringExpense;
import org.springframework.data.repository.ListCrudRepository;

public interface ExpenseRepo extends ListCrudRepository<RecurringExpense,Integer> {
}
