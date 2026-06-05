package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.analytics.merchant.MerchantIntelligenceEngine;
import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.util.TransactionFieldParser;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportPipeline {

    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;
    private final MerchantIntelligenceEngine merchantEngine;

    public TransactionImportPipeline(
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            MerchantIntelligenceEngine merchantEngine
    ) {
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.merchantEngine = merchantEngine;
    }

    public Transaction saveNew(Transaction transaction, Long userId) {
        TransactionFieldParser.applyTypedFields(transaction);
        transaction.setUserId(userId);
        Transaction saved = transactionsRepo.save(transaction);
        normalizeAndClassify(saved.getId(), userId);
        return saved;
    }

    public Transaction updateExisting(Transaction existing, Transaction mapped, Long userId) {
        TransactionFieldParser.applyTypedFields(mapped);
        existing.setName(mapped.getName());
        existing.setMerchant(mapped.getMerchant());
        existing.setAmount(mapped.getAmount());
        existing.setAmountValue(mapped.getAmountValue());
        existing.setOccurredOn(mapped.getOccurredOn());
        existing.setTransactionDate(mapped.getTransactionDate());
        existing.setCurrency(mapped.getCurrency());
        existing.setAddress(mapped.getAddress());
        existing.setCardType(mapped.getCardType());
        Transaction saved = transactionsRepo.save(existing);
        normalizeAndClassify(saved.getId(), userId);
        return saved;
    }

    public void normalizeAndClassify(int transactionId, Long userId) {
        normalizationService.normalizeOne(transactionId)
                .ifPresent(normalized -> merchantEngine.classifyOne(normalized, userId));
    }

    public boolean fieldsDiffer(Transaction existing, Transaction mapped) {
        return !safeEquals(existing.getName(), mapped.getName())
                || !safeEquals(existing.getMerchant(), mapped.getMerchant())
                || !safeEquals(existing.getAmount(), mapped.getAmount())
                || !safeEquals(existing.getAmountValue(), mapped.getAmountValue())
                || !safeEquals(existing.getOccurredOn(), mapped.getOccurredOn())
                || !safeEquals(existing.getCurrency(), mapped.getCurrency())
                || !safeEquals(existing.getAddress(), mapped.getAddress());
    }

    private static boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
