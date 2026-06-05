package com.financial.tracker.financial_transactions.splitwise.client;

public class SplitwiseApiException extends RuntimeException {

    private final int statusCode;

    public SplitwiseApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public SplitwiseApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
