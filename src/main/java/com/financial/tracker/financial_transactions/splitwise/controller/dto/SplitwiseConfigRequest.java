package com.financial.tracker.financial_transactions.splitwise.controller.dto;

public record SplitwiseConfigRequest(
        String apiKey,
        String groupNames,
        Boolean enabled
) {}
