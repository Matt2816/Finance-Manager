package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringIncome;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.IncomeRepo;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.util.RecurringTransactionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringIncomeService {

    private final IncomeRepo incomeRepo;
    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;
    private final UserRepository userRepository;

    public RecurringIncomeService(
            IncomeRepo incomeRepo,
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            UserRepository userRepository
    ) {
        this.incomeRepo = incomeRepo;
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void processRecurringIncome() {
        for (User user : userRepository.findAll()) {
            processRecurringIncomeForUser(user.getId());
        }
    }

    public void processRecurringIncomeForUser(Long userId) {
        List<RecurringIncome> recurringIncomes = incomeRepo.findByUserId(userId);

        for (RecurringIncome income : recurringIncomes) {
            LocalDate today = LocalDate.now();

            if (income.getNextPaymentDate().isBefore(today) || income.getNextPaymentDate().isEqual(today)) {
                Transaction transaction = RecurringTransactionFactory.createIncome(
                        income.getIncomeSource(),
                        income.getAmount(),
                        today
                );
                transaction.setUserId(userId);

                Transaction saved = transactionsRepo.save(transaction);
                normalizationService.normalizeOne(saved.getId());

                income.setNextPaymentDate(calculateNextPaymentDate(income.getNextPaymentDate(), income.getFrequency()));
                incomeRepo.save(income);
            }
        }
    }

    private LocalDate calculateNextPaymentDate(LocalDate currentPaymentDate, Frequency frequency) {
        return switch (frequency) {
            case DAILY -> currentPaymentDate.plusDays(1);
            case WEEKLY -> currentPaymentDate.plusWeeks(1);
            case BIWEEKLY -> currentPaymentDate.plusWeeks(2);
            case MONTHLY -> currentPaymentDate.plusMonths(1);
            case SEMIYEARLY -> currentPaymentDate.plusMonths(6);
            case YEARLY -> currentPaymentDate.plusYears(1);
        };
    }
}
