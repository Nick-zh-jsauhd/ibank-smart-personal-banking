package com.bank.bean;

import java.sql.Timestamp;

public class AdminAlert {
    private Long alertId;
    private String alertType;
    private String severity;
    private String title;
    private String content;
    private String targetType;
    private String targetId;
    private String responsibleRoleCode;
    private String status;
    private Long assignedAdminUserId;
    private String assignedAdminUsername;
    private Long handledByAdminUserId;
    private String handledByAdminUsername;
    private String handleNote;
    private Timestamp handledAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getResponsibleRoleCode() {
        return responsibleRoleCode;
    }

    public void setResponsibleRoleCode(String responsibleRoleCode) {
        this.responsibleRoleCode = responsibleRoleCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssignedAdminUserId() {
        return assignedAdminUserId;
    }

    public void setAssignedAdminUserId(Long assignedAdminUserId) {
        this.assignedAdminUserId = assignedAdminUserId;
    }

    public String getAssignedAdminUsername() {
        return assignedAdminUsername;
    }

    public void setAssignedAdminUsername(String assignedAdminUsername) {
        this.assignedAdminUsername = assignedAdminUsername;
    }

    public Long getHandledByAdminUserId() {
        return handledByAdminUserId;
    }

    public void setHandledByAdminUserId(Long handledByAdminUserId) {
        this.handledByAdminUserId = handledByAdminUserId;
    }

    public String getHandledByAdminUsername() {
        return handledByAdminUsername;
    }

    public void setHandledByAdminUsername(String handledByAdminUsername) {
        this.handledByAdminUsername = handledByAdminUsername;
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
