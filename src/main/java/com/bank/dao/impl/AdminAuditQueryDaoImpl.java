package com.bank.dao.impl;

import com.bank.dao.AdminAuditQueryDao;
import com.bank.dto.AdminAuditLogView;
import com.bank.dto.AdminLoginLogView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminAuditQueryDaoImpl implements AdminAuditQueryDao {
    @Override
    public List<AdminAuditLogView> findAdminAuditLogs(Connection connection, String adminKeyword,
                                                      String operationType, String targetType,
                                                      Timestamp startInclusive, Timestamp endExclusive,
                                                      boolean highRiskOnly, Set<String> highRiskOperationTypes,
                                                      int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        sql.append("SELECT l.log_id, l.admin_user_id, u.username AS admin_username, l.operation_type, ")
                .append("l.target_type, l.target_id, l.detail, l.ip_address, l.created_at ")
                .append("FROM t_admin_audit_log l ")
                .append("JOIN t_user u ON u.user_id = l.admin_user_id ")
                .append("WHERE 1 = 1 ");
        if (adminKeyword != null && adminKeyword.trim().length() > 0) {
            sql.append("AND (u.username LIKE ? OR l.admin_user_id = ?) ");
            params.add("%" + adminKeyword.trim() + "%");
            params.add(parseLongOrMinusOne(adminKeyword));
        }
        if (operationType != null && operationType.trim().length() > 0) {
            sql.append("AND l.operation_type = ? ");
            params.add(operationType.trim());
        }
        if (targetType != null && targetType.trim().length() > 0) {
            sql.append("AND l.target_type = ? ");
            params.add(targetType.trim());
        }
        if (startInclusive != null) {
            sql.append("AND l.created_at >= ? ");
            params.add(startInclusive);
        }
        if (endExclusive != null) {
            sql.append("AND l.created_at < ? ");
            params.add(endExclusive);
        }
        if (highRiskOnly) {
            appendInClause(sql, params, "l.operation_type", highRiskOperationTypes);
        }
        sql.append("ORDER BY l.log_id DESC LIMIT ?");
        params.add(limit);

        List<AdminAuditLogView> logs = new ArrayList<AdminAuditLogView>();
        Set<String> highRiskSet = highRiskOperationTypes == null
                ? Collections.<String>emptySet() : new HashSet<String>(highRiskOperationTypes);
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapAuditLog(resultSet, highRiskSet));
                }
            }
        }
        return logs;
    }

    @Override
    public List<AdminLoginLogView> findLoginLogs(Connection connection, String identityKeyword, String userRole,
                                                 String successFilter, Timestamp startInclusive,
                                                 Timestamp endExclusive, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        sql.append("SELECT l.log_id, l.user_id, u.username, u.role AS user_role, l.login_identity, ")
                .append("l.ip_address, l.user_agent, l.success, l.failure_reason, l.risk_score, l.created_at ")
                .append("FROM t_login_log l ")
                .append("LEFT JOIN t_user u ON u.user_id = l.user_id ")
                .append("WHERE 1 = 1 ");
        if (identityKeyword != null && identityKeyword.trim().length() > 0) {
            sql.append("AND (l.login_identity LIKE ? OR u.username LIKE ? OR l.ip_address LIKE ?) ");
            String like = "%" + identityKeyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (userRole != null && userRole.trim().length() > 0) {
            sql.append("AND u.role = ? ");
            params.add(userRole.trim());
        }
        if ("SUCCESS".equals(successFilter)) {
            sql.append("AND l.success = 1 ");
        } else if ("FAILURE".equals(successFilter)) {
            sql.append("AND l.success = 0 ");
        }
        if (startInclusive != null) {
            sql.append("AND l.created_at >= ? ");
            params.add(startInclusive);
        }
        if (endExclusive != null) {
            sql.append("AND l.created_at < ? ");
            params.add(endExclusive);
        }
        sql.append("ORDER BY l.log_id DESC LIMIT ?");
        params.add(limit);

        List<AdminLoginLogView> logs = new ArrayList<AdminLoginLogView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapLoginLog(resultSet));
                }
            }
        }
        return logs;
    }

    @Override
    public int countAdminLogins(Connection connection, Timestamp startInclusive, Timestamp endExclusive)
            throws SQLException {
        String sql = "SELECT COUNT(*) AS count_value "
                + "FROM t_login_log l JOIN t_user u ON u.user_id = l.user_id "
                + "WHERE u.role = 'ADMIN' AND l.success = 1 AND l.created_at >= ? AND l.created_at < ?";
        return countBetween(connection, sql, startInclusive, endExclusive);
    }

    @Override
    public int countLoginFailures(Connection connection, Timestamp startInclusive, Timestamp endExclusive)
            throws SQLException {
        String sql = "SELECT COUNT(*) AS count_value FROM t_login_log "
                + "WHERE success = 0 AND created_at >= ? AND created_at < ?";
        return countBetween(connection, sql, startInclusive, endExclusive);
    }

    @Override
    public int countAuditLogsByOperation(Connection connection, String operationType, Timestamp startInclusive,
                                         Timestamp endExclusive) throws SQLException {
        String sql = "SELECT COUNT(*) AS count_value FROM t_admin_audit_log "
                + "WHERE operation_type = ? AND created_at >= ? AND created_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, operationType);
            statement.setTimestamp(2, startInclusive);
            statement.setTimestamp(3, endExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count_value");
                }
            }
        }
        return 0;
    }

    @Override
    public int countHighRiskAuditLogs(Connection connection, Set<String> highRiskOperationTypes,
                                      Timestamp startInclusive, Timestamp endExclusive) throws SQLException {
        if (highRiskOperationTypes == null || highRiskOperationTypes.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS count_value FROM t_admin_audit_log WHERE 1 = 1 ");
        List<Object> params = new ArrayList<Object>();
        appendInClause(sql, params, "operation_type", highRiskOperationTypes);
        sql.append("AND created_at >= ? AND created_at < ?");
        params.add(startInclusive);
        params.add(endExclusive);
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count_value");
                }
            }
        }
        return 0;
    }

    private int countBetween(Connection connection, String sql, Timestamp startInclusive, Timestamp endExclusive)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, startInclusive);
            statement.setTimestamp(2, endExclusive);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("count_value");
                }
            }
        }
        return 0;
    }

    private void appendInClause(StringBuilder sql, List<Object> params, String column, Set<String> values) {
        sql.append("AND ").append(column).append(" IN (");
        int index = 0;
        for (String value : values) {
            if (index++ > 0) {
                sql.append(",");
            }
            sql.append("?");
            params.add(value);
        }
        sql.append(") ");
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Timestamp) {
                statement.setTimestamp(i + 1, (Timestamp) param);
            } else if (param instanceof Integer) {
                statement.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                statement.setLong(i + 1, (Long) param);
            } else {
                statement.setString(i + 1, String.valueOf(param));
            }
        }
    }

    private long parseLongOrMinusOne(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private AdminAuditLogView mapAuditLog(ResultSet resultSet, Set<String> highRiskOperationTypes)
            throws SQLException {
        AdminAuditLogView log = new AdminAuditLogView();
        log.setLogId(resultSet.getLong("log_id"));
        log.setAdminUserId(resultSet.getLong("admin_user_id"));
        log.setAdminUsername(resultSet.getString("admin_username"));
        log.setOperationType(resultSet.getString("operation_type"));
        log.setTargetType(resultSet.getString("target_type"));
        log.setTargetId(resultSet.getString("target_id"));
        log.setDetail(resultSet.getString("detail"));
        log.setIpAddress(resultSet.getString("ip_address"));
        log.setCreatedAt(resultSet.getTimestamp("created_at"));
        log.setHighRisk(highRiskOperationTypes.contains(log.getOperationType()));
        return log;
    }

    private AdminLoginLogView mapLoginLog(ResultSet resultSet) throws SQLException {
        AdminLoginLogView log = new AdminLoginLogView();
        log.setLogId(resultSet.getLong("log_id"));
        long userId = resultSet.getLong("user_id");
        log.setUserId(resultSet.wasNull() ? null : userId);
        log.setUsername(resultSet.getString("username"));
        log.setUserRole(resultSet.getString("user_role"));
        log.setLoginIdentity(resultSet.getString("login_identity"));
        log.setIpAddress(resultSet.getString("ip_address"));
        log.setUserAgent(resultSet.getString("user_agent"));
        log.setSuccess(resultSet.getBoolean("success"));
        log.setFailureReason(resultSet.getString("failure_reason"));
        log.setRiskScore(resultSet.getInt("risk_score"));
        log.setCreatedAt(resultSet.getTimestamp("created_at"));
        return log;
    }
}
