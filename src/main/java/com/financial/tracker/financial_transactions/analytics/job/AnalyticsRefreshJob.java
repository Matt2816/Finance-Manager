package com.financial.tracker.financial_transactions.analytics.job;

import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.repo.UserRepository;
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
    private final UserRepository userRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AnalyticsRefreshJob(AnalyticsRefreshOrchestrator orchestrator, UserRepository userRepository) {
        this.orchestrator = orchestrator;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${analytics.refresh.cron:0 30 2 * * ?}")
    public void scheduledRefresh() {
        runRefreshInternal();
    }

    @Async
    public void triggerAsync() {
        runRefreshInternal();
    }

    @Async
    public void triggerAsync(Long userId) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Analytics refresh already running, skipping user {}", userId);
            return;
        }
        try {
            orchestrator.runRefresh(userId);
        } finally {
            running.set(false);
        }
    }

    public boolean runRefreshInternal() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Analytics refresh already running, skipping");
            return false;
        }
        try {
            for (User user : userRepository.findAll()) {
                orchestrator.runRefresh(user.getId());
            }
            return true;
        } finally {
            running.set(false);
        }
    }
}
