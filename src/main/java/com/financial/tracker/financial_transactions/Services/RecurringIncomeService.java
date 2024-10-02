package com.financial.tracker.financial_transactions.Services;

import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringIncome;
import com.financial.tracker.financial_transactions.model.Transaction;
import com.financial.tracker.financial_transactions.repo.IncomeRepo;
import com.financial.tracker.financial_transactions.repo.TransactionsRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringIncomeService {

    private final IncomeRepo incomeRepo;
    private final TransactionsRepo transactionsRepo;

    public RecurringIncomeService(IncomeRepo recurringIncomeRepo, TransactionsRepo transactionsRepo) {
        this.incomeRepo = recurringIncomeRepo;
        this.transactionsRepo = transactionsRepo;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void processRecurringIncome() {
        List<RecurringIncome> recurringIncomes = incomeRepo.findAll();

        for (RecurringIncome income : recurringIncomes) {
            LocalDate today = LocalDate.now();

            if (today.isEqual(income.getNextPaymentDate()) || today.isAfter(income.getNextPaymentDate())) {
                Transaction transaction = new Transaction();
                transaction.setMerchant(income.getIncomeSource());
                transaction.setAmount(String.valueOf(income.getAmount()));
                transaction.setTransactionDate(String.valueOf(today));

                transactionsRepo.save(transaction);

                income.setNextPaymentDate(calculateNextPaymentDate(income.getNextPaymentDate(), income.getFrequency()));
                incomeRepo.save(income);
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
