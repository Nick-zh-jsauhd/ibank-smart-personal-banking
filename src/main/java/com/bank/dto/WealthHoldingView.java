package com.bank.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WealthHoldingView {
    private Long holdingId;
    private Long accountId;
    private String accountNo;
    private Long productId;
    private String productCode;
    private String productName;
    private String riskLevel;
    private String productType;
    private boolean allowEarlyRedeem;
    private BigDecimal principal;
    private BigDecimal expectedRate;
    private int periodDays;
    private int remainingDays;
    private int holdingDays;
    private BigDecimal currentIncome;
    private BigDecimal estimatedValue;
    private BigDecimal redeemAmount;
    private String status;
    private Timestamp buyTime;
    private java.sql.Date valueDate;
    private java.sql.Date maturityDate;
    private java.sql.Date expectedArrivalDate;
    private Timestamp redeemTime;

    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

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

    public boolean isAllowEarlyRedeem() {
        return allowEarlyRedeem;
    }

    public void setAllowEarlyRedeem(boolean allowEarlyRedeem) {
        this.allowEarlyRedeem = allowEarlyRedeem;
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

    public int getPeriodDays() {
        return periodDays;
    }

    public void setPeriodDays(int periodDays) {
        this.periodDays = periodDays;
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(int remainingDays) {
        this.remainingDays = remainingDays;
    }

    public int getHoldingDays() {
        return holdingDays;
    }

    public void setHoldingDays(int holdingDays) {
        this.holdingDays = holdingDays;
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

    public BigDecimal getRedeemAmount() {
        return redeemAmount;
    }

    public void setRedeemAmount(BigDecimal redeemAmount) {
        this.redeemAmount = redeemAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public java.sql.Date getExpectedArrivalDate() {
        return expectedArrivalDate;
    }

    public void setExpectedArrivalDate(java.sql.Date expectedArrivalDate) {
        this.expectedArrivalDate = expectedArrivalDate;
    }

    public Timestamp getRedeemTime() {
        return redeemTime;
    }

    public void setRedeemTime(Timestamp redeemTime) {
        this.redeemTime = redeemTime;
    }
}
