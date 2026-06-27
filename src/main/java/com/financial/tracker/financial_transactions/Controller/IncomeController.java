package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.RecurringIncomeService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringIncome;
import com.financial.tracker.financial_transactions.repo.IncomeRepo;
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
@RequestMapping("/api/income")
public class IncomeController {

    private static final Logger log = LoggerFactory.getLogger(IncomeController.class);

    private final IncomeRepo incomeRepo;
    private final RecurringIncomeService recurringIncomeService;

    public IncomeController(IncomeRepo incomeRepo, RecurringIncomeService recurringIncomeService) {
        this.recurringIncomeService = recurringIncomeService;
        this.incomeRepo = incomeRepo;
    }

    @GetMapping
    public List<RecurringIncome> getAllRecurringIncomes() {
        ControllerRequestLogger.logIncoming(log, "getAllRecurringIncomes");
        Long userId = SecurityUtils.getCurrentUserId();
        return ControllerRequestLogger.logResponseBody(log, "getAllRecurringIncomes", incomeRepo.findByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringIncome> getRecurringIncomeById(@PathVariable Integer id) {
        ControllerRequestLogger.logIncoming(log, "getRecurringIncomeById", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        ResponseEntity<RecurringIncome> response = incomeRepo.findByIdAndUserId(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        return ControllerRequestLogger.logResponse(log, "getRecurringIncomeById", response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringIncome(@PathVariable Integer id) {
        ControllerRequestLogger.logIncoming(log, "deleteRecurringIncome", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        ResponseEntity<Void> response;
        if (incomeRepo.existsByIdAndUserId(id, userId)) {
            incomeRepo.deleteById(id);
            response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            response = ResponseEntity.notFound().build();
        }
        return ControllerRequestLogger.logResponse(log, "deleteRecurringIncome", response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringIncome> updateRecurringIncome(
            @PathVariable Integer id,
            @RequestBody RecurringIncome updatedIncome
    ) {
        ControllerRequestLogger.logIncoming(log, "updateRecurringIncome", "id", id, "body", updatedIncome);
        Long userId = SecurityUtils.getCurrentUserId();
        ResponseEntity<RecurringIncome> response = incomeRepo.findByIdAndUserId(id, userId)
                .map(income -> {
                    income.setIncomeSource(updatedIncome.getIncomeSource());
                    income.setAmount(updatedIncome.getAmount());
                    income.setFrequency(updatedIncome.getFrequency());
                    income.setStartDate(updatedIncome.getStartDate());
                    income.setNextPaymentDate(updatedIncome.getNextPaymentDate());
                    incomeRepo.save(income);
                    return new ResponseEntity<>(income, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
        return ControllerRequestLogger.logResponse(log, "updateRecurringIncome", response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecurringIncome> addRecurringIncome(@RequestBody RecurringIncome recurringIncome) {
        ControllerRequestLogger.logIncoming(log, "addRecurringIncome", recurringIncome);
        Long userId = SecurityUtils.getCurrentUserId();
        recurringIncome.setUserId(userId);
        recurringIncome.setNextPaymentDate(
                calculateNextPaymentDate(recurringIncome.getStartDate(), recurringIncome.getFrequency())
        );
        incomeRepo.save(recurringIncome);
        return ControllerRequestLogger.logResponse(log, "addRecurringIncome",
                new ResponseEntity<>(recurringIncome, HttpStatus.CREATED));
    }

    @Profile("dev")
    @PostMapping("/testScheduler")
    public ResponseEntity<String> triggerIncomeScheduler() {
        ControllerRequestLogger.logIncoming(log, "triggerIncomeScheduler");
        recurringIncomeService.processRecurringIncome();
        return ControllerRequestLogger.logResponse(log, "triggerIncomeScheduler",
                new ResponseEntity<>("Recurring Income Scheduler triggered successfully", HttpStatus.OK));
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
