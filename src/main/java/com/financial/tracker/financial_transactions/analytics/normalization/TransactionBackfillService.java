package com.financial.tracker.financial_transactions.analytics.normalization;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.util.TransactionFieldParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TransactionBackfillService {

    private static final Logger log = LoggerFactory.getLogger(TransactionBackfillService.class);

    private final TransactionsRepo transactionsRepo;
    private final UserRepository userRepository;

    public TransactionBackfillService(TransactionsRepo transactionsRepo, UserRepository userRepository) {
        this.transactionsRepo = transactionsRepo;
        this.userRepository = userRepository;
    }

    public void backfillAll() {
        for (User user : userRepository.findAll()) {
            backfillAll(user.getId());
        }
    }

    @Transactional
    public BackfillResult backfillAll(Long userId) {
        List<Transaction> needsBackfill = transactionsRepo.findByUserIdAndOccurredOnIsNull(userId);
        int updated = 0;
        int failed = 0;

        for (Transaction transaction : needsBackfill) {
            TransactionFieldParser.applyTypedFields(transaction);
            if (transaction.getOccurredOn() == null || transaction.getAmountValue() == null) {
                failed++;
                log.warn("Backfill failed for transaction id={}", transaction.getId());
                continue;
            }
            transactionsRepo.save(transaction);
            updated++;
        }

        log.info("Transaction backfill complete: updated={}, failed={}", updated, failed);
        return new BackfillResult(updated, failed);
    }

    public record BackfillResult(int updated, int failed) {}
}
