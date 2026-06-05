package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.controller.dto.AnalyticsStatusDto;
import com.financial.tracker.financial_transactions.analytics.job.AnalyticsRefreshJob;
import com.financial.tracker.financial_transactions.analytics.model.NormalizedTransaction;
import com.financial.tracker.financial_transactions.analytics.model.TransactionDirection;
import com.financial.tracker.financial_transactions.analytics.repo.AnalyticsRefreshRunRepository;
import com.financial.tracker.financial_transactions.analytics.repo.NormalizedTransactionRepository;
import com.financial.tracker.financial_transactions.Controller.ControllerRequestLogger;
import com.financial.tracker.financial_transactions.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsRefreshRunRepository refreshRunRepository;
    private final AnalyticsRefreshJob refreshJob;
    private final NormalizedTransactionRepository normalizedRepo;

    public AnalyticsController(
            AnalyticsRefreshRunRepository refreshRunRepository,
            AnalyticsRefreshJob refreshJob,
            NormalizedTransactionRepository normalizedRepo
    ) {
        this.refreshRunRepository = refreshRunRepository;
        this.refreshJob = refreshJob;
        this.normalizedRepo = normalizedRepo;
    }

    @GetMapping("/status")
    public AnalyticsStatusDto status() {
        ControllerRequestLogger.logIncoming(log, "status");
        Long userId = SecurityUtils.getCurrentUserId();
        return refreshRunRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
                .map(AnalyticsStatusDto::from)
                .orElse(AnalyticsStatusDto.from(null));
    }

    @GetMapping("/income-summary")
    public IncomeSummaryDto incomeSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        ControllerRequestLogger.logIncoming(log, "incomeSummary", "from", from, "to", to);
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate start = from != null && !from.isBlank() ? LocalDate.parse(from) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null && !to.isBlank() ? LocalDate.parse(to) : LocalDate.now();

        List<NormalizedTransaction> txs = normalizedRepo.findByUserIdAndOccurredOnBetween(userId, start, end);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (NormalizedTransaction tx : txs) {
            if (tx.getDirection() == TransactionDirection.CREDIT) {
                totalIncome = totalIncome.add(tx.getAmount());
            } else if (tx.getDirection() == TransactionDirection.DEBIT) {
                totalExpenses = totalExpenses.add(tx.getAmount());
            }
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        BigDecimal savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netSavings.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new IncomeSummaryDto(
                totalIncome.doubleValue(),
                totalExpenses.doubleValue(),
                netSavings.doubleValue(),
                savingsRate.doubleValue()
        );
    }

    public record IncomeSummaryDto(double totalIncome, double totalExpenses, double netSavings, double savingsRate) {}

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh() {
        ControllerRequestLogger.logIncoming(log, "refresh");
        Long userId = SecurityUtils.getCurrentUserId();
        refreshJob.triggerAsync(userId);
        return ResponseEntity.accepted().body(Map.of("status", "accepted", "message", "Analytics refresh started"));
    }
}
