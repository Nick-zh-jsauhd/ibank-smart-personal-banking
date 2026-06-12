package com.bank.bean;

import java.sql.Date;
import java.sql.Timestamp;

public class ReconciliationBatch {
    private Long batchId;
    private Date reconDate;
    private String status;
    private int totalChecks;
    private int exceptionCount;
    private Long createdByAdminUserId;
    private String createdByUsername;
    private Timestamp startedAt;
    private Timestamp finishedAt;
    private Timestamp createdAt;

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Date getReconDate() {
        return reconDate;
    }

    public void setReconDate(Date reconDate) {
        this.reconDate = reconDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalChecks() {
        return totalChecks;
    }

    public void setTotalChecks(int totalChecks) {
        this.totalChecks = totalChecks;
    }

    public int getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(int exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public Long getCreatedByAdminUserId() {
        return createdByAdminUserId;
    }

    public void setCreatedByAdminUserId(Long createdByAdminUserId) {
        this.createdByAdminUserId = createdByAdminUserId;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
