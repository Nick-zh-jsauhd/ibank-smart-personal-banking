package com.bank.dto;

import java.sql.Timestamp;

public class AdminRiskGraphModelGovernanceView {
    private String modelVersion;
    private String modelRole;
    private String lifecycleStatus;
    private String onlineMode;
    private boolean operational;
    private String governanceNote;
    private Long promotedByAdminUserId;
    private Timestamp promotedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getModelRole() {
        return modelRole;
    }

    public void setModelRole(String modelRole) {
        this.modelRole = modelRole;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public String getOnlineMode() {
        return onlineMode;
    }

    public void setOnlineMode(String onlineMode) {
        this.onlineMode = onlineMode;
    }

    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public String getGovernanceNote() {
        return governanceNote;
    }

    public void setGovernanceNote(String governanceNote) {
        this.governanceNote = governanceNote;
    }

    public Long getPromotedByAdminUserId() {
        return promotedByAdminUserId;
    }

    public void setPromotedByAdminUserId(Long promotedByAdminUserId) {
        this.promotedByAdminUserId = promotedByAdminUserId;
    }

    public Timestamp getPromotedAt() {
        return promotedAt;
    }

    public void setPromotedAt(Timestamp promotedAt) {
        this.promotedAt = promotedAt;
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
