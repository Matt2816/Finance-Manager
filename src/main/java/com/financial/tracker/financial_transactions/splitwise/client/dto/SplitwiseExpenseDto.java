package com.financial.tracker.financial_transactions.splitwise.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SplitwiseExpenseDto {

    private Long id;
    private String description;
    private String date;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("group_id")
    private Long groupId;

    private Boolean payment;

    @JsonProperty("deleted_at")
    private String deletedAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_by")
    private SplitwiseUserDto createdBy;

    private List<SplitwiseExpenseUserDto> users = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Boolean getPayment() {
        return payment;
    }

    public void setPayment(Boolean payment) {
        this.payment = payment;
    }

    public boolean isPayment() {
        return Boolean.TRUE.equals(payment);
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null && !deletedAt.isBlank();
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public SplitwiseUserDto getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(SplitwiseUserDto createdBy) {
        this.createdBy = createdBy;
    }

    public List<SplitwiseExpenseUserDto> getUsers() {
        return users;
    }

    public void setUsers(List<SplitwiseExpenseUserDto> users) {
        this.users = users == null ? new ArrayList<>() : users;
    }
}
