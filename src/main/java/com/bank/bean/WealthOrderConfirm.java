package com.bank.bean;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WealthOrderConfirm {
    private Long confirmId;
    private Long transactionId;
    private Long customerId;
    private Long accountId;
    private Long productId;
    private BigDecimal amount;
    private String customerRiskLevel;
    private String productRiskLevel;
    private String matchResult;
    private String disclosureVersion;
    private boolean productDisclosureChecked;
    private boolean nonDepositChecked;
    private boolean yieldNotGuaranteedChecked;
    private boolean accountConfirmed;
    private String ipAddress;
    private Timestamp createdAt;

    public Long getConfirmId() {
        return confirmId;
    }

    public void setConfirmId(Long confirmId) {
        this.confirmId = confirmId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomerRiskLevel() {
        return customerRiskLevel;
    }

    public void setCustomerRiskLevel(String customerRiskLevel) {
        this.customerRiskLevel = customerRiskLevel;
    }

    public String getProductRiskLevel() {
        return productRiskLevel;
    }

    public void setProductRiskLevel(String productRiskLevel) {
        this.productRiskLevel = productRiskLevel;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public String getDisclosureVersion() {
        return disclosureVersion;
    }

    public void setDisclosureVersion(String disclosureVersion) {
        this.disclosureVersion = disclosureVersion;
    }

    public boolean isProductDisclosureChecked() {
        return productDisclosureChecked;
    }

    public void setProductDisclosureChecked(boolean productDisclosureChecked) {
        this.productDisclosureChecked = productDisclosureChecked;
    }

    public boolean isNonDepositChecked() {
        return nonDepositChecked;
    }

    public void setNonDepositChecked(boolean nonDepositChecked) {
        this.nonDepositChecked = nonDepositChecked;
    }

    public boolean isYieldNotGuaranteedChecked() {
        return yieldNotGuaranteedChecked;
    }

    public void setYieldNotGuaranteedChecked(boolean yieldNotGuaranteedChecked) {
        this.yieldNotGuaranteedChecked = yieldNotGuaranteedChecked;
    }

    public boolean isAccountConfirmed() {
        return accountConfirmed;
    }

    public void setAccountConfirmed(boolean accountConfirmed) {
        this.accountConfirmed = accountConfirmed;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
