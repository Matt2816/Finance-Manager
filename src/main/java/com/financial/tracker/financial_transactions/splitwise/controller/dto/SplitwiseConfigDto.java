package com.financial.tracker.financial_transactions.splitwise.controller.dto;

public record SplitwiseConfigDto(
        boolean configured,
        String keyHint,
        String groupNames,
        boolean enabled
) {}
