package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.RecurringIncomeService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringIncome;
import com.financial.tracker.financial_transactions.repo.IncomeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public IncomeController(IncomeRepo incomeRepo, RecurringIncomeService recurringIncomeService, IncomeRepo recurringIncomeRepo) {
        this.recurringIncomeService = recurringIncomeService;
        this.incomeRepo = incomeRepo;
    }

    @GetMapping
    public List<RecurringIncome> getAllRecurringIncomes() {
        ControllerRequestLogger.logIncoming(log, "getAllRecurringIncomes");
        return ControllerRequestLogger.logResponseBody(log, "getAllRecurringIncomes", incomeRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringIncome> getRecurringIncomeById(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "getRecurringIncomeById", "id", id);
        ResponseEntity<RecurringIncome> response = incomeRepo.findById(Math.toIntExact(id))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        return ControllerRequestLogger.logResponse(log, "getRecurringIncomeById", response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringIncome(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "deleteRecurringIncome", "id", id);
        ResponseEntity<Void> response;
        if (incomeRepo.existsById(Math.toIntExact(id))) {
            incomeRepo.deleteById(Math.toIntExact(id));
            response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ControllerRequestLogger.logResponse(log, "deleteRecurringIncome", response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringIncome> updateRecurringIncome(
            @PathVariable Long id,
            @RequestBody RecurringIncome updatedIncome
    ) {
        ControllerRequestLogger.logIncoming(log, "updateRecurringIncome", "id", id, "body", updatedIncome);
        ResponseEntity<RecurringIncome> response = incomeRepo.findById(Math.toIntExact(id))
                .map(income -> {
                    income.setIncomeSource(updatedIncome.getIncomeSource());
                    income.setAmount(updatedIncome.getAmount());
                    income.setFrequency(updatedIncome.getFrequency());
                    income.setStartDate(updatedIncome.getStartDate());
                    income.setNextPaymentDate(updatedIncome.getNextPaymentDate());
                    incomeRepo.save(income);
                    return new ResponseEntity<>(income, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        return ControllerRequestLogger.logResponse(log, "updateRecurringIncome", response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecurringIncome> addRecurringIncome(@RequestBody RecurringIncome recurringIncome) {
        ControllerRequestLogger.logIncoming(log, "addRecurringIncome", recurringIncome);
        recurringIncome.setNextPaymentDate(
                calculateNextPaymentDate(recurringIncome.getStartDate(), recurringIncome.getFrequency())
        );
        incomeRepo.save(recurringIncome);
        return ControllerRequestLogger.logResponse(log, "addRecurringIncome",
                new ResponseEntity<>(recurringIncome, HttpStatus.CREATED));
    }

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
