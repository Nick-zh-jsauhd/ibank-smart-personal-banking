package com.bank.service.impl;

import com.bank.bean.AdminAlert;
import com.bank.dao.AdminAlertDao;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.impl.AdminAlertDaoImpl;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAlertService;
import com.bank.service.PermissionService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class AdminAlertServiceImpl implements AdminAlertService {
    private static final int ALERT_QUERY_LIMIT = 200;

    private final AdminAlertDao adminAlertDao = new AdminAlertDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final PermissionService permissionService = new PermissionServiceImpl();

    @Override
    public void create(Connection connection, String alertType, String severity, String title, String content,
                       String targetType, String targetId, String responsibleRoleCode) throws SQLException {
        createOrReuse(connection, alertType, severity, title, content, targetType, targetId, responsibleRoleCode);
    }

    @Override
    public long createOrReuse(Connection connection, String alertType, String severity, String title, String content,
                              String targetType, String targetId, String responsibleRoleCode) throws SQLException {
        String normalizedType = trimToLength(alertType, 40, "SYSTEM");
        String normalizedTargetType = trimToLength(targetType, 50, null);
        String normalizedTargetId = trimToLength(targetId, 64, null);
        AdminAlert existing = adminAlertDao.findOpenByBusinessKey(connection, normalizedType,
                normalizedTargetType, normalizedTargetId);
        if (existing != null && existing.getAlertId() != null) {
            return existing.getAlertId();
        }

        AdminAlert alert = new AdminAlert();
        alert.setAlertType(normalizedType);
        alert.setSeverity(normalizeSeverityForCreate(severity));
        alert.setTitle(trimToLength(title, 120, "后台消息"));
        alert.setContent(trimToLength(content, 500, ""));
        alert.setTargetType(normalizedTargetType);
        alert.setTargetId(normalizedTargetId);
        alert.setResponsibleRoleCode(trimToLength(responsibleRoleCode, 50, null));
        alert.setStatus("NEW");
        return adminAlertDao.insert(connection, alert);
    }

    @Override
    public ServiceResult<List<AdminAlert>> listAlerts(long adminUserId, String status, String severity) {
        String normalizedStatus;
        String normalizedSeverity;
        try {
            normalizedStatus = normalizeStatusFilter(status);
            normalizedSeverity = normalizeSeverityFilter(severity);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        try (Connection connection = DBUtil.getConnection()) {
            Set<String> roles = permissionService.rolesFor(adminUserId);
            boolean viewAll = canViewAll(roles);
            return ServiceResult.success("后台消息查询成功。",
                    adminAlertDao.findVisible(connection, roles, adminUserId, viewAll,
                            normalizedStatus, normalizedSeverity, ALERT_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("后台消息查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdminAlert> getAlert(long adminUserId, long alertId) {
        if (alertId <= 0) {
            return ServiceResult.failure("后台消息不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            AdminAlert alert = adminAlertDao.findById(connection, alertId);
            if (alert == null || !canView(adminUserId, alert)) {
                return ServiceResult.failure("后台消息不存在或无权查看。");
            }
            return ServiceResult.success("后台消息查询成功。", alert);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("后台消息查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Integer> countOpenAlerts(long adminUserId) {
        try (Connection connection = DBUtil.getConnection()) {
            Set<String> roles = permissionService.rolesFor(adminUserId);
            return ServiceResult.success("待处理消息数量查询成功。",
                    adminAlertDao.countOpenVisible(connection, roles, adminUserId, canViewAll(roles)));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("待处理消息数量查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> handleAlert(long adminUserId, long alertId, String action, String note,
                                           String ipAddress) {
        String targetStatus;
        String normalizedNote;
        try {
            targetStatus = normalizeAction(action);
            normalizedNote = normalizeHandleNote(targetStatus, note);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Set<String> permissions = permissionService.permissionsFor(adminUserId);
        if (!permissions.contains("ADMIN_ALERT_HANDLE")) {
            return ServiceResult.failure("当前管理员没有处理后台消息的权限。");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            AdminAlert alert = adminAlertDao.findByIdForUpdate(connection, alertId);
            if (alert == null || !canView(adminUserId, alert)) {
                rollbackQuietly(connection);
                return ServiceResult.failure("后台消息不存在或无权处理。");
            }
            if ("CLOSED".equals(alert.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("已关闭的消息不能重复处理。");
            }
            if (targetStatus.equals(alert.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("消息状态没有变化。");
            }

            String beforeStatus = alert.getStatus();
            adminAlertDao.updateStatus(connection, alertId, adminUserId, targetStatus, normalizedNote);
            adminAuditLogDao.insert(connection, adminUserId, "HANDLE_ADMIN_ALERT", "ADMIN_ALERT",
                    String.valueOf(alertId), "处理后台消息：" + beforeStatus + " -> " + targetStatus
                            + "，标题：" + alert.getTitle() + "，说明：" + normalizedNote, ipAddress);
            connection.commit();
            return ServiceResult.success("后台消息状态已更新。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("后台消息处理失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public void ackByTarget(Connection connection, String alertType, String targetType, String targetId,
                            long adminUserId, String note) throws SQLException {
        updateByTarget(connection, alertType, targetType, targetId, adminUserId, "ACKED",
                trimToLength(note, 500, "已开始处理"));
    }

    @Override
    public void resolveByTarget(Connection connection, String alertType, String targetType, String targetId,
                                long adminUserId, String note) throws SQLException {
        updateByTarget(connection, alertType, targetType, targetId, adminUserId, "RESOLVED",
                trimToLength(note, 500, "关联业务已处理完成，系统自动解决待办"));
    }

    @Override
    public void closeByTarget(Connection connection, String alertType, String targetType, String targetId,
                              long adminUserId, String note) throws SQLException {
        updateByTarget(connection, alertType, targetType, targetId, adminUserId, "CLOSED",
                trimToLength(note, 500, "关联业务已关闭，系统自动关闭待办"));
    }

    private void updateByTarget(Connection connection, String alertType, String targetType, String targetId,
                                long adminUserId, String status, String note) throws SQLException {
        String normalizedType = trimToLength(alertType, 40, "SYSTEM");
        String normalizedTargetType = trimToLength(targetType, 50, null);
        String normalizedTargetId = trimToLength(targetId, 64, null);
        int updated = adminAlertDao.updateOpenStatusByBusinessKey(connection, normalizedType, normalizedTargetType,
                normalizedTargetId, adminUserId, status, note);
        if (updated > 0) {
            adminAuditLogDao.insert(connection, adminUserId, "AUTO_SYNC_ADMIN_ALERT", "ADMIN_ALERT",
                    normalizedType + ":" + normalizedTargetType + ":" + normalizedTargetId,
                    "自动同步后台消息状态为 " + status + "，说明：" + note, null);
        }
    }

    private boolean canView(long adminUserId, AdminAlert alert) {
        Set<String> roles = permissionService.rolesFor(adminUserId);
        if (canViewAll(roles)) {
            return true;
        }
        if (alert.getAssignedAdminUserId() != null
                && Long.valueOf(adminUserId).equals(alert.getAssignedAdminUserId())) {
            return true;
        }
        String responsibleRoleCode = alert.getResponsibleRoleCode();
        return responsibleRoleCode == null || responsibleRoleCode.length() == 0 || roles.contains(responsibleRoleCode);
    }

    private boolean canViewAll(Set<String> roles) {
        return roles != null && (roles.contains("SUPER_ADMIN") || roles.contains("AUDITOR"));
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.trim().length() == 0 || "ALL".equals(status)) {
            return null;
        }
        String value = status.trim();
        if (isValidStatus(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的消息状态。");
    }

    private String normalizeSeverityFilter(String severity) {
        if (severity == null || severity.trim().length() == 0 || "ALL".equals(severity)) {
            return null;
        }
        String value = severity.trim();
        if (isValidSeverity(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的消息级别。");
    }

    private String normalizeAction(String action) {
        if ("ACK".equals(action)) {
            return "ACKED";
        }
        if ("RESOLVE".equals(action)) {
            return "RESOLVED";
        }
        if ("CLOSE".equals(action)) {
            return "CLOSED";
        }
        throw new IllegalArgumentException("请选择正确的处理动作。");
    }

    private String normalizeHandleNote(String targetStatus, String note) {
        String value = note == null ? "" : note.trim();
        if (value.length() > 500) {
            value = value.substring(0, 500);
        }
        if ("ACKED".equals(targetStatus)) {
            return value.length() == 0 ? "已确认接收" : value;
        }
        if (value.length() < 5) {
            throw new IllegalArgumentException("解决或关闭消息时，请填写至少 5 个字的处理说明。");
        }
        return value;
    }

    private String normalizeSeverityForCreate(String severity) {
        String value = severity == null ? "" : severity.trim();
        if (isValidSeverity(value)) {
            return value;
        }
        return "INFO";
    }

    private boolean isValidStatus(String status) {
        return "NEW".equals(status) || "ACKED".equals(status)
                || "RESOLVED".equals(status) || "CLOSED".equals(status);
    }

    private boolean isValidSeverity(String severity) {
        return "INFO".equals(severity) || "WARNING".equals(severity)
                || "HIGH".equals(severity) || "CRITICAL".equals(severity);
    }

    private String trimToLength(String value, int maxLength, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return defaultValue;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
