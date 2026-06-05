package com.financial.tracker.financial_transactions.splitwise.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitwiseExpenseUserDto {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("net_balance")
    private String netBalance;

    @JsonProperty("paid_share")
    private String paidShare;

    @JsonProperty("owed_share")
    private String owedShare;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(String netBalance) {
        this.netBalance = netBalance;
    }

    public String getPaidShare() {
        return paidShare;
    }

    public void setPaidShare(String paidShare) {
        this.paidShare = paidShare;
    }

    public String getOwedShare() {
        return owedShare;
    }

    public void setOwedShare(String owedShare) {
        this.owedShare = owedShare;
    }
}
