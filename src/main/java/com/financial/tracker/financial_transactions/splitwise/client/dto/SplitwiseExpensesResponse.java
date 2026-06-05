package com.financial.tracker.financial_transactions.splitwise.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitwiseExpensesResponse {

    private List<SplitwiseExpenseDto> expenses = new ArrayList<>();

    public List<SplitwiseExpenseDto> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<SplitwiseExpenseDto> expenses) {
        this.expenses = expenses == null ? new ArrayList<>() : expenses;
    }
}
