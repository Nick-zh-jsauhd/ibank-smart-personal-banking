package com.bank.service;

import com.bank.bean.AdminAlert;
import com.bank.dto.ServiceResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface AdminAlertService {
    void create(Connection connection, String alertType, String severity, String title, String content,
                String targetType, String targetId, String responsibleRoleCode) throws SQLException;

    long createOrReuse(Connection connection, String alertType, String severity, String title, String content,
                       String targetType, String targetId, String responsibleRoleCode) throws SQLException;

    ServiceResult<List<AdminAlert>> listAlerts(long adminUserId, String status, String severity);

    ServiceResult<AdminAlert> getAlert(long adminUserId, long alertId);

    ServiceResult<Integer> countOpenAlerts(long adminUserId);

    ServiceResult<Void> handleAlert(long adminUserId, long alertId, String action, String note, String ipAddress);

    void ackByTarget(Connection connection, String alertType, String targetType, String targetId,
                     long adminUserId, String note) throws SQLException;

    void resolveByTarget(Connection connection, String alertType, String targetType, String targetId,
                         long adminUserId, String note) throws SQLException;

    void closeByTarget(Connection connection, String alertType, String targetType, String targetId,
                       long adminUserId, String note) throws SQLException;
}
