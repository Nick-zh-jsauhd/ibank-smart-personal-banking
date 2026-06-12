package com.bank.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AdminRiskEventView {
    private Long eventId;
    private Long customerId;
    private Long accountId;
    private String fullName;
    private String phone;
    private String accountNo;
    private String accountStatus;
    private String transactionNo;
    private String txnType;
    private BigDecimal amount;
    private int riskScore;
    private String riskLevel;
    private String decision;
    private String hitRules;
    private String reason;
    private String ipAddress;
    private String handleStatus;
    private String handleResult;
    private Long handlerAdminUserId;
    private String handlerUsername;
    private String handleNote;
    private Timestamp handledAt;
    private Timestamp createdAt;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getHitRules() {
        return hitRules;
    }

    public void setHitRules(String hitRules) {
        this.hitRules = hitRules;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(String handleStatus) {
        this.handleStatus = handleStatus;
    }

    public String getHandleResult() {
        return handleResult;
    }

    public void setHandleResult(String handleResult) {
        this.handleResult = handleResult;
    }

    public Long getHandlerAdminUserId() {
        return handlerAdminUserId;
    }

    public void setHandlerAdminUserId(Long handlerAdminUserId) {
        this.handlerAdminUserId = handlerAdminUserId;
    }

    public String getHandlerUsername() {
        return handlerUsername;
    }

    public void setHandlerUsername(String handlerUsername) {
        this.handlerUsername = handlerUsername;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public Timestamp getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(Timestamp handledAt) {
        this.handledAt = handledAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
