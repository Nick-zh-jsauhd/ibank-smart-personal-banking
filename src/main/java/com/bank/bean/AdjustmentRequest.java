package com.bank.bean;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AdjustmentRequest {
    private Long adjustmentId;
    private String adjustmentNo;
    private Long reconciliationItemId;
    private String sourceType;
    private Long sourceTicketId;
    private Long accountId;
    private Long customerId;
    private String direction;
    private BigDecimal amount;
    private String reason;
    private String evidence;
    private String status;
    private Long applicantAdminUserId;
    private Long reviewerAdminUserId;
    private String reviewNote;
    private Timestamp reviewedAt;
    private Long executedTransactionId;
    private Long executedLedgerId;
    private Timestamp executedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String accountNo;
    private String customerName;
    private String applicantUsername;
    private String reviewerUsername;
    private String reconciliationCheckType;
    private String reconciliationBusinessId;
    private String reconciliationStatus;

    public Long getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(Long adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public String getAdjustmentNo() {
        return adjustmentNo;
    }

    public void setAdjustmentNo(String adjustmentNo) {
        this.adjustmentNo = adjustmentNo;
    }

    public Long getReconciliationItemId() {
        return reconciliationItemId;
    }

    public void setReconciliationItemId(Long reconciliationItemId) {
        this.reconciliationItemId = reconciliationItemId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceTicketId() {
        return sourceTicketId;
    }

    public void setSourceTicketId(Long sourceTicketId) {
        this.sourceTicketId = sourceTicketId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getApplicantAdminUserId() {
        return applicantAdminUserId;
    }

    public void setApplicantAdminUserId(Long applicantAdminUserId) {
        this.applicantAdminUserId = applicantAdminUserId;
    }

    public Long getReviewerAdminUserId() {
        return reviewerAdminUserId;
    }

    public void setReviewerAdminUserId(Long reviewerAdminUserId) {
        this.reviewerAdminUserId = reviewerAdminUserId;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getExecutedTransactionId() {
        return executedTransactionId;
    }

    public void setExecutedTransactionId(Long executedTransactionId) {
        this.executedTransactionId = executedTransactionId;
    }

    public Long getExecutedLedgerId() {
        return executedLedgerId;
    }

    public void setExecutedLedgerId(Long executedLedgerId) {
        this.executedLedgerId = executedLedgerId;
    }

    public Timestamp getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Timestamp executedAt) {
        this.executedAt = executedAt;
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

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getApplicantUsername() {
        return applicantUsername;
    }

    public void setApplicantUsername(String applicantUsername) {
        this.applicantUsername = applicantUsername;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getReconciliationCheckType() {
        return reconciliationCheckType;
    }

    public void setReconciliationCheckType(String reconciliationCheckType) {
        this.reconciliationCheckType = reconciliationCheckType;
    }

    public String getReconciliationBusinessId() {
        return reconciliationBusinessId;
    }

    public void setReconciliationBusinessId(String reconciliationBusinessId) {
        this.reconciliationBusinessId = reconciliationBusinessId;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }
}
