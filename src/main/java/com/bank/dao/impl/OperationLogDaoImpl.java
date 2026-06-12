package com.bank.dao.impl;

import com.bank.dao.OperationLogDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class OperationLogDaoImpl implements OperationLogDao {
    @Override
    public void insert(Connection connection, Long userId, String operationType, String businessId,
                       String detail, String ipAddress) throws SQLException {
        String sql = "INSERT INTO t_operation_log (user_id, operation_type, business_id, detail, ip_address) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, operationType);
            statement.setString(3, businessId);
            statement.setString(4, detail);
            statement.setString(5, ipAddress);
            statement.executeUpdate();
        }
    }
}
