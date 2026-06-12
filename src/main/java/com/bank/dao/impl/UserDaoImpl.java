package com.bank.dao.impl;

import com.bank.bean.User;
import com.bank.dao.UserDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class UserDaoImpl implements UserDao {
    @Override
    public User findById(Connection connection, long userId) throws SQLException {
        String sql = "SELECT user_id, customer_id, username, phone, password_hash, pay_password_hash, role, status, "
                + "failed_login_count, locked_until, last_login_at, created_at, updated_at "
                + "FROM t_user WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public User findByIdentity(Connection connection, String identity) throws SQLException {
        String sql = "SELECT user_id, customer_id, username, phone, password_hash, pay_password_hash, role, status, "
                + "failed_login_count, locked_until, last_login_at, created_at, updated_at "
                + "FROM t_user WHERE username = ? OR phone = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identity);
            statement.setString(2, identity);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public boolean existsByUsername(Connection connection, String username) throws SQLException {
        return exists(connection, "SELECT 1 FROM t_user WHERE username = ?", username);
    }

    @Override
    public boolean existsByPhone(Connection connection, String phone) throws SQLException {
        return exists(connection, "SELECT 1 FROM t_user WHERE phone = ?", phone);
    }

    @Override
    public long insert(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO t_user (username, phone, password_hash, pay_password_hash, role, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPhone());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getPayPasswordHash());
            statement.setString(5, user.getRole());
            statement.setString(6, user.getStatus());
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    @Override
    public void updateCustomerId(Connection connection, long userId, long customerId) throws SQLException {
        String sql = "UPDATE t_user SET customer_id = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updatePayPassword(Connection connection, long userId, String payPasswordHash) throws SQLException {
        String sql = "UPDATE t_user SET pay_password_hash = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, payPasswordHash);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    @Override
    public void recordLoginSuccess(Connection connection, long userId) throws SQLException {
        String sql = "UPDATE t_user SET failed_login_count = 0, locked_until = NULL, last_login_at = NOW() "
                + "WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    @Override
    public void recordLoginFailure(Connection connection, long userId, int failedCount, Timestamp lockedUntil)
            throws SQLException {
        String sql = "UPDATE t_user SET failed_login_count = ?, locked_until = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, failedCount);
            statement.setTimestamp(2, lockedUntil);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("No generated key returned");
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getLong("user_id"));
        long customerId = resultSet.getLong("customer_id");
        user.setCustomerId(resultSet.wasNull() ? null : customerId);
        user.setUsername(resultSet.getString("username"));
        user.setPhone(resultSet.getString("phone"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setPayPasswordHash(resultSet.getString("pay_password_hash"));
        user.setRole(resultSet.getString("role"));
        user.setStatus(resultSet.getString("status"));
        user.setFailedLoginCount(resultSet.getInt("failed_login_count"));
        user.setLockedUntil(resultSet.getTimestamp("locked_until"));
        user.setLastLoginAt(resultSet.getTimestamp("last_login_at"));
        user.setCreatedAt(resultSet.getTimestamp("created_at"));
        user.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return user;
    }
}
