package com.financial.tracker.financial_transactions.repo;

import com.financial.tracker.financial_transactions.model.Transaction;
import org.springframework.data.repository.ListCrudRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionsRepo extends ListCrudRepository<Transaction, Integer> {
    Transaction findByHash(String hash);

    List<Transaction> findByOccurredOnBetween(LocalDate start, LocalDate end);

    List<Transaction> findByOccurredOnIsNotNullOrderByOccurredOnAsc();

    List<Transaction> findByOccurredOnIsNull();

    List<Transaction> findByCategoryId(Long categoryId);

    List<Transaction> findByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Integer id, Long userId);

    Transaction findByHashAndUserId(String hash, Long userId);

    void deleteByUserId(Long userId);

    List<Transaction> findByUserIdAndOccurredOnBetween(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdAndOccurredOnIsNotNullOrderByOccurredOnAsc(Long userId);

    List<Transaction> findByUserIdAndOccurredOnIsNull(Long userId);

    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
