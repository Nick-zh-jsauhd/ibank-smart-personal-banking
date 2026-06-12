package com.bank.dao.impl;

import com.bank.dao.LoginLogDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoginLogDaoImpl implements LoginLogDao {
    @Override
    public void insert(Connection connection, Long userId, String identity, String ipAddress, String userAgent,
                       boolean success, String failureReason, int riskScore) throws SQLException {
        String sql = "INSERT INTO t_login_log (user_id, login_identity, ip_address, user_agent, success, "
                + "failure_reason, risk_score) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, identity);
            statement.setString(3, ipAddress);
            statement.setString(4, userAgent);
            statement.setBoolean(5, success);
            statement.setString(6, failureReason);
            statement.setInt(7, riskScore);
            statement.executeUpdate();
        }
    }
}
