package com.bank.dao.impl;

import com.bank.dao.AdminAuditLogDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AdminAuditLogDaoImpl implements AdminAuditLogDao {
    @Override
    public void insert(Connection connection, long adminUserId, String operationType, String targetType,
                       String targetId, String detail, String ipAddress) throws SQLException {
        String sql = "INSERT INTO t_admin_audit_log (admin_user_id, operation_type, target_type, target_id, "
                + "detail, ip_address) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setString(2, operationType);
            statement.setString(3, targetType);
            statement.setString(4, targetId);
            statement.setString(5, detail);
            statement.setString(6, ipAddress);
            statement.executeUpdate();
        }
    }
}
