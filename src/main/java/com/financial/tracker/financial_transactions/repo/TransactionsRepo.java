package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.Transaction;
import org.springframework.data.repository.ListCrudRepository;

public interface TransactionsRepo extends ListCrudRepository<Transaction,Integer> {
    Transaction findByHash(String hash);
}
