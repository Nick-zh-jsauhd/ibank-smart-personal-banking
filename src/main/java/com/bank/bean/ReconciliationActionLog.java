package com.bank.bean;

import java.sql.Timestamp;

public class ReconciliationActionLog {
    private Long actionId;
    private Long itemId;
    private Long adminUserId;
    private String adminUsername;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private String note;
    private Timestamp createdAt;

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public String getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
