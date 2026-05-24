package com.financial.tracker.financial_transactions.analytics.normalization;

import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
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

    public TransactionBackfillService(TransactionsRepo transactionsRepo) {
        this.transactionsRepo = transactionsRepo;
    }

    @Transactional
    public BackfillResult backfillAll() {
        List<Transaction> needsBackfill = transactionsRepo.findByOccurredOnIsNull();
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
