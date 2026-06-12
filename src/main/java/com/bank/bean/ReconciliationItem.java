package com.bank.bean;

import java.sql.Date;
import java.sql.Timestamp;

public class ReconciliationItem {
    private Long itemId;
    private Long batchId;
    private Date reconDate;
    private String checkType;
    private String severity;
    private String businessType;
    private String businessId;
    private String expectedValue;
    private String actualValue;
    private String description;
    private String status;
    private Long handlerAdminUserId;
    private String handlerUsername;
    private String handleResult;
    private String handleNote;
    private Timestamp handledAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

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

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getHandleResult() {
        return handleResult;
    }

    public void setHandleResult(String handleResult) {
        this.handleResult = handleResult;
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

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
