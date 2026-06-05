package com.financial.tracker.financial_transactions.analytics.recurring;

import com.financial.tracker.financial_transactions.analytics.model.RecurringTransaction;
import com.financial.tracker.financial_transactions.analytics.model.RecurrenceFrequency;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.analytics.repo.RecurringTransactionRepository;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecurringTransactionGenerator {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionGenerator.class);

    private final RecurringTransactionRepository recurringRepo;
    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;
    private final UserRepository userRepository;

    public RecurringTransactionGenerator(
            RecurringTransactionRepository recurringRepo,
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            UserRepository userRepository
    ) {
        this.recurringRepo = recurringRepo;
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void dailyGeneration() {
        log.info("Running daily recurring transaction generation...");
        for (User user : userRepository.findAll()) {
            List<RecurringTransaction> active = recurringRepo.findByUserIdAndActiveTrue(user.getId());
            for (RecurringTransaction rt : active) {
                generateFor(rt);
            }
        }
        log.info("Daily recurring generation complete.");
    }

    @Transactional
    public void generateFor(RecurringTransaction rt) {
        LocalDate today = LocalDate.now();
        if (rt.getStartDate().isAfter(today)) {
            return; // hasn't started yet
        }
        if (rt.getEndDate() != null && rt.getEndDate().isBefore(today)) {
            return; // already ended
        }

        LocalDate current = rt.getLastGeneratedDate() != null
                ? nextDate(rt.getLastGeneratedDate(), rt.getFrequency())
                : rt.getStartDate();

        List<Transaction> generated = new ArrayList<>();
        while (!current.isAfter(today) && (rt.getEndDate() == null || !current.isAfter(rt.getEndDate()))) {
            Transaction tx = createTransaction(rt, current);
            generated.add(tx);
            current = nextDate(current, rt.getFrequency());
        }

        if (!generated.isEmpty()) {
            transactionsRepo.saveAll(generated);
            for (Transaction tx : generated) {
                normalizationService.normalizeOne(tx.getId());
            }
            rt.setLastGeneratedDate(current.minusDays(1)); // last date that was actually generated
            recurringRepo.save(rt);
            log.info("Generated {} transactions for recurring {}", generated.size(), rt.getId());
        }
    }

    private Transaction createTransaction(RecurringTransaction rt, LocalDate date) {
        Transaction tx = new Transaction();
        tx.setUserId(rt.getUserId());
        tx.setCardType(rt.getCardType() != null ? rt.getCardType() : "other");

        // For DEBIT (expense), store as positive amount string.
        // For CREDIT (income), store as negative amount string to match existing normalization logic.
        BigDecimal storedAmount = rt.getAmount();
        if (rt.getDirection() == TransactionDirection.CREDIT) {
            storedAmount = storedAmount.negate();
        }
        tx.setAmount(storedAmount.toPlainString());

        tx.setName(rt.getName());
        tx.setMerchant(rt.getName());
        tx.setTransactionDate(date.toString());
        tx.setHash(generateHash(rt.getId(), date));
        tx.setAddress("");
        tx.setCategoryId(rt.getCategoryId());
        tx.setRecurringGenerated(true);
        tx.setRecurringParentId(rt.getId().intValue());
        return tx;
    }

    private String generateHash(Long recurringId, LocalDate date) {
        return "recurring-" + recurringId + "-" + date.toString() + "-" + System.nanoTime();
    }

    private LocalDate nextDate(LocalDate current, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
        };
    }
}
