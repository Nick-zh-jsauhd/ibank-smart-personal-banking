package com.bank.bean;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WealthProduct {
    private Long productId;
    private String productCode;
    private String productName;
    private String riskLevel;
    private String productType;
    private BigDecimal expectedRate;
    private int periodDays;
    private int confirmDays;
    private int arrivalDays;
    private boolean allowEarlyRedeem;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String status;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BigDecimal getExpectedRate() {
        return expectedRate;
    }

    public void setExpectedRate(BigDecimal expectedRate) {
        this.expectedRate = expectedRate;
    }

    public int getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(int periodDays) {
        this.periodDays = periodDays;
    }

    public int getConfirmDays() {
        return confirmDays;
    }

    public void setConfirmDays(int confirmDays) {
        this.confirmDays = confirmDays;
    }

    public int getArrivalDays() {
        return arrivalDays;
    }

    public void setArrivalDays(int arrivalDays) {
        this.arrivalDays = arrivalDays;
    }

    public boolean isAllowEarlyRedeem() {
        return allowEarlyRedeem;
    }

    public void setAllowEarlyRedeem(boolean allowEarlyRedeem) {
        this.allowEarlyRedeem = allowEarlyRedeem;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
