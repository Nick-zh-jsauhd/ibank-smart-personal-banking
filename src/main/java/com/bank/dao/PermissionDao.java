package com.bank.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public interface PermissionDao {
    Set<String> findPermissionCodesByUserId(Connection connection, long userId) throws SQLException;

    Set<String> findRoleCodesByUserId(Connection connection, long userId) throws SQLException;

    void assignRoleByCode(Connection connection, long userId, String roleCode) throws SQLException;
}
