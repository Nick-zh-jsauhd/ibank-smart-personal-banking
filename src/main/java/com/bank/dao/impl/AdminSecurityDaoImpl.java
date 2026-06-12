package com.bank.dao.impl;

import com.bank.dao.AdminSecurityDao;
import com.bank.dto.AdminPermissionView;
import com.bank.dto.AdminRoleView;
import com.bank.dto.AdminUserView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminSecurityDaoImpl implements AdminSecurityDao {
    @Override
    public List<AdminUserView> findAdminUsers(Connection connection, String keyword, String status, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT u.user_id, u.username, u.phone, u.status, u.failed_login_count, ")
                .append("u.locked_until, u.last_login_at, u.created_at, u.updated_at, ")
                .append("GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ',') AS role_codes, ")
                .append("GROUP_CONCAT(r.role_name ORDER BY r.role_code SEPARATOR ',') AS role_names ")
                .append("FROM t_user u ")
                .append("LEFT JOIN t_admin_user_role ur ON ur.user_id = u.user_id ")
                .append("LEFT JOIN t_admin_role r ON r.role_id = ur.role_id ")
                .append("WHERE u.role = 'ADMIN' ");
        List<String> params = new ArrayList<String>();
        if (keyword != null && keyword.trim().length() > 0) {
            sql.append("AND (u.username LIKE ? OR u.phone LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null && status.trim().length() > 0) {
            sql.append("AND u.status = ? ");
            params.add(status.trim());
        }
        sql.append("GROUP BY u.user_id, u.username, u.phone, u.status, u.failed_login_count, ")
                .append("u.locked_until, u.last_login_at, u.created_at, u.updated_at ")
                .append("ORDER BY u.created_at DESC LIMIT ?");
        List<AdminUserView> users = new ArrayList<AdminUserView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (String param : params) {
                statement.setString(index++, param);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapAdminUser(resultSet));
                }
            }
        }
        return users;
    }

    @Override
    public AdminUserView findAdminUserById(Connection connection, long userId) throws SQLException {
        String sql = "SELECT u.user_id, u.username, u.phone, u.status, u.failed_login_count, "
                + "u.locked_until, u.last_login_at, u.created_at, u.updated_at, "
                + "GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ',') AS role_codes, "
                + "GROUP_CONCAT(r.role_name ORDER BY r.role_code SEPARATOR ',') AS role_names "
                + "FROM t_user u "
                + "LEFT JOIN t_admin_user_role ur ON ur.user_id = u.user_id "
                + "LEFT JOIN t_admin_role r ON r.role_id = ur.role_id "
                + "WHERE u.role = 'ADMIN' AND u.user_id = ? "
                + "GROUP BY u.user_id, u.username, u.phone, u.status, u.failed_login_count, "
                + "u.locked_until, u.last_login_at, u.created_at, u.updated_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAdminUser(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<AdminRoleView> findRolesWithPermissions(Connection connection) throws SQLException {
        String sql = "SELECT r.role_id, r.role_code, r.role_name, r.description AS role_description, "
                + "r.status AS role_status, p.permission_id, p.permission_code, p.permission_name, "
                + "p.permission_group AS module, p.description AS permission_description "
                + "FROM t_admin_role r "
                + "LEFT JOIN t_admin_role_permission rp ON rp.role_id = r.role_id "
                + "LEFT JOIN t_admin_permission p ON p.permission_id = rp.permission_id "
                + "ORDER BY r.role_code, p.permission_group, p.permission_code";
        Map<Long, AdminRoleView> roles = new LinkedHashMap<Long, AdminRoleView>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long roleId = resultSet.getLong("role_id");
                AdminRoleView role = roles.get(roleId);
                if (role == null) {
                    role = new AdminRoleView();
                    role.setRoleId(roleId);
                    role.setRoleCode(resultSet.getString("role_code"));
                    role.setRoleName(resultSet.getString("role_name"));
                    role.setDescription(resultSet.getString("role_description"));
                    role.setStatus(resultSet.getString("role_status"));
                    roles.put(roleId, role);
                }
                long permissionId = resultSet.getLong("permission_id");
                if (!resultSet.wasNull()) {
                    AdminPermissionView permission = new AdminPermissionView();
                    permission.setPermissionId(permissionId);
                    permission.setPermissionCode(resultSet.getString("permission_code"));
                    permission.setPermissionName(resultSet.getString("permission_name"));
                    permission.setModule(resultSet.getString("module"));
                    permission.setDescription(resultSet.getString("permission_description"));
                    role.addPermission(permission);
                }
            }
        }
        return new ArrayList<AdminRoleView>(roles.values());
    }

    @Override
    public Set<String> findActiveRoleCodes(Connection connection, List<String> roleCodes) throws SQLException {
        Set<String> activeRoleCodes = new HashSet<String>();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return activeRoleCodes;
        }
        StringBuilder sql = new StringBuilder("SELECT role_code FROM t_admin_role WHERE status = 'ACTIVE' AND role_code IN (");
        for (int i = 0; i < roleCodes.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(")");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < roleCodes.size(); i++) {
                statement.setString(i + 1, roleCodes.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    activeRoleCodes.add(resultSet.getString("role_code"));
                }
            }
        }
        return activeRoleCodes;
    }

    @Override
    public Set<String> findUserRoleCodes(Connection connection, long userId) throws SQLException {
        String sql = "SELECT r.role_code "
                + "FROM t_admin_user_role ur "
                + "JOIN t_admin_role r ON r.role_id = ur.role_id "
                + "WHERE ur.user_id = ?";
        Set<String> roleCodes = new HashSet<String>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    roleCodes.add(resultSet.getString("role_code"));
                }
            }
        }
        return roleCodes;
    }

    @Override
    public void replaceUserRoles(Connection connection, long userId, List<String> roleCodes) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM t_admin_user_role WHERE user_id = ?")) {
            delete.setLong(1, userId);
            delete.executeUpdate();
        }
        String sql = "INSERT INTO t_admin_user_role (user_id, role_id) "
                + "SELECT ?, role_id FROM t_admin_role WHERE role_code = ? AND status = 'ACTIVE'";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (String roleCode : roleCodes) {
                insert.setLong(1, userId);
                insert.setString(2, roleCode);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    @Override
    public void updateAdminStatus(Connection connection, long userId, String status) throws SQLException {
        String sql = "UPDATE t_user SET status = ?, failed_login_count = 0, locked_until = NULL WHERE user_id = ? "
                + "AND role = 'ADMIN'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateAdminPassword(Connection connection, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE t_user SET password_hash = ?, failed_login_count = 0, locked_until = NULL "
                + "WHERE user_id = ? AND role = 'ADMIN'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    @Override
    public int countActiveSuperAdmins(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT u.user_id) AS count_value "
                + "FROM t_user u "
                + "JOIN t_admin_user_role ur ON ur.user_id = u.user_id "
                + "JOIN t_admin_role r ON r.role_id = ur.role_id "
                + "WHERE u.role = 'ADMIN' AND u.status = 'NORMAL' "
                + "AND r.role_code = 'SUPER_ADMIN' AND r.status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt("count_value");
            }
        }
        return 0;
    }

    private AdminUserView mapAdminUser(ResultSet resultSet) throws SQLException {
        AdminUserView user = new AdminUserView();
        user.setUserId(resultSet.getLong("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPhone(resultSet.getString("phone"));
        user.setStatus(resultSet.getString("status"));
        user.setFailedLoginCount(resultSet.getInt("failed_login_count"));
        user.setLockedUntil(resultSet.getTimestamp("locked_until"));
        user.setLastLoginAt(resultSet.getTimestamp("last_login_at"));
        user.setCreatedAt(resultSet.getTimestamp("created_at"));
        user.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        user.setRoleCodesFromCsv(resultSet.getString("role_codes"));
        user.setRoleNamesFromCsv(resultSet.getString("role_names"));
        return user;
    }
}
