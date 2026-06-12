package com.bank.bean;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WealthHolding {
    private Long holdingId;
    private Long customerId;
    private Long accountId;
    private Long productId;
    private Long buyTransactionId;
    private Long redeemTransactionId;
    private Long buyOrderId;
    private Long redeemOrderId;
    private BigDecimal principal;
    private BigDecimal expectedRate;
    private Timestamp buyTime;
    private java.sql.Date valueDate;
    private java.sql.Date maturityDate;
    private Timestamp redeemTime;
    private BigDecimal currentIncome;
    private BigDecimal estimatedValue;
    private String status;

    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getBuyTransactionId() {
        return buyTransactionId;
    }

    public void setBuyTransactionId(Long buyTransactionId) {
        this.buyTransactionId = buyTransactionId;
    }

    public Long getRedeemTransactionId() {
        return redeemTransactionId;
    }

    public void setRedeemTransactionId(Long redeemTransactionId) {
        this.redeemTransactionId = redeemTransactionId;
    }

    public Long getBuyOrderId() {
        return buyOrderId;
    }

    public void setBuyOrderId(Long buyOrderId) {
        this.buyOrderId = buyOrderId;
    }

    public Long getRedeemOrderId() {
        return redeemOrderId;
    }

    public void setRedeemOrderId(Long redeemOrderId) {
        this.redeemOrderId = redeemOrderId;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getExpectedRate() {
        return expectedRate;
    }

    public void setExpectedRate(BigDecimal expectedRate) {
        this.expectedRate = expectedRate;
    }

    public Timestamp getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(Timestamp buyTime) {
        this.buyTime = buyTime;
    }

    public java.sql.Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(java.sql.Date valueDate) {
        this.valueDate = valueDate;
    }

    public java.sql.Date getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(java.sql.Date maturityDate) {
        this.maturityDate = maturityDate;
    }

    public Timestamp getRedeemTime() {
        return redeemTime;
    }

    public void setRedeemTime(Timestamp redeemTime) {
        this.redeemTime = redeemTime;
    }

    public BigDecimal getCurrentIncome() {
        return currentIncome;
    }

    public void setCurrentIncome(BigDecimal currentIncome) {
        this.currentIncome = currentIncome;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
