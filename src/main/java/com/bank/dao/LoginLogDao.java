package com.bank.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface LoginLogDao {
    void insert(Connection connection, Long userId, String identity, String ipAddress, String userAgent,
                boolean success, String failureReason, int riskScore) throws SQLException;
}
