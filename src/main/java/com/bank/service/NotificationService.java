package com.bank.service;

import com.bank.bean.Notification;
import com.bank.dto.ServiceResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface NotificationService {
    void create(Connection connection, long customerId, Long userId, String notificationType, String title,
                String content, String businessType, String businessId) throws SQLException;

    ServiceResult<List<Notification>> listNotifications(long customerId);

    ServiceResult<List<Notification>> listNotifications(long customerId, String notificationType);

    ServiceResult<Integer> countUnread(long customerId);

    ServiceResult<Void> markRead(long customerId, long notificationId);

    ServiceResult<Void> markAllRead(long customerId);
}
