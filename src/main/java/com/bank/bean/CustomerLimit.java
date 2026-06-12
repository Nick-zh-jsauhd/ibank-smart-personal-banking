package com.bank.bean;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class CustomerLimit {
    private Long customerLimitId;
    private Long customerId;
    private String txnType;
    private BigDecimal singleLimit;
    private BigDecimal dailyAmountLimit;
    private int dailyCountLimit;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getCustomerLimitId() {
        return customerLimitId;
    }

    public void setCustomerLimitId(Long customerLimitId) {
        this.customerLimitId = customerLimitId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public BigDecimal getSingleLimit() {
        return singleLimit;
    }

    public void setSingleLimit(BigDecimal singleLimit) {
        this.singleLimit = singleLimit;
    }

    public BigDecimal getDailyAmountLimit() {
        return dailyAmountLimit;
    }

    public void setDailyAmountLimit(BigDecimal dailyAmountLimit) {
        this.dailyAmountLimit = dailyAmountLimit;
    }

    public int getDailyCountLimit() {
        return dailyCountLimit;
    }

    public void setDailyCountLimit(int dailyCountLimit) {
        this.dailyCountLimit = dailyCountLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
