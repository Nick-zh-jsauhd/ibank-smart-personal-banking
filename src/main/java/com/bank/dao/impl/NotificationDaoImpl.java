package com.bank.dao.impl;

import com.bank.bean.Notification;
import com.bank.dao.NotificationDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class NotificationDaoImpl implements NotificationDao {
    @Override
    public long insert(Connection connection, Notification notification) throws SQLException {
        String sql = "INSERT INTO t_notification (customer_id, user_id, notification_type, title, content, "
                + "business_type, business_id, read_flag) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, notification.getCustomerId());
            if (notification.getUserId() == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, notification.getUserId());
            }
            statement.setString(3, notification.getNotificationType());
            statement.setString(4, notification.getTitle());
            statement.setString(5, notification.getContent());
            statement.setString(6, notification.getBusinessType());
            statement.setString(7, notification.getBusinessId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No generated key returned");
    }

    @Override
    public int countUnread(Connection connection, long customerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_notification WHERE customer_id = ? AND read_flag = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public List<Notification> findByCustomer(Connection connection, long customerId, int limit) throws SQLException {
        String sql = "SELECT notification_id, customer_id, user_id, notification_type, title, content, "
                + "business_type, business_id, read_flag, read_at, created_at "
                + "FROM t_notification WHERE customer_id = ? ORDER BY created_at DESC, notification_id DESC LIMIT ?";
        List<Notification> notifications = new ArrayList<Notification>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }
        }
        return notifications;
    }

    @Override
    public List<Notification> findByCustomerAndType(Connection connection, long customerId, String notificationType,
                                                    int limit) throws SQLException {
        String sql = "SELECT notification_id, customer_id, user_id, notification_type, title, content, "
                + "business_type, business_id, read_flag, read_at, created_at "
                + "FROM t_notification WHERE customer_id = ? AND notification_type = ? "
                + "ORDER BY created_at DESC, notification_id DESC LIMIT ?";
        List<Notification> notifications = new ArrayList<Notification>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, notificationType);
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }
        }
        return notifications;
    }

    @Override
    public void markRead(Connection connection, long customerId, long notificationId) throws SQLException {
        String sql = "UPDATE t_notification SET read_flag = 1, read_at = NOW() "
                + "WHERE notification_id = ? AND customer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, notificationId);
            statement.setLong(2, customerId);
            statement.executeUpdate();
        }
    }

    @Override
    public void markAllRead(Connection connection, long customerId) throws SQLException {
        String sql = "UPDATE t_notification SET read_flag = 1, read_at = NOW() "
                + "WHERE customer_id = ? AND read_flag = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.executeUpdate();
        }
    }

    private Notification mapNotification(ResultSet resultSet) throws SQLException {
        Notification notification = new Notification();
        notification.setNotificationId(resultSet.getLong("notification_id"));
        notification.setCustomerId(resultSet.getLong("customer_id"));
        long userId = resultSet.getLong("user_id");
        notification.setUserId(resultSet.wasNull() ? null : userId);
        notification.setNotificationType(resultSet.getString("notification_type"));
        notification.setTitle(resultSet.getString("title"));
        notification.setContent(resultSet.getString("content"));
        notification.setBusinessType(resultSet.getString("business_type"));
        notification.setBusinessId(resultSet.getString("business_id"));
        notification.setReadFlag(resultSet.getBoolean("read_flag"));
        notification.setReadAt(resultSet.getTimestamp("read_at"));
        notification.setCreatedAt(resultSet.getTimestamp("created_at"));
        return notification;
    }
}
