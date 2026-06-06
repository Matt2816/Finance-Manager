package com.financial.tracker.financial_transactions.analytics.normalization;

import com.financial.tracker.financial_transactions.analytics.merchant.MerchantNormalizer;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.util.TransactionFieldParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionNormalizationService {

    private final TransactionsRepo transactionsRepo;
    private final NormalizedTransactionRepository normalizedRepo;
    private final MerchantNormalizer merchantNormalizer;

    public TransactionNormalizationService(
            TransactionsRepo transactionsRepo,
            NormalizedTransactionRepository normalizedRepo,
            MerchantNormalizer merchantNormalizer
    ) {
        this.transactionsRepo = transactionsRepo;
        this.normalizedRepo = normalizedRepo;
        this.merchantNormalizer = merchantNormalizer;
    }

    @Transactional
    public ReconcileResult reconcileAll(Long userId) {
        int normalized = 0;
        int skipped = 0;
        for (Transaction transaction : transactionsRepo.findByUserId(userId)) {
            if (normalizeTransaction(transaction).isPresent()) {
                normalized++;
            } else {
                skipped++;
            }
        }
        return new ReconcileResult(normalized, skipped);
    }

    @Transactional
    public Optional<NormalizedTransaction> normalizeOne(int transactionId) {
        return transactionsRepo.findById(transactionId)
                .flatMap(this::normalizeTransaction);
    }

    private Optional<NormalizedTransaction> normalizeTransaction(Transaction transaction) {
        TransactionFieldParser.applyTypedFields(transaction);
        if (transaction.getOccurredOn() == null || transaction.getAmountValue() == null) {
            return Optional.empty();
        }
        transactionsRepo.save(transaction);

        String merchantRaw = resolveMerchantRaw(transaction);
        String merchantKey = merchantNormalizer.normalize(merchantRaw);

        NormalizedTransaction normalized = normalizedRepo.findByTransactionId(transaction.getId())
                .orElse(new NormalizedTransaction());
        normalized.setUserId(transaction.getUserId());
        normalized.setTransactionId(transaction.getId());
        normalized.setOccurredOn(transaction.getOccurredOn());
        BigDecimal originalAmount = transaction.getAmountValue();
        normalized.setAmount(originalAmount.abs());
        normalized.setDirection(
                originalAmount != null && originalAmount.compareTo(BigDecimal.ZERO) < 0
                        ? TransactionDirection.CREDIT
                        : TransactionDirection.DEBIT
        );
        normalized.setMerchantRaw(merchantRaw);
        normalized.setMerchantKey(merchantKey);
        normalized.setCategoryId(transaction.getCategoryId());
        normalized.setRecurringGenerated(transaction.isRecurringGenerated());
        normalized.setNormalizedAt(ZonedDateTime.now());

        return Optional.of(normalizedRepo.save(normalized));
    }

    private static String resolveMerchantRaw(Transaction transaction) {
        String merchant = transaction.getMerchant();
        if (merchant != null && !merchant.isBlank()) {
            return merchant.trim();
        }
        return transaction.getName() == null ? "" : transaction.getName().trim();
    }

    public record ReconcileResult(int normalized, int skipped) {}
}
