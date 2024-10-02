package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.RecurringIncome;
import org.springframework.data.repository.ListCrudRepository;

public interface IncomeRepo extends ListCrudRepository<RecurringIncome,Integer> {
}