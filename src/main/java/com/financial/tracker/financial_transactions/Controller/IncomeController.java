package com.financial.tracker.financial_transactions.Controller;

import com.financial.tracker.financial_transactions.Services.RecurringIncomeService;
import com.financial.tracker.financial_transactions.model.Frequency;
import com.financial.tracker.financial_transactions.model.RecurringIncome;
import com.financial.tracker.financial_transactions.repo.IncomeRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recurringIncome")
public class IncomeController {

    private final IncomeRepo incomeRepo;
    private final RecurringIncomeService recurringIncomeService;

    public IncomeController(IncomeRepo incomeRepo, RecurringIncomeService recurringIncomeService, IncomeRepo recurringIncomeRepo) {
        this.recurringIncomeService = recurringIncomeService;
        this.incomeRepo = incomeRepo;
    }

    @GetMapping("/getAll")
    public List<RecurringIncome> getAllRecurringIncomes() {
        return incomeRepo.findAll();
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<RecurringIncome> getRecurringIncomeById(@PathVariable Long id) {
        return incomeRepo.findById(Math.toIntExact(id))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteRecurringIncome(@PathVariable Long id) {
        if (incomeRepo.existsById(Math.toIntExact(id))) {
            incomeRepo.deleteById(Math.toIntExact(id));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<RecurringIncome> updateRecurringIncome(@PathVariable Long id, @RequestBody RecurringIncome updatedIncome) {
        return incomeRepo.findById(Math.toIntExact(id))
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
    }
    
    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecurringIncome> addRecurringIncome(@RequestBody RecurringIncome recurringIncome) {
        recurringIncome.setNextPaymentDate(calculateNextPaymentDate(recurringIncome.getStartDate(), recurringIncome.getFrequency()));
        incomeRepo.save(recurringIncome);
        return new ResponseEntity<>(recurringIncome, HttpStatus.CREATED);
    }

    @PostMapping("/testScheduler")
    public ResponseEntity<String> triggerIncomeScheduler() {
        recurringIncomeService.processRecurringIncome();
        return new ResponseEntity<>("Recurring Income Scheduler triggered successfully", HttpStatus.OK);
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
