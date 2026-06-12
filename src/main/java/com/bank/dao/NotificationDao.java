package com.bank.dao;

import com.bank.bean.Notification;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface NotificationDao {
    long insert(Connection connection, Notification notification) throws SQLException;

    int countUnread(Connection connection, long customerId) throws SQLException;

    List<Notification> findByCustomer(Connection connection, long customerId, int limit) throws SQLException;

    List<Notification> findByCustomerAndType(Connection connection, long customerId, String notificationType,
                                             int limit) throws SQLException;

    void markRead(Connection connection, long customerId, long notificationId) throws SQLException;

    void markAllRead(Connection connection, long customerId) throws SQLException;
}
