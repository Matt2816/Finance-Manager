package com.financial.tracker.financial_transactions.splitwise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "splitwise")
public class SplitwiseProperties {

    private final Polling polling = new Polling();
    private final Api api = new Api();
    private int initialLookbackDays = 90;
    private int pollOverlapMinutes = 5;
    private int pageSize = 50;
    private int userDelayMs = 200;

    public Polling getPolling() {
        return polling;
    }

    public Api getApi() {
        return api;
    }

    public int getInitialLookbackDays() {
        return initialLookbackDays;
    }

    public void setInitialLookbackDays(int initialLookbackDays) {
        this.initialLookbackDays = initialLookbackDays;
    }

    public int getPollOverlapMinutes() {
        return pollOverlapMinutes;
    }

    public void setPollOverlapMinutes(int pollOverlapMinutes) {
        this.pollOverlapMinutes = pollOverlapMinutes;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getUserDelayMs() {
        return userDelayMs;
    }

    public void setUserDelayMs(int userDelayMs) {
        this.userDelayMs = userDelayMs;
    }

    public static class Polling {
        private boolean enabled = true;
        private String cron = "0 0 8 * * ?";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Api {
        private String baseUrl = "https://secure.splitwise.com/api/v3.0";
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
        private int maxRetries = 3;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
}
