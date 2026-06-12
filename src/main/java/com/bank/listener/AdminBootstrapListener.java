package com.bank.listener;

import com.bank.bean.User;
import com.bank.dao.PermissionDao;
import com.bank.dao.UserDao;
import com.bank.dao.impl.PermissionDaoImpl;
import com.bank.dao.impl.UserDaoImpl;
import com.bank.util.DBUtil;
import com.bank.util.PasswordUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.SQLException;

@WebListener
public class AdminBootstrapListener implements ServletContextListener {
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PHONE = "18800000000";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_REVIEWER_USERNAME = "admin_reviewer";
    private static final String DEFAULT_REVIEWER_PHONE = "18800000001";

    private final UserDao userDao = new UserDaoImpl();
    private final PermissionDao permissionDao = new PermissionDaoImpl();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try (Connection connection = DBUtil.getConnection()) {
            User admin = ensureAdminUser(connection, DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PHONE);
            assignRoleQuietly(connection, admin.getUserId(), "SUPER_ADMIN", sce);

            User reviewer = ensureAdminUser(connection, DEFAULT_REVIEWER_USERNAME, DEFAULT_REVIEWER_PHONE);
            assignRoleQuietly(connection, reviewer.getUserId(), "ACCOUNTING_REVIEWER", sce);
        } catch (SQLException e) {
            sce.getServletContext().log("Failed to bootstrap default admin user", e);
        }
    }

    private User ensureAdminUser(Connection connection, String username, String phone) throws SQLException {
        User existing = userDao.findByIdentity(connection, username);
        if (existing != null) {
            return existing;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPhone(phone);
        admin.setPasswordHash(PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD));
        admin.setPayPasswordHash(null);
        admin.setRole("ADMIN");
        admin.setStatus("NORMAL");
        long userId = userDao.insert(connection, admin);
        admin.setUserId(userId);
        return admin;
    }

    private void assignRoleQuietly(Connection connection, long userId, String roleCode, ServletContextEvent sce) {
        try {
            permissionDao.assignRoleByCode(connection, userId, roleCode);
        } catch (SQLException e) {
            sce.getServletContext().log("Failed to assign admin role: " + roleCode, e);
        }
    }
}
