package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.analytics.normalization.TransactionNormalizationService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringExpense;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.ExpenseRepo;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import com.financial.tracker.financial_transactions.repo.UserRepository;
import com.financial.tracker.financial_transactions.util.RecurringTransactionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringExpenseService {

    private final ExpenseRepo expenseRepo;
    private final TransactionsRepo transactionsRepo;
    private final TransactionNormalizationService normalizationService;
    private final UserRepository userRepository;

    public RecurringExpenseService(
            ExpenseRepo expenseRepo,
            TransactionsRepo transactionsRepo,
            TransactionNormalizationService normalizationService,
            UserRepository userRepository
    ) {
        this.expenseRepo = expenseRepo;
        this.transactionsRepo = transactionsRepo;
        this.normalizationService = normalizationService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void processRecurringExpenses() {
        for (User user : userRepository.findAll()) {
            processRecurringExpensesForUser(user.getId());
        }
    }

    public void processRecurringExpensesForUser(Long userId) {
        List<RecurringExpense> recurringExpenses = expenseRepo.findByUserId(userId);

        for (RecurringExpense expense : recurringExpenses) {
            LocalDate today = LocalDate.now();

            if (expense.getNextPaymentDate().isBefore(today) || expense.getNextPaymentDate().isEqual(today)) {
                Transaction transaction = RecurringTransactionFactory.createExpense(
                        expense.getMerchant(),
                        expense.getAmount(),
                        today
                );
                transaction.setUserId(userId);

                Transaction saved = transactionsRepo.save(transaction);
                normalizationService.normalizeOne(saved.getId());

                expense.setNextPaymentDate(calculateNextPaymentDate(expense.getNextPaymentDate(), expense.getFrequency()));
                expenseRepo.save(expense);
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
