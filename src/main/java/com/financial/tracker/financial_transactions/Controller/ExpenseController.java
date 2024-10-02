package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.RecurringExpenseService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringExpense;
import com.financial.tracker.financial_transactions.repo.ExpenseRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recurringExpense")
public class ExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    private final ExpenseRepo expenseRepo;

    public ExpenseController(RecurringExpenseService recurringExpenseService, ExpenseRepo expenseRepo) {
        this.recurringExpenseService = recurringExpenseService;
        this.expenseRepo = expenseRepo;
    }

    @GetMapping("/getAll")
    public List<RecurringExpense> getAllRecurringExpenses() {
        return expenseRepo.findAll();
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<RecurringExpense> getRecurringExpenseById(@PathVariable Long id) {
        return expenseRepo.findById(Math.toIntExact(id))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        if (expenseRepo.existsById(Math.toIntExact(id))) {
            expenseRepo.deleteById(Math.toIntExact(id));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpense(@PathVariable Long id, @RequestBody RecurringExpense updatedExpense) {
        return expenseRepo.findById(Math.toIntExact(id))
                .map(expense -> {
                    expense.setMerchant(updatedExpense.getMerchant());
                    expense.setAmount(updatedExpense.getAmount());
                    expense.setFrequency(updatedExpense.getFrequency());
                    expense.setStartDate(updatedExpense.getStartDate());
                    expense.setNextPaymentDate(updatedExpense.getNextPaymentDate());
                    expenseRepo.save(expense);
                    return new ResponseEntity<>(expense, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecurringExpense> addRecurringExpense(@RequestBody RecurringExpense recurringExpense) {
        recurringExpense.setNextPaymentDate(calculateNextPaymentDate(recurringExpense.getStartDate(), recurringExpense.getFrequency()));
        expenseRepo.save(recurringExpense);
        return new ResponseEntity<>(recurringExpense, HttpStatus.CREATED);
    }

    @PostMapping("/testScheduler")
    public ResponseEntity<String> triggerExpenseScheduler() {
        recurringExpenseService.processRecurringExpenses();
        return new ResponseEntity<>("Recurring Expense Scheduler triggered successfully", HttpStatus.OK);
    }

    private LocalDate calculateNextPaymentDate(LocalDate startDate, Frequency frequency) {
        switch (frequency) {
            case DAILY:
                return startDate.plusDays(1);
            case WEEKLY:
                return startDate.plusWeeks(1);
            case BIWEEKLY:
                return startDate.plusWeeks(2);
            case MONTHLY:
                return startDate.plusMonths(1);
            case SEMIYEARLY:
                return startDate.plusMonths(6);
            case YEARLY:
                return startDate.plusYears(1);
            default:
                throw new IllegalArgumentException("Invalid frequency");
        }
    }
}


