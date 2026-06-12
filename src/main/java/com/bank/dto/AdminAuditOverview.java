package com.bank.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminAuditOverview {
    private int todayAdminLoginCount;
    private int todayLoginFailureCount;
    private int todayPermissionDeniedCount;
    private int todayHighRiskOperationCount;
    private final List<AdminAuditLogView> recentHighRiskLogs = new ArrayList<AdminAuditLogView>();
    private final List<AdminAuditLogView> recentPermissionDeniedLogs = new ArrayList<AdminAuditLogView>();

    public int getTodayAdminLoginCount() {
        return todayAdminLoginCount;
    }

    public void setTodayAdminLoginCount(int todayAdminLoginCount) {
        this.todayAdminLoginCount = todayAdminLoginCount;
    }

    public int getTodayLoginFailureCount() {
        return todayLoginFailureCount;
    }

    public void setTodayLoginFailureCount(int todayLoginFailureCount) {
        this.todayLoginFailureCount = todayLoginFailureCount;
    }

    public int getTodayPermissionDeniedCount() {
        return todayPermissionDeniedCount;
    }

    public void setTodayPermissionDeniedCount(int todayPermissionDeniedCount) {
        this.todayPermissionDeniedCount = todayPermissionDeniedCount;
    }

    public int getTodayHighRiskOperationCount() {
        return todayHighRiskOperationCount;
    }

    public void setTodayHighRiskOperationCount(int todayHighRiskOperationCount) {
        this.todayHighRiskOperationCount = todayHighRiskOperationCount;
    }

    public List<AdminAuditLogView> getRecentHighRiskLogs() {
        return Collections.unmodifiableList(recentHighRiskLogs);
    }

    public void setRecentHighRiskLogs(List<AdminAuditLogView> logs) {
        recentHighRiskLogs.clear();
        if (logs != null) {
            recentHighRiskLogs.addAll(logs);
        }
    }

    public List<AdminAuditLogView> getRecentPermissionDeniedLogs() {
        return Collections.unmodifiableList(recentPermissionDeniedLogs);
    }

    public void setRecentPermissionDeniedLogs(List<AdminAuditLogView> logs) {
        recentPermissionDeniedLogs.clear();
        if (logs != null) {
            recentPermissionDeniedLogs.addAll(logs);
        }
    }
}
