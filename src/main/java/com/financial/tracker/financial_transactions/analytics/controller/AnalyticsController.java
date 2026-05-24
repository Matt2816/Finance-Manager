package com.financial.tracker.financial_transactions.analytics.controller;

import com.financial.tracker.financial_transactions.analytics.controller.dto.AnalyticsStatusDto;
import com.financial.tracker.financial_transactions.analytics.job.AnalyticsRefreshJob;
import com.financial.tracker.financial_transactions.analytics.repo.AnalyticsRefreshRunRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsRefreshRunRepository refreshRunRepository;
    private final AnalyticsRefreshJob refreshJob;

    public AnalyticsController(
            AnalyticsRefreshRunRepository refreshRunRepository,
            AnalyticsRefreshJob refreshJob
    ) {
        this.refreshRunRepository = refreshRunRepository;
        this.refreshJob = refreshJob;
    }

    @GetMapping("/status")
    public AnalyticsStatusDto status() {
        return refreshRunRepository.findFirstByOrderByStartedAtDesc()
                .map(AnalyticsStatusDto::from)
                .orElse(AnalyticsStatusDto.from(null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh() {
        refreshJob.triggerAsync();
        return ResponseEntity.accepted().body(Map.of("status", "accepted", "message", "Analytics refresh started"));
    }
}
