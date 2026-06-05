package com.financial.tracker.financial_transactions.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "splitwise_api_key_enc")
    private byte[] splitwiseApiKeyEnc;

    @Column(name = "splitwise_api_key_iv")
    private byte[] splitwiseApiKeyIv;

    @Column(name = "splitwise_group_names")
    private String splitwiseGroupNames;

    @Column(name = "splitwise_enabled", nullable = false)
    private boolean splitwiseEnabled = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public byte[] getSplitwiseApiKeyEnc() {
        return splitwiseApiKeyEnc;
    }

    public void setSplitwiseApiKeyEnc(byte[] splitwiseApiKeyEnc) {
        this.splitwiseApiKeyEnc = splitwiseApiKeyEnc;
    }

    public byte[] getSplitwiseApiKeyIv() {
        return splitwiseApiKeyIv;
    }

    public void setSplitwiseApiKeyIv(byte[] splitwiseApiKeyIv) {
        this.splitwiseApiKeyIv = splitwiseApiKeyIv;
    }

    public String getSplitwiseGroupNames() {
        return splitwiseGroupNames;
    }

    public void setSplitwiseGroupNames(String splitwiseGroupNames) {
        this.splitwiseGroupNames = splitwiseGroupNames;
    }

    public boolean isSplitwiseEnabled() {
        return splitwiseEnabled;
    }

    public void setSplitwiseEnabled(boolean splitwiseEnabled) {
        this.splitwiseEnabled = splitwiseEnabled;
    }

    public boolean hasSplitwiseCredentials() {
        return splitwiseApiKeyEnc != null && splitwiseApiKeyEnc.length > 0
                && splitwiseApiKeyIv != null && splitwiseApiKeyIv.length > 0;
    }
}
