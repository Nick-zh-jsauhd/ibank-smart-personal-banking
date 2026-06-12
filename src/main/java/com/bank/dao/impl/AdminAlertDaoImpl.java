package com.bank.dao.impl;

import com.bank.bean.AdminAlert;
import com.bank.dao.AdminAlertDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdminAlertDaoImpl implements AdminAlertDao {
    @Override
    public long insert(Connection connection, AdminAlert alert) throws SQLException {
        String sql = "INSERT INTO t_admin_alert (alert_type, severity, title, content, target_type, target_id, "
                + "responsible_role_code, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, alert.getAlertType());
            statement.setString(2, alert.getSeverity());
            statement.setString(3, alert.getTitle());
            statement.setString(4, alert.getContent());
            statement.setString(5, alert.getTargetType());
            statement.setString(6, alert.getTargetId());
            statement.setString(7, alert.getResponsibleRoleCode());
            statement.setString(8, alert.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No generated key returned");
    }

    @Override
    public AdminAlert findOpenByBusinessKey(Connection connection, String alertType, String targetType,
                                            String targetId) throws SQLException {
        String sql = selectSql() + " WHERE a.alert_type = ? AND a.target_type <=> ? AND a.target_id <=> ? "
                + "AND a.status IN ('NEW', 'ACKED') ORDER BY a.alert_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, alertType);
            statement.setString(2, targetType);
            statement.setString(3, targetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAlert(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<AdminAlert> findVisible(Connection connection, Set<String> roleCodes, long adminUserId,
                                        boolean viewAll, String status, String severity, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();
        sql.append(selectSql()).append(" WHERE 1 = 1 ");
        appendVisibility(sql, params, roleCodes, adminUserId, viewAll);
        if (status != null && status.length() > 0) {
            sql.append("AND a.status = ? ");
            params.add(status);
        }
        if (severity != null && severity.length() > 0) {
            sql.append("AND a.severity = ? ");
            params.add(severity);
        }
        sql.append("ORDER BY FIELD(a.status, 'NEW', 'ACKED', 'RESOLVED', 'CLOSED'), ");
        sql.append("FIELD(a.severity, 'CRITICAL', 'HIGH', 'WARNING', 'INFO'), ");
        sql.append("a.created_at DESC, a.alert_id DESC LIMIT ?");
        params.add(Integer.valueOf(limit));

        List<AdminAlert> alerts = new ArrayList<AdminAlert>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    alerts.add(mapAlert(resultSet));
                }
            }
        }
        return alerts;
    }

    @Override
    public int countOpenVisible(Connection connection, Set<String> roleCodes, long adminUserId, boolean viewAll)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM t_admin_alert a WHERE ");
        List<Object> params = new ArrayList<Object>();
        sql.append("a.status IN ('NEW', 'ACKED') ");
        appendVisibility(sql, params, roleCodes, adminUserId, viewAll);
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public AdminAlert findById(Connection connection, long alertId) throws SQLException {
        return findById(connection, alertId, false);
    }

    @Override
    public AdminAlert findByIdForUpdate(Connection connection, long alertId) throws SQLException {
        return findById(connection, alertId, true);
    }

    @Override
    public void updateStatus(Connection connection, long alertId, long adminUserId, String status, String note)
            throws SQLException {
        String sql;
        if ("ACKED".equals(status)) {
            sql = "UPDATE t_admin_alert SET status = ?, assigned_admin_user_id = COALESCE(assigned_admin_user_id, ?), "
                    + "handle_note = ?, updated_at = NOW() WHERE alert_id = ?";
        } else {
            sql = "UPDATE t_admin_alert SET status = ?, assigned_admin_user_id = COALESCE(assigned_admin_user_id, ?), "
                    + "handled_by_admin_user_id = ?, handle_note = ?, handled_at = NOW(), updated_at = NOW() "
                    + "WHERE alert_id = ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, adminUserId);
            if ("ACKED".equals(status)) {
                statement.setString(3, note);
                statement.setLong(4, alertId);
            } else {
                statement.setLong(3, adminUserId);
                statement.setString(4, note);
                statement.setLong(5, alertId);
            }
            statement.executeUpdate();
        }
    }

    @Override
    public int updateOpenStatusByBusinessKey(Connection connection, String alertType, String targetType,
                                             String targetId, long adminUserId, String status, String note)
            throws SQLException {
        String sql;
        if ("ACKED".equals(status)) {
            sql = "UPDATE t_admin_alert SET status = IF(status = 'NEW', 'ACKED', status), "
                    + "assigned_admin_user_id = COALESCE(assigned_admin_user_id, ?), handle_note = ?, "
                    + "updated_at = NOW() WHERE alert_type = ? AND target_type <=> ? AND target_id <=> ? "
                    + "AND status IN ('NEW', 'ACKED')";
        } else {
            sql = "UPDATE t_admin_alert SET status = ?, "
                    + "assigned_admin_user_id = COALESCE(assigned_admin_user_id, ?), "
                    + "handled_by_admin_user_id = ?, handle_note = ?, handled_at = NOW(), updated_at = NOW() "
                    + "WHERE alert_type = ? AND target_type <=> ? AND target_id <=> ? "
                    + "AND status IN ('NEW', 'ACKED')";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if ("ACKED".equals(status)) {
                statement.setLong(1, adminUserId);
                statement.setString(2, note);
                statement.setString(3, alertType);
                statement.setString(4, targetType);
                statement.setString(5, targetId);
            } else {
                statement.setString(1, status);
                statement.setLong(2, adminUserId);
                statement.setLong(3, adminUserId);
                statement.setString(4, note);
                statement.setString(5, alertType);
                statement.setString(6, targetType);
                statement.setString(7, targetId);
            }
            return statement.executeUpdate();
        }
    }

    private AdminAlert findById(Connection connection, long alertId, boolean forUpdate) throws SQLException {
        String sql = selectSql() + " WHERE a.alert_id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, alertId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAlert(resultSet);
                }
            }
        }
        return null;
    }

    private String selectSql() {
        return "SELECT a.alert_id, a.alert_type, a.severity, a.title, a.content, a.target_type, a.target_id, "
                + "a.responsible_role_code, a.status, a.assigned_admin_user_id, au.username AS assigned_username, "
                + "a.handled_by_admin_user_id, hu.username AS handled_username, a.handle_note, "
                + "a.handled_at, a.created_at, a.updated_at "
                + "FROM t_admin_alert a "
                + "LEFT JOIN t_user au ON a.assigned_admin_user_id = au.user_id "
                + "LEFT JOIN t_user hu ON a.handled_by_admin_user_id = hu.user_id";
    }

    private void appendVisibility(StringBuilder sql, List<Object> params, Set<String> roleCodes,
                                  long adminUserId, boolean viewAll) {
        if (viewAll) {
            return;
        }
        sql.append("AND (a.responsible_role_code IS NULL OR a.assigned_admin_user_id = ? ");
        params.add(Long.valueOf(adminUserId));
        if (roleCodes != null && !roleCodes.isEmpty()) {
            sql.append("OR a.responsible_role_code IN (");
            int index = 0;
            for (String roleCode : roleCodes) {
                if (index++ > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                params.add(roleCode);
            }
            sql.append(") ");
        }
        sql.append(") ");
    }

    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param == null) {
                statement.setNull(i + 1, Types.VARCHAR);
            } else if (param instanceof Integer) {
                statement.setInt(i + 1, ((Integer) param).intValue());
            } else if (param instanceof Long) {
                statement.setLong(i + 1, ((Long) param).longValue());
            } else {
                statement.setString(i + 1, String.valueOf(param));
            }
        }
    }

    private AdminAlert mapAlert(ResultSet resultSet) throws SQLException {
        AdminAlert alert = new AdminAlert();
        alert.setAlertId(resultSet.getLong("alert_id"));
        alert.setAlertType(resultSet.getString("alert_type"));
        alert.setSeverity(resultSet.getString("severity"));
        alert.setTitle(resultSet.getString("title"));
        alert.setContent(resultSet.getString("content"));
        alert.setTargetType(resultSet.getString("target_type"));
        alert.setTargetId(resultSet.getString("target_id"));
        alert.setResponsibleRoleCode(resultSet.getString("responsible_role_code"));
        alert.setStatus(resultSet.getString("status"));
        long assignedAdminUserId = resultSet.getLong("assigned_admin_user_id");
        alert.setAssignedAdminUserId(resultSet.wasNull() ? null : Long.valueOf(assignedAdminUserId));
        alert.setAssignedAdminUsername(resultSet.getString("assigned_username"));
        long handledByAdminUserId = resultSet.getLong("handled_by_admin_user_id");
        alert.setHandledByAdminUserId(resultSet.wasNull() ? null : Long.valueOf(handledByAdminUserId));
        alert.setHandledByAdminUsername(resultSet.getString("handled_username"));
        alert.setHandleNote(resultSet.getString("handle_note"));
        alert.setHandledAt(resultSet.getTimestamp("handled_at"));
        alert.setCreatedAt(resultSet.getTimestamp("created_at"));
        alert.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return alert;
    }
}
