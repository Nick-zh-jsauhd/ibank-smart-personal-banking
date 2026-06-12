package com.bank.dao;

import com.bank.bean.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public interface UserDao {
    User findById(Connection connection, long userId) throws SQLException;

    User findByIdentity(Connection connection, String identity) throws SQLException;

    boolean existsByUsername(Connection connection, String username) throws SQLException;

    boolean existsByPhone(Connection connection, String phone) throws SQLException;

    long insert(Connection connection, User user) throws SQLException;

    void updateCustomerId(Connection connection, long userId, long customerId) throws SQLException;

    void updatePayPassword(Connection connection, long userId, String payPasswordHash) throws SQLException;

    void recordLoginSuccess(Connection connection, long userId) throws SQLException;

    void recordLoginFailure(Connection connection, long userId, int failedCount, Timestamp lockedUntil) throws SQLException;
}
