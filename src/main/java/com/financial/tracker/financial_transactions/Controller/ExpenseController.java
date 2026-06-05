package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.RecurringExpenseService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringExpense;
import com.financial.tracker.financial_transactions.repo.ExpenseRepo;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    private final RecurringExpenseService recurringExpenseService;
    private final ExpenseRepo expenseRepo;

    public ExpenseController(RecurringExpenseService recurringExpenseService, ExpenseRepo expenseRepo) {
        this.recurringExpenseService = recurringExpenseService;
        this.expenseRepo = expenseRepo;
    }

    @GetMapping
    public List<RecurringExpense> getAllRecurringExpenses() {
        ControllerRequestLogger.logIncoming(log, "getAllRecurringExpenses");
        Long userId = SecurityUtils.getCurrentUserId();
        return ControllerRequestLogger.logResponseBody(log, "getAllRecurringExpenses", expenseRepo.findByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpense> getRecurringExpenseById(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "getRecurringExpenseById", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        ResponseEntity<RecurringExpense> response = expenseRepo.findByIdAndUserId(Math.toIntExact(id), userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        return ControllerRequestLogger.logResponse(log, "getRecurringExpenseById", response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "deleteRecurringExpense", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        int expenseId = Math.toIntExact(id);
        ResponseEntity<Void> response;
        if (expenseRepo.existsByIdAndUserId(expenseId, userId)) {
            expenseRepo.deleteById(expenseId);
            response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            response = ResponseEntity.notFound().build();
        }
        return ControllerRequestLogger.logResponse(log, "deleteRecurringExpense", response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpense> updateRecurringExpense(
            @PathVariable Long id,
            @RequestBody RecurringExpense updatedExpense
    ) {
        ControllerRequestLogger.logIncoming(log, "updateRecurringExpense", "id", id, "body", updatedExpense);
        Long userId = SecurityUtils.getCurrentUserId();
        ResponseEntity<RecurringExpense> response = expenseRepo.findByIdAndUserId(Math.toIntExact(id), userId)
                .map(expense -> {
                    expense.setMerchant(updatedExpense.getMerchant());
                    expense.setAmount(updatedExpense.getAmount());
                    expense.setFrequency(updatedExpense.getFrequency());
                    expense.setStartDate(updatedExpense.getStartDate());
                    expense.setNextPaymentDate(updatedExpense.getNextPaymentDate());
                    expenseRepo.save(expense);
                    return new ResponseEntity<>(expense, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
        return ControllerRequestLogger.logResponse(log, "updateRecurringExpense", response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecurringExpense> addRecurringExpense(@RequestBody RecurringExpense recurringExpense) {
        ControllerRequestLogger.logIncoming(log, "addRecurringExpense", recurringExpense);
        Long userId = SecurityUtils.getCurrentUserId();
        recurringExpense.setUserId(userId);
        recurringExpense.setNextPaymentDate(
                calculateNextPaymentDate(recurringExpense.getStartDate(), recurringExpense.getFrequency())
        );
        expenseRepo.save(recurringExpense);
        return ControllerRequestLogger.logResponse(log, "addRecurringExpense",
                new ResponseEntity<>(recurringExpense, HttpStatus.CREATED));
    }

    @Profile("dev")
    @PostMapping("/testScheduler")
    public ResponseEntity<String> triggerExpenseScheduler() {
        ControllerRequestLogger.logIncoming(log, "triggerExpenseScheduler");
        recurringExpenseService.processRecurringExpenses();
        return ControllerRequestLogger.logResponse(log, "triggerExpenseScheduler",
                new ResponseEntity<>("Recurring Expense Scheduler triggered successfully", HttpStatus.OK));
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
