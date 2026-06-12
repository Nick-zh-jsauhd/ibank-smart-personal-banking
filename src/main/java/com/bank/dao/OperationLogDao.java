package com.bank.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface OperationLogDao {
    void insert(Connection connection, Long userId, String operationType, String businessId,
                String detail, String ipAddress) throws SQLException;
}
