package com.bank.service.impl;

import com.bank.bean.Notification;
import com.bank.dao.NotificationDao;
import com.bank.dao.impl.NotificationDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.service.NotificationService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class NotificationServiceImpl implements NotificationService {
    private static final int NOTIFICATION_QUERY_LIMIT = 100;

    private final NotificationDao notificationDao = new NotificationDaoImpl();

    @Override
    public void create(Connection connection, long customerId, Long userId, String notificationType, String title,
                       String content, String businessType, String businessId) throws SQLException {
        Notification notification = new Notification();
        notification.setCustomerId(customerId);
        notification.setUserId(userId);
        notification.setNotificationType(trimToLength(notificationType, 30, "SYSTEM"));
        notification.setTitle(trimToLength(title, 120, "系统通知"));
        notification.setContent(trimToLength(content, 500, ""));
        notification.setBusinessType(trimToLength(businessType, 40, null));
        notification.setBusinessId(trimToLength(businessId, 64, null));
        notificationDao.insert(connection, notification);
    }

    @Override
    public ServiceResult<List<Notification>> listNotifications(long customerId) {
        return listNotifications(customerId, null);
    }

    @Override
    public ServiceResult<List<Notification>> listNotifications(long customerId, String notificationType) {
        String normalizedType = normalizeType(notificationType);
        try (Connection connection = DBUtil.getConnection()) {
            List<Notification> notifications = normalizedType == null
                    ? notificationDao.findByCustomer(connection, customerId, NOTIFICATION_QUERY_LIMIT)
                    : notificationDao.findByCustomerAndType(connection, customerId, normalizedType,
                            NOTIFICATION_QUERY_LIMIT);
            return ServiceResult.success("通知查询成功。",
                    notifications);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("通知查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Integer> countUnread(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("未读通知数查询成功。", notificationDao.countUnread(connection, customerId));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("未读通知数查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> markRead(long customerId, long notificationId) {
        try (Connection connection = DBUtil.getConnection()) {
            notificationDao.markRead(connection, customerId, notificationId);
            return ServiceResult.success("通知已标记为已读。", null);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("通知状态更新失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> markAllRead(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            notificationDao.markAllRead(connection, customerId);
            return ServiceResult.success("全部通知已标记为已读。", null);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("通知状态更新失败，请检查数据库状态或稍后重试。");
        }
    }

    private String trimToLength(String value, int maxLength, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return defaultValue;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private String normalizeType(String notificationType) {
        if (notificationType == null || notificationType.trim().length() == 0 || "ALL".equals(notificationType)) {
            return null;
        }
        String value = notificationType.trim();
        if ("TRANSACTION".equals(value) || "RISK".equals(value)
                || "WEALTH".equals(value) || "SECURITY".equals(value)
                || "SERVICE".equals(value) || "SYSTEM".equals(value)) {
            return value;
        }
        return null;
    }
}
