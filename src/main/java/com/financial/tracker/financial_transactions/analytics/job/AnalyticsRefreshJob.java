package com.financial.tracker.financial_transactions.analytics.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AnalyticsRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRefreshJob.class);

    private final AnalyticsRefreshOrchestrator orchestrator;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AnalyticsRefreshJob(AnalyticsRefreshOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(cron = "${analytics.refresh.cron:0 30 2 * * ?}")
    public void scheduledRefresh() {
        runRefreshInternal();
    }

    @Async
    public void triggerAsync() {
        runRefreshInternal();
    }

    public boolean runRefreshInternal() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Analytics refresh already running, skipping");
            return false;
        }
        try {
            orchestrator.runRefresh();
            return true;
        } finally {
            running.set(false);
        }
    }
}
