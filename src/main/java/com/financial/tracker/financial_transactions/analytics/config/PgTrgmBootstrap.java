package com.financial.tracker.financial_transactions.analytics.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PgTrgmBootstrap {

    private static final Logger log = LoggerFactory.getLogger(PgTrgmBootstrap.class);

    private final JdbcTemplate jdbcTemplate;

    public PgTrgmBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensurePgTrgmExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            log.info("Verified PostgreSQL extension pg_trgm");
        } catch (Exception ex) {
            log.warn("Could not enable pg_trgm extension; fuzzy resolver will use fallback mode: {}", ex.getMessage());
        }
    }
}
