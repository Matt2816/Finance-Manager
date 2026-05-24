package com.financial.tracker.financial_transactions.analytics.config;

import com.financial.tracker.financial_transactions.analytics.normalization.TransactionBackfillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AnalyticsStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsStartupRunner.class);

    private final TransactionBackfillService backfillService;

    @Value("${analytics.backfill-on-start:false}")
    private boolean backfillOnStart;

    public AnalyticsStartupRunner(TransactionBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (backfillOnStart) {
            log.info("Running transaction backfill on startup");
            backfillService.backfillAll();
        }
    }
}
