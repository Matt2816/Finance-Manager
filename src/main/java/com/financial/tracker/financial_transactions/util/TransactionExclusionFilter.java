package com.financial.tracker.financial_transactions.util;

import com.financial.tracker.financial_transactions.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TransactionExclusionFilter {

    private static final Logger log = LoggerFactory.getLogger(TransactionExclusionFilter.class);

    private final List<String> exclusionPatterns;

    public TransactionExclusionFilter(
            @Value("${transaction.exclusion.patterns:PAYMENT RECEIVED - THANK YOU}")
            String exclusionPatternsString
    ) {
        this.exclusionPatterns = Arrays.stream(exclusionPatternsString.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toList();
        log.info("TransactionExclusionFilter initialized with {} exclusion patterns: {}", 
                exclusionPatterns.size(), exclusionPatterns);
    }

    public boolean shouldExclude(Transaction transaction) {
        if (transaction == null) {
            return false;
        }

        String description = transaction.getName();
        if (description == null || description.isBlank()) {
            return false;
        }

        String normalizedDescription = description.toUpperCase();

        for (String pattern : exclusionPatterns) {
            if (normalizedDescription.contains(pattern.toUpperCase())) {
                log.info("Excluding transaction with description containing '{}': {}", pattern, description);
                return true;
            }
        }

        return false;
    }

    public boolean shouldExclude(String description) {
        if (description == null || description.isBlank()) {
            return false;
        }

        String normalizedDescription = description.toUpperCase();

        for (String pattern : exclusionPatterns) {
            if (normalizedDescription.contains(pattern.toUpperCase())) {
                log.info("Excluding transaction with description containing '{}': {}", pattern, description);
                return true;
            }
        }

        return false;
    }
}
