package com.financial.tracker.financial_transactions.analytics.model;

public enum RecurrenceFrequency {
    WEEKLY(52.0 / 12.0),
    BIWEEKLY(26.0 / 12.0),
    MONTHLY(1.0);

    private final double monthlyMultiplier;

    RecurrenceFrequency(double monthlyMultiplier) {
        this.monthlyMultiplier = monthlyMultiplier;
    }

    public double getMonthlyMultiplier() {
        return monthlyMultiplier;
    }
}
