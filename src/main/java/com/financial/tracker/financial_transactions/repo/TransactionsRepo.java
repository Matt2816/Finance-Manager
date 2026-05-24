package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.Transaction;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionsRepo extends ListCrudRepository<Transaction, Integer> {
    Transaction findByHash(String hash);

    List<Transaction> findByOccurredOnBetween(LocalDate start, LocalDate end);

    List<Transaction> findByOccurredOnIsNotNullOrderByOccurredOnAsc();

    List<Transaction> findByOccurredOnIsNull();
}
