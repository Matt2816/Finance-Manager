package com.financial.tracker.financial_transactions.splitwise;

import com.financial.tracker.financial_transactions.model.User;
import com.financial.tracker.financial_transactions.splitwise.config.SplitwiseProperties;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SplitwisePollingJob {

    private static final Logger log = LoggerFactory.getLogger(SplitwisePollingJob.class);

    private final SplitwiseImportService importService;
    private final SplitwiseProperties properties;

    public SplitwisePollingJob(SplitwiseImportService importService, SplitwiseProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @Scheduled(cron = "${splitwise.polling.cron:0 0 * * * ?}")
    @SchedulerLock(name = "splitwise-polling", lockAtLeastFor = "1m", lockAtMostFor = "30m")
    public void scheduledPoll() {
        if (!properties.getPolling().isEnabled()) {
            return;
        }
        List<User> users = importService.usersEligibleForPolling();
        log.info("Splitwise scheduled poll starting for {} user(s)", users.size());
        for (User user : users) {
            syncUserSafely(user.getId());
            sleepBetweenUsers();
        }
    }

    @Async
    public void triggerAsync(Long userId) {
        syncUserSafely(userId);
    }

    public SplitwiseImportResult triggerSync(Long userId) {
        if (!importService.tryStartSyncForUser(userId)) {
            throw new IllegalStateException("Splitwise sync already running for user " + userId);
        }
        try {
            return importService.syncForUser(userId);
        } catch (Exception ex) {
            log.error("Splitwise sync failed for user {}", userId, ex);
            throw ex;
        } finally {
            importService.finishSyncForUser(userId);
        }
    }

    private void syncUserSafely(Long userId) {
        if (!importService.tryStartSyncForUser(userId)) {
            log.warn("Splitwise sync already running for user {}, skipping", userId);
            return;
        }
        try {
            importService.syncForUser(userId);
        } catch (Exception ex) {
            log.error("Splitwise sync failed for user {}", userId, ex);
        } finally {
            importService.finishSyncForUser(userId);
        }
    }

    private void sleepBetweenUsers() {
        int delay = properties.getUserDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
