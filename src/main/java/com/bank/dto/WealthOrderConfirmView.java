package com.bank.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class WealthOrderConfirmView {
    private Long confirmId;
    private String transactionNo;
    private String accountNo;
    private String productCode;
    private String productName;
    private BigDecimal amount;
    private String customerRiskLevel;
    private String productRiskLevel;
    private String matchResult;
    private String disclosureVersion;
    private String ipAddress;
    private Timestamp createdAt;

    public Long getConfirmId() {
        return confirmId;
    }

    public void setConfirmId(Long confirmId) {
        this.confirmId = confirmId;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
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
