package com.bank.dao;

import com.bank.dto.AdminRoleView;
import com.bank.dto.AdminUserView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface AdminSecurityDao {
    List<AdminUserView> findAdminUsers(Connection connection, String keyword, String status, int limit)
            throws SQLException;

    AdminUserView findAdminUserById(Connection connection, long userId) throws SQLException;

    List<AdminRoleView> findRolesWithPermissions(Connection connection) throws SQLException;

    Set<String> findActiveRoleCodes(Connection connection, List<String> roleCodes) throws SQLException;

    Set<String> findUserRoleCodes(Connection connection, long userId) throws SQLException;

    void replaceUserRoles(Connection connection, long userId, List<String> roleCodes) throws SQLException;

    void updateAdminStatus(Connection connection, long userId, String status) throws SQLException;

    void updateAdminPassword(Connection connection, long userId, String passwordHash) throws SQLException;

    int countActiveSuperAdmins(Connection connection) throws SQLException;
}
