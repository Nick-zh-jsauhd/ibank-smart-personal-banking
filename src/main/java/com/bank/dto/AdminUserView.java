package com.bank.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminUserView {
    private long userId;
    private String username;
    private String phone;
    private String status;
    private int failedLoginCount;
    private Timestamp lockedUntil;
    private Timestamp lastLoginAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private final List<String> roleCodes = new ArrayList<String>();
    private final List<String> roleNames = new ArrayList<String>();

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public Timestamp getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Timestamp lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Timestamp lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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

    public List<String> getRoleCodes() {
        return Collections.unmodifiableList(roleCodes);
    }

    public List<String> getRoleNames() {
        return Collections.unmodifiableList(roleNames);
    }

    public void setRoleCodesFromCsv(String csv) {
        setListFromCsv(roleCodes, csv);
    }

    public void setRoleNamesFromCsv(String csv) {
        setListFromCsv(roleNames, csv);
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    public String roleNameSummary() {
        if (roleNames.isEmpty()) {
            return "未分配";
        }
        StringBuilder builder = new StringBuilder();
        for (String roleName : roleNames) {
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(roleName);
        }
        return builder.toString();
    }

    private void setListFromCsv(List<String> target, String csv) {
        target.clear();
        if (csv == null || csv.trim().length() == 0) {
            return;
        }
        String[] values = csv.split(",");
        for (String value : values) {
            String trimmed = value.trim();
            if (trimmed.length() > 0) {
                target.add(trimmed);
            }
        }
    }
}
