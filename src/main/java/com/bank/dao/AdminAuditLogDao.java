package com.bank.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface AdminAuditLogDao {
    void insert(Connection connection, long adminUserId, String operationType, String targetType,
                String targetId, String detail, String ipAddress) throws SQLException;
}
