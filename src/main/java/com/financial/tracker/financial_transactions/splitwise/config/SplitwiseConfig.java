package com.financial.tracker.financial_transactions.splitwise.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SplitwiseProperties.class)
public class SplitwiseConfig {
}
