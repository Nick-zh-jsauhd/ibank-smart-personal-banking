package com.bank.dao.impl;

import com.bank.dao.PermissionDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class PermissionDaoImpl implements PermissionDao {
    @Override
    public Set<String> findPermissionCodesByUserId(Connection connection, long userId) throws SQLException {
        String sql = "SELECT DISTINCT p.permission_code "
                + "FROM t_admin_user_role ur "
                + "JOIN t_admin_role r ON ur.role_id = r.role_id AND r.status = 'ACTIVE' "
                + "JOIN t_admin_role_permission rp ON r.role_id = rp.role_id "
                + "JOIN t_admin_permission p ON rp.permission_id = p.permission_id "
                + "WHERE ur.user_id = ?";
        Set<String> permissions = new HashSet<String>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    permissions.add(resultSet.getString("permission_code"));
                }
            }
        }
        return permissions;
    }

    @Override
    public Set<String> findRoleCodesByUserId(Connection connection, long userId) throws SQLException {
        String sql = "SELECT DISTINCT r.role_code "
                + "FROM t_admin_user_role ur "
                + "JOIN t_admin_role r ON ur.role_id = r.role_id AND r.status = 'ACTIVE' "
                + "WHERE ur.user_id = ?";
        Set<String> roles = new HashSet<String>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    roles.add(resultSet.getString("role_code"));
                }
            }
        }
        return roles;
    }

    @Override
    public void assignRoleByCode(Connection connection, long userId, String roleCode) throws SQLException {
        String sql = "INSERT INTO t_admin_user_role (user_id, role_id) "
                + "SELECT ?, role_id FROM t_admin_role WHERE role_code = ? "
                + "ON DUPLICATE KEY UPDATE user_id = user_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, roleCode);
            statement.executeUpdate();
        }
    }
}
