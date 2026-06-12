package com.bank.bean;

import java.sql.Timestamp;

public class Customer {
    private Long customerId;
    private Long userId;
    private String fullName;
    private String idCardNo;
    private String phone;
    private String email;
    private String address;
    private String riskLevel;
    private String riskLevelSource;
    private Timestamp riskLevelUpdatedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskLevelSource() {
        return riskLevelSource;
    }

    public void setRiskLevelSource(String riskLevelSource) {
        this.riskLevelSource = riskLevelSource;
    }

    public Timestamp getRiskLevelUpdatedAt() {
        return riskLevelUpdatedAt;
    }

    public void setRiskLevelUpdatedAt(Timestamp riskLevelUpdatedAt) {
        this.riskLevelUpdatedAt = riskLevelUpdatedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
