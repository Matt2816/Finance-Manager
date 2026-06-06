package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.model.RecurringTransaction;
import com.financial.tracker.financial_transactions.analytics.model.RecurrenceFrequency;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.recurring.RecurringTransactionGenerator;
import com.financial.tracker.financial_transactions.analytics.repo.RecurringTransactionRepository;
import com.financial.tracker.financial_transactions.Controller.ControllerRequestLogger;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recurring")
public class RecurringTransactionController {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionController.class);

    private final RecurringTransactionRepository repository;
    private final RecurringTransactionGenerator generator;

    public RecurringTransactionController(
            RecurringTransactionRepository repository,
            RecurringTransactionGenerator generator
    ) {
        this.repository = repository;
        this.generator = generator;
    }

    @GetMapping
    public List<RecurringTransaction> list(
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "active", required = false) Boolean active
    ) {
        ControllerRequestLogger.logIncoming(log, "list", "direction", direction, "active", active);
        Long userId = SecurityUtils.getCurrentUserId();
        if (direction != null && !direction.isBlank()) {
            try {
                TransactionDirection d = TransactionDirection.valueOf(direction.toUpperCase());
                if (active != null) {
                    return active
                            ? repository.findByUserIdAndDirectionAndActiveTrue(userId, d)
                            : repository.findByUserIdAndDirectionAndActiveFalse(userId, d);
                }
                return repository.findByUserIdAndDirection(userId, d);
            } catch (IllegalArgumentException e) {
                if (active != null) {
                    return active
                            ? repository.findByUserIdAndActiveTrue(userId)
                            : repository.findByUserIdAndActiveFalse(userId);
                }
                return repository.findByUserId(userId);
            }
        }
        if (active != null) {
            return active ? repository.findByUserIdAndActiveTrue(userId) : repository.findByUserIdAndActiveFalse(userId);
        }
        return repository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringTransaction> getById(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "getById", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        return repository.findByIdAndUserId(id, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RecurringTransaction> create(@RequestBody CreateRecurringRequest request) {
        ControllerRequestLogger.logIncoming(log, "create", "name", request.name, "amount", request.amount);
        if (request.name == null || request.name.isBlank()
                || request.amount == null
                || request.frequency == null
                || request.startDate == null) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = SecurityUtils.getCurrentUserId();
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(userId);
        rt.setName(request.name.trim());
        rt.setAmount(request.amount.abs());
        rt.setDirection(request.direction != null ? request.direction : TransactionDirection.DEBIT);
        rt.setFrequency(request.frequency);
        rt.setStartDate(request.startDate);
        rt.setEndDate(request.endDate);
        rt.setCategoryId(request.categoryId);
        rt.setCardType(request.cardType);
        rt.setActive(true);
        rt.setCreatedAt(ZonedDateTime.now());

        RecurringTransaction saved = repository.save(rt);
        log.info("Created recurring transaction {}", saved.getId());

        generator.generateFor(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransaction> update(
            @PathVariable Long id,
            @RequestBody CreateRecurringRequest request
    ) {
        ControllerRequestLogger.logIncoming(log, "update", "id", id, "name", request.name);
        Long userId = SecurityUtils.getCurrentUserId();
        return repository.findByIdAndUserId(id, userId)
                .map(rt -> {
                    if (request.name == null || request.name.isBlank()
                            || request.amount == null
                            || request.frequency == null
                            || request.startDate == null) {
                        return ResponseEntity.badRequest().<RecurringTransaction>build();
                    }

                    rt.setName(request.name.trim());
                    rt.setAmount(request.amount.abs());
                    rt.setDirection(request.direction != null ? request.direction : TransactionDirection.DEBIT);
                    rt.setFrequency(request.frequency);
                    rt.setStartDate(request.startDate);
                    rt.setEndDate(request.endDate);
                    rt.setCategoryId(request.categoryId);
                    rt.setCardType(request.cardType);

                    RecurringTransaction saved = repository.save(rt);
                    log.info("Updated recurring transaction {}", saved.getId());
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "delete", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        return repository.findByIdAndUserId(id, userId)
                .map(rt -> {
                    rt.setActive(false);
                    repository.save(rt);
                    log.info("Soft-deleted (paused) recurring transaction {}", id);
                    return ResponseEntity.ok("Paused");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<String> resume(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "resume", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        return repository.findByIdAndUserId(id, userId)
                .map(rt -> {
                    rt.setActive(true);
                    repository.save(rt);
                    log.info("Resumed recurring transaction {}", id);
                    return ResponseEntity.ok("Resumed");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/generate-now")
    public ResponseEntity<String> generateNow(@PathVariable Long id) {
        ControllerRequestLogger.logIncoming(log, "generateNow", "id", id);
        Long userId = SecurityUtils.getCurrentUserId();
        return repository.findByIdAndUserId(id, userId)
                .map(rt -> {
                    generator.generateFor(rt);
                    return ResponseEntity.ok("Generated");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record CreateRecurringRequest(
            String name,
            BigDecimal amount,
            TransactionDirection direction,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            String cardType
    ) {}
}
