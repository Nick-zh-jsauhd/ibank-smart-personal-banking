package com.bank.dao;

import com.bank.bean.AdminAlert;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface AdminAlertDao {
    long insert(Connection connection, AdminAlert alert) throws SQLException;

    AdminAlert findOpenByBusinessKey(Connection connection, String alertType, String targetType,
                                     String targetId) throws SQLException;

    List<AdminAlert> findVisible(Connection connection, Set<String> roleCodes, long adminUserId,
                                 boolean viewAll, String status, String severity, int limit)
            throws SQLException;

    int countOpenVisible(Connection connection, Set<String> roleCodes, long adminUserId, boolean viewAll)
            throws SQLException;

    AdminAlert findById(Connection connection, long alertId) throws SQLException;

    AdminAlert findByIdForUpdate(Connection connection, long alertId) throws SQLException;

    void updateStatus(Connection connection, long alertId, long adminUserId, String status, String note)
            throws SQLException;

    int updateOpenStatusByBusinessKey(Connection connection, String alertType, String targetType,
                                      String targetId, long adminUserId, String status, String note)
            throws SQLException;
}
