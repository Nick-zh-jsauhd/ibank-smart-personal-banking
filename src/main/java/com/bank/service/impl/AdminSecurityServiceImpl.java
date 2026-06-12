package com.bank.service.impl;

import com.bank.bean.User;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.AdminSecurityDao;
import com.bank.dao.UserDao;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.AdminSecurityDaoImpl;
import com.bank.dao.impl.UserDaoImpl;
import com.bank.dto.AdminRoleView;
import com.bank.dto.AdminUserDetail;
import com.bank.dto.AdminUserView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminSecurityService;
import com.bank.util.DBUtil;
import com.bank.util.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminSecurityServiceImpl implements AdminSecurityService {
    private static final int ADMIN_QUERY_LIMIT = 100;
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminSecurityDao adminSecurityDao = new AdminSecurityDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final UserDao userDao = new UserDaoImpl();

    @Override
    public ServiceResult<List<AdminUserView>> listAdminUsers(String keyword, String status) {
        String normalizedStatus = normalizeStatusForQuery(status);
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("管理员查询成功。",
                    adminSecurityDao.findAdminUsers(connection, trim(keyword), normalizedStatus, ADMIN_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("管理员查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<AdminRoleView>> listRoles() {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("角色权限查询成功。", adminSecurityDao.findRolesWithPermissions(connection));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("角色权限查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdminUserDetail> getAdminUserDetail(long userId) {
        try (Connection connection = DBUtil.getConnection()) {
            AdminUserView adminUser = adminSecurityDao.findAdminUserById(connection, userId);
            if (adminUser == null) {
                return ServiceResult.failure("管理员账号不存在。");
            }
            AdminUserDetail detail = new AdminUserDetail();
            detail.setAdminUser(adminUser);
            detail.setAvailableRoles(adminSecurityDao.findRolesWithPermissions(connection));
            return ServiceResult.success("管理员详情查询成功。", detail);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("管理员详情查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Long> createAdmin(long operatorUserId, String username, String phone, String password,
                                           String[] roleCodes, String ipAddress) {
        String normalizedUsername = trim(username);
        String normalizedPhone = trim(phone);
        String normalizedPassword = trim(password);
        List<String> normalizedRoles = normalizeRoleCodes(roleCodes);
        ServiceResult<Void> validation = validateCreateInput(normalizedUsername, normalizedPhone,
                normalizedPassword, normalizedRoles);
        if (!validation.isSuccess()) {
            return ServiceResult.failure(validation.getMessage());
        }

        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!validateRoles(connection, normalizedRoles).isSuccess()) {
                    connection.rollback();
                    return ServiceResult.failure("存在不可用的角色，请刷新页面后重试。");
                }
                if (normalizedRoles.contains(SUPER_ADMIN) && !operatorIsSuperAdmin(connection, operatorUserId)) {
                    connection.rollback();
                    return ServiceResult.failure("只有超级管理员可以授予超级管理员角色。");
                }
                if (userDao.existsByUsername(connection, normalizedUsername)) {
                    connection.rollback();
                    return ServiceResult.failure("管理员账号已存在。");
                }
                if (userDao.existsByPhone(connection, normalizedPhone)) {
                    connection.rollback();
                    return ServiceResult.failure("手机号已被其他用户使用。");
                }

                User user = new User();
                user.setUsername(normalizedUsername);
                user.setPhone(normalizedPhone);
                user.setPasswordHash(PasswordUtil.hash(normalizedPassword));
                user.setPayPasswordHash(null);
                user.setRole("ADMIN");
                user.setStatus("NORMAL");
                long userId = userDao.insert(connection, user);
                adminSecurityDao.replaceUserRoles(connection, userId, normalizedRoles);
                adminAuditLogDao.insert(connection, operatorUserId, "CREATE_ADMIN_USER", "ADMIN",
                        String.valueOf(userId), "创建管理员：" + normalizedUsername + "，角色：" + join(normalizedRoles),
                        ipAddress);
                connection.commit();
                return ServiceResult.success("管理员创建成功。", userId);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("管理员创建失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> updateAdmin(long operatorUserId, long targetUserId, String status, String[] roleCodes,
                                           String resetPassword, String ipAddress) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedPassword = trim(resetPassword);
        List<String> normalizedRoles = normalizeRoleCodes(roleCodes);
        if (normalizedStatus == null) {
            return ServiceResult.failure("管理员状态不合法。");
        }
        if (normalizedRoles.isEmpty()) {
            return ServiceResult.failure("管理员至少需要分配一个角色。");
        }
        if (normalizedPassword.length() > 0 && normalizedPassword.length() < 6) {
            return ServiceResult.failure("重置密码至少需要 6 位。");
        }

        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                AdminUserView target = adminSecurityDao.findAdminUserById(connection, targetUserId);
                if (target == null) {
                    connection.rollback();
                    return ServiceResult.failure("管理员账号不存在。");
                }
                if (!validateRoles(connection, normalizedRoles).isSuccess()) {
                    connection.rollback();
                    return ServiceResult.failure("存在不可用的角色，请刷新页面后重试。");
                }
                Set<String> currentRoles = adminSecurityDao.findUserRoleCodes(connection, targetUserId);
                Set<String> newRoleSet = new HashSet<String>(normalizedRoles);
                boolean operatorSuperAdmin = operatorIsSuperAdmin(connection, operatorUserId);
                if (currentRoles.contains(SUPER_ADMIN) && !operatorSuperAdmin) {
                    connection.rollback();
                    return ServiceResult.failure("只有超级管理员可以管理超级管理员账号。");
                }
                if (newRoleSet.contains(SUPER_ADMIN) && !operatorSuperAdmin) {
                    connection.rollback();
                    return ServiceResult.failure("只有超级管理员可以授予超级管理员角色。");
                }
                if (operatorUserId == targetUserId
                        && (!normalizedStatus.equals(target.getStatus()) || !currentRoles.equals(newRoleSet))) {
                    connection.rollback();
                    return ServiceResult.failure("不能修改自己的账号状态或角色。");
                }
                if (currentRoles.contains(SUPER_ADMIN)
                        && (!"NORMAL".equals(normalizedStatus) || !newRoleSet.contains(SUPER_ADMIN))
                        && adminSecurityDao.countActiveSuperAdmins(connection) <= 1) {
                    connection.rollback();
                    return ServiceResult.failure("系统必须至少保留一个启用状态的超级管理员。");
                }

                adminSecurityDao.updateAdminStatus(connection, targetUserId, normalizedStatus);
                adminSecurityDao.replaceUserRoles(connection, targetUserId, normalizedRoles);
                if (normalizedPassword.length() > 0) {
                    adminSecurityDao.updateAdminPassword(connection, targetUserId, PasswordUtil.hash(normalizedPassword));
                }
                String detail = "更新管理员：" + target.getUsername() + "，状态：" + normalizedStatus
                        + "，角色：" + join(normalizedRoles)
                        + (normalizedPassword.length() > 0 ? "，已重置密码" : "");
                adminAuditLogDao.insert(connection, operatorUserId, "UPDATE_ADMIN_USER", "ADMIN",
                        String.valueOf(targetUserId), detail, ipAddress);
                connection.commit();
                return ServiceResult.success("管理员更新成功。", null);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("管理员更新失败，请检查数据库状态或稍后重试。");
        }
    }

    private ServiceResult<Void> validateCreateInput(String username, String phone, String password,
                                                    List<String> roleCodes) {
        if (username.length() < 3 || username.length() > 50) {
            return ServiceResult.failure("管理员账号长度需要在 3 到 50 位之间。");
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            return ServiceResult.failure("管理员账号只能使用字母、数字和下划线。");
        }
        if (phone.length() < 6 || phone.length() > 20) {
            return ServiceResult.failure("手机号长度需要在 6 到 20 位之间。");
        }
        if (password.length() < 6) {
            return ServiceResult.failure("初始密码至少需要 6 位。");
        }
        if (roleCodes.isEmpty()) {
            return ServiceResult.failure("管理员至少需要分配一个角色。");
        }
        return ServiceResult.success("校验通过。", null);
    }

    private ServiceResult<Void> validateRoles(Connection connection, List<String> roleCodes) throws SQLException {
        Set<String> activeRoleCodes = adminSecurityDao.findActiveRoleCodes(connection, roleCodes);
        if (activeRoleCodes.size() != roleCodes.size()) {
            return ServiceResult.failure("存在不可用的角色。");
        }
        return ServiceResult.success("角色可用。", null);
    }

    private boolean operatorIsSuperAdmin(Connection connection, long operatorUserId) throws SQLException {
        return adminSecurityDao.findUserRoleCodes(connection, operatorUserId).contains(SUPER_ADMIN);
    }

    private List<String> normalizeRoleCodes(String[] roleCodes) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                String normalized = trim(roleCode);
                if (normalized.length() > 0) {
                    result.add(normalized);
                }
            }
        }
        return new ArrayList<String>(result);
    }

    private String normalizeStatus(String status) {
        String normalized = trim(status).toUpperCase();
        if ("NORMAL".equals(normalized) || "DISABLED".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeStatusForQuery(String status) {
        String normalized = normalizeStatus(status);
        return normalized == null ? "" : normalized;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
