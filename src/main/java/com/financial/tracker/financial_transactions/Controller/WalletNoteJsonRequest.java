package com.financial.tracker.financial_transactions.Controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON body for wallet-note import. Accepts lowercase API keys and Wallet-style
 * capitalized keys (e.g. from Apple Shortcuts dictionaries).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WalletNoteJsonRequest(
        @JsonProperty("name") @JsonAlias("Name") String name,
        @JsonProperty("merchant") @JsonAlias("Merchant") String merchant,
        @JsonProperty("amount") @JsonAlias("Amount") String amount,
        @JsonProperty("date") @JsonAlias("Date") String date,
        @JsonProperty("location") @JsonAlias("Location") String location
) {}
