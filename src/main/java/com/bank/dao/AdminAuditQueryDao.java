package com.bank.dao;

import com.bank.dto.AdminAuditLogView;
import com.bank.dto.AdminLoginLogView;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

public interface AdminAuditQueryDao {
    List<AdminAuditLogView> findAdminAuditLogs(Connection connection, String adminKeyword,
                                               String operationType, String targetType,
                                               Timestamp startInclusive, Timestamp endExclusive,
                                               boolean highRiskOnly, Set<String> highRiskOperationTypes,
                                               int limit) throws SQLException;

    List<AdminLoginLogView> findLoginLogs(Connection connection, String identityKeyword, String userRole,
                                          String successFilter, Timestamp startInclusive,
                                          Timestamp endExclusive, int limit) throws SQLException;

    int countAdminLogins(Connection connection, Timestamp startInclusive, Timestamp endExclusive)
            throws SQLException;

    int countLoginFailures(Connection connection, Timestamp startInclusive, Timestamp endExclusive)
            throws SQLException;

    int countAuditLogsByOperation(Connection connection, String operationType, Timestamp startInclusive,
                                  Timestamp endExclusive) throws SQLException;

    int countHighRiskAuditLogs(Connection connection, Set<String> highRiskOperationTypes,
                               Timestamp startInclusive, Timestamp endExclusive) throws SQLException;
}
