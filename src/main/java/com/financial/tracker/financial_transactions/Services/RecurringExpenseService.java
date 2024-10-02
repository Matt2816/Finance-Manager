package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringExpense;
import com.financial.tracker.financial_transactions.model.RecurringIncome;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.ExpenseRepo;
import com.financial.tracker.financial_transactions.repo.IncomeRepo;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringExpenseService {

    private final ExpenseRepo expenseRepo;
    private final TransactionsRepo transactionsRepo;

    public RecurringExpenseService(ExpenseRepo expenseRepo, TransactionsRepo transactionsRepo) {
        this.expenseRepo = expenseRepo;
        this.transactionsRepo = transactionsRepo;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void processRecurringExpenses() {
        List<RecurringExpense> recurringExpenses = expenseRepo.findAll();

        for (RecurringExpense expense : recurringExpenses) {
            LocalDate today = LocalDate.now();

            if (today.isEqual(expense.getNextPaymentDate()) || today.isAfter(expense.getNextPaymentDate())) {
                Transaction transaction = new Transaction();
                transaction.setMerchant(expense.getMerchant());
                transaction.setAmount(String.valueOf(expense.getAmount()));
                transaction.setTransactionDate(String.valueOf(today));

                transactionsRepo.save(transaction);

                expense.setNextPaymentDate(calculateNextPaymentDate(expense.getNextPaymentDate(), expense.getFrequency()));
                expenseRepo.save(expense);
            }
        }
    }

    private LocalDate calculateNextPaymentDate(LocalDate currentPaymentDate, Frequency frequency) {
        switch (frequency) {
            case DAILY:
                return currentPaymentDate.plusDays(1);
            case WEEKLY:
                return currentPaymentDate.plusWeeks(1);
            case BIWEEKLY:
                return currentPaymentDate.plusWeeks(2);
            case MONTHLY:
                return currentPaymentDate.plusMonths(1);
            case SEMIYEARLY:
                return currentPaymentDate.plusMonths(6);
            case YEARLY:
                return currentPaymentDate.plusYears(1);
            default:
                throw new IllegalArgumentException("Invalid frequency");
        }
    }
}
