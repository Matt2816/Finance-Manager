package com.financial.tracker.financial_transactions.splitwise.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitwiseCurrentUserResponse {

    private SplitwiseUserDto user;

    public SplitwiseUserDto getUser() {
        return user;
    }

    public void setUser(SplitwiseUserDto user) {
        this.user = user;
    }
}
