package com.financial.tracker.financial_transactions.splitwise.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitwiseGroupsResponse {

    private List<SplitwiseGroupDto> groups = new ArrayList<>();

    public List<SplitwiseGroupDto> getGroups() {
        return groups;
    }

    public void setGroups(List<SplitwiseGroupDto> groups) {
        this.groups = groups == null ? new ArrayList<>() : groups;
    }
}
