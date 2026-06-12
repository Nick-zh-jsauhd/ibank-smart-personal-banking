package com.bank.bean;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class WealthOrder {
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private Long accountId;
    private Long productId;
    private Long holdingId;
    private Long transactionId;
    private String orderType;
    private BigDecimal amount;
    private BigDecimal confirmedAmount;
    private BigDecimal incomeAmount;
    private String status;
    private Timestamp submitTime;
    private Timestamp confirmTime;
    private Date valueDate;
    private Date maturityDate;
    private Date expectedArrivalDate;
    private Timestamp completedTime;
    private String failReason;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getConfirmedAmount() {
        return confirmedAmount;
    }

    public void setConfirmedAmount(BigDecimal confirmedAmount) {
        this.confirmedAmount = confirmedAmount;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public void setIncomeAmount(BigDecimal incomeAmount) {
        this.incomeAmount = incomeAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Timestamp submitTime) {
        this.submitTime = submitTime;
    }

    public Timestamp getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(Timestamp confirmTime) {
        this.confirmTime = confirmTime;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public Date getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(Date maturityDate) {
        this.maturityDate = maturityDate;
    }

    public Date getExpectedArrivalDate() {
        return expectedArrivalDate;
    }

    public void setExpectedArrivalDate(Date expectedArrivalDate) {
        this.expectedArrivalDate = expectedArrivalDate;
    }

    public Timestamp getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Timestamp completedTime) {
        this.completedTime = completedTime;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
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
