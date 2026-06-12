package com.bank.service.impl;

import com.bank.dao.AdminAuditQueryDao;
import com.bank.dao.impl.AdminAuditQueryDaoImpl;
import com.bank.dto.AdminAuditLogView;
import com.bank.dto.AdminAuditOverview;
import com.bank.dto.AdminLoginLogView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAuditService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminAuditServiceImpl implements AdminAuditService {
    private static final int LIST_LIMIT = 200;
    private static final int OVERVIEW_LIMIT = 20;
    private static final Set<String> HIGH_RISK_OPERATION_TYPES = new LinkedHashSet<String>(Arrays.asList(
            "ADJUST_CUSTOMER_RISK_LEVEL",
            "HANDLE_RISK_EVENT",
            "UPDATE_RISK_RULE",
            "UPDATE_WEALTH_PRODUCT",
            "RUN_RECONCILIATION",
            "HANDLE_RECONCILIATION_ITEM",
            "CREATE_ADJUSTMENT",
            "REVIEW_ADJUSTMENT",
            "EXECUTE_ADJUSTMENT",
            "HANDLE_ADMIN_ALERT",
            "AUTO_SYNC_ADMIN_ALERT",
            "HANDLE_SERVICE_TICKET",
            "CREATE_ADMIN_USER",
            "UPDATE_ADMIN_USER",
            "ADMIN_PERMISSION_DENIED"
    ));

    private final AdminAuditQueryDao adminAuditQueryDao = new AdminAuditQueryDaoImpl();

    @Override
    public ServiceResult<AdminAuditOverview> overview() {
        LocalDate today = LocalDate.now();
        Timestamp start = Timestamp.valueOf(today.atStartOfDay());
        Timestamp end = Timestamp.valueOf(today.plusDays(1).atStartOfDay());
        try (Connection connection = DBUtil.getConnection()) {
            AdminAuditOverview overview = new AdminAuditOverview();
            overview.setTodayAdminLoginCount(adminAuditQueryDao.countAdminLogins(connection, start, end));
            overview.setTodayLoginFailureCount(adminAuditQueryDao.countLoginFailures(connection, start, end));
            overview.setTodayPermissionDeniedCount(adminAuditQueryDao.countAuditLogsByOperation(connection,
                    "ADMIN_PERMISSION_DENIED", start, end));
            overview.setTodayHighRiskOperationCount(adminAuditQueryDao.countHighRiskAuditLogs(connection,
                    HIGH_RISK_OPERATION_TYPES, start, end));
            overview.setRecentHighRiskLogs(adminAuditQueryDao.findAdminAuditLogs(connection, "", "", "",
                    null, null, true, HIGH_RISK_OPERATION_TYPES, OVERVIEW_LIMIT));
            overview.setRecentPermissionDeniedLogs(adminAuditQueryDao.findAdminAuditLogs(connection, "",
                    "ADMIN_PERMISSION_DENIED", "", null, null, false, HIGH_RISK_OPERATION_TYPES, OVERVIEW_LIMIT));
            return ServiceResult.success("审计总览查询成功。", overview);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("审计总览查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<AdminAuditLogView>> listAdminAuditLogs(String adminKeyword, String operationType,
                                                                     String targetType, String startDate,
                                                                     String endDate, boolean highRiskOnly) {
        DateRange range;
        try {
            range = parseRange(startDate, endDate);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("管理员操作日志查询成功。",
                    adminAuditQueryDao.findAdminAuditLogs(connection, trim(adminKeyword), trim(operationType),
                            trim(targetType), range.startInclusive, range.endExclusive, highRiskOnly,
                            HIGH_RISK_OPERATION_TYPES, LIST_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("管理员操作日志查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<AdminLoginLogView>> listLoginLogs(String identityKeyword, String userRole,
                                                                String successFilter, String startDate,
                                                                String endDate) {
        DateRange range;
        try {
            range = parseRange(startDate, endDate);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("登录日志查询成功。",
                    adminAuditQueryDao.findLoginLogs(connection, trim(identityKeyword), normalizeRole(userRole),
                            normalizeSuccess(successFilter), range.startInclusive, range.endExclusive, LIST_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("登录日志查询失败，请检查数据库状态或稍后重试。");
        }
    }

    private DateRange parseRange(String startDate, String endDate) {
        DateRange range = new DateRange();
        String start = trim(startDate);
        String end = trim(endDate);
        try {
            if (start.length() > 0) {
                range.startInclusive = Timestamp.valueOf(LocalDate.parse(start).atStartOfDay());
            }
            if (end.length() > 0) {
                range.endExclusive = Timestamp.valueOf(LocalDate.parse(end).plusDays(1).atStartOfDay());
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("日期格式不正确，请使用 yyyy-MM-dd。");
        }
        if (range.startInclusive != null && range.endExclusive != null
                && !range.startInclusive.before(range.endExclusive)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期。");
        }
        return range;
    }

    private String normalizeRole(String userRole) {
        String role = trim(userRole).toUpperCase();
        if ("ADMIN".equals(role) || "CUSTOMER".equals(role)) {
            return role;
        }
        return "";
    }

    private String normalizeSuccess(String successFilter) {
        String value = trim(successFilter).toUpperCase();
        if ("SUCCESS".equals(value) || "FAILURE".equals(value)) {
            return value;
        }
        return "";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static class DateRange {
        private Timestamp startInclusive;
        private Timestamp endExclusive;
    }
}
