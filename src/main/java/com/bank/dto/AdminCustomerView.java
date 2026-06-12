package com.bank.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AdminCustomerView {
    private Long customerId;
    private String username;
    private String fullName;
    private String phone;
    private String riskLevel;
    private int accountCount;
    private BigDecimal totalAvailableBalance;
    private Timestamp createdAt;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public BigDecimal getTotalAvailableBalance() {
        return totalAvailableBalance;
    }

    public void setTotalAvailableBalance(BigDecimal totalAvailableBalance) {
        this.totalAvailableBalance = totalAvailableBalance;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
