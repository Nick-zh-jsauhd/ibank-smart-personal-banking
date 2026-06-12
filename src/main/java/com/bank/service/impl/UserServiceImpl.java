package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.bean.Customer;
import com.bank.bean.User;
import com.bank.dao.AccountDao;
import com.bank.dao.CustomerDao;
import com.bank.dao.LoginLogDao;
import com.bank.dao.UserDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dao.impl.LoginLogDaoImpl;
import com.bank.dao.impl.UserDaoImpl;
import com.bank.dto.RegisterRequest;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.NotificationService;
import com.bank.service.UserService;
import com.bank.util.AccountNoGenerator;
import com.bank.util.DBUtil;
import com.bank.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UserServiceImpl implements UserService {
    private static final int MAX_FAILED_LOGIN = 5;
    private static final long LOCK_MINUTES = 15L;

    private final UserDao userDao = new UserDaoImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final LoginLogDao loginLogDao = new LoginLogDaoImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();

    @Override
    public ServiceResult<Void> register(RegisterRequest request) {
        String validationMessage = validateRegisterRequest(request);
        if (validationMessage != null) {
            return ServiceResult.failure(validationMessage);
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            if (userDao.existsByUsername(connection, request.getUsername())) {
                connection.rollback();
                return ServiceResult.failure("用户名已存在，请更换后再试。");
            }
            if (userDao.existsByPhone(connection, request.getPhone())) {
                connection.rollback();
                return ServiceResult.failure("手机号已注册，请直接登录。");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setPhone(request.getPhone());
            user.setPasswordHash(PasswordUtil.hash(request.getPassword()));
            user.setRole("CUSTOMER");
            user.setStatus("NORMAL");
            long userId = userDao.insert(connection, user);

            Customer customer = new Customer();
            customer.setUserId(userId);
            customer.setFullName(request.getFullName());
            customer.setPhone(request.getPhone());
            customer.setEmail(emptyToNull(request.getEmail()));
            customer.setAddress(emptyToNull(request.getAddress()));
            customer.setRiskLevel("C2");
            long customerId = customerDao.insert(connection, customer);
            userDao.updateCustomerId(connection, userId, customerId);

            Account defaultAccount = new Account();
            defaultAccount.setCustomerId(customerId);
            defaultAccount.setAccountNo(generateUniqueAccountNo(connection));
            defaultAccount.setAccountType("SAVING");
            defaultAccount.setBranchName("iBank Online Branch");
            defaultAccount.setAvailableBalance(BigDecimal.ZERO.setScale(2));
            defaultAccount.setFrozenBalance(BigDecimal.ZERO.setScale(2));
            defaultAccount.setStatus("NORMAL");
            defaultAccount.setDefaultFlag(true);
            accountDao.insert(connection, defaultAccount);

            connection.commit();
            return ServiceResult.success("注册成功，请使用用户名或手机号登录。", null);
        } catch (Exception e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("注册失败，请检查数据库配置或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<SessionUser> login(String identity, String password, String ipAddress, String userAgent) {
        if (isBlank(identity) || isBlank(password)) {
            return ServiceResult.failure("请输入账号和密码。");
        }

        try (Connection connection = DBUtil.getConnection()) {
            User user = userDao.findByIdentity(connection, identity);
            if (user == null) {
                loginLogDao.insert(connection, null, identity, ipAddress, userAgent, false, "BAD_CREDENTIALS", 0);
                return ServiceResult.failure("账号或密码错误。");
            }

            if (!"NORMAL".equals(user.getStatus())) {
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "USER_NOT_NORMAL", 0);
                return ServiceResult.failure("账号状态不可用，请联系管理员。");
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (user.getLockedUntil() != null && user.getLockedUntil().after(now)) {
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "USER_LOCKED", 0);
                return ServiceResult.failure("连续登录失败次数过多，账号已临时锁定。");
            }

            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                int failedCount = user.getFailedLoginCount() + 1;
                Timestamp lockedUntil = null;
                if (failedCount >= MAX_FAILED_LOGIN) {
                    lockedUntil = new Timestamp(System.currentTimeMillis() + LOCK_MINUTES * 60L * 1000L);
                }
                userDao.recordLoginFailure(connection, user.getUserId(), failedCount, lockedUntil);
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "BAD_CREDENTIALS", 0);
                if (lockedUntil != null) {
                    return ServiceResult.failure("密码错误次数过多，账号已锁定 15 分钟。");
                }
                return ServiceResult.failure("账号或密码错误。");
            }

            userDao.recordLoginSuccess(connection, user.getUserId());
            loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, true, null, 0);

            Customer customer = user.getCustomerId() == null
                    ? customerDao.findByUserId(connection, user.getUserId())
                    : customerDao.findById(connection, user.getCustomerId());
            if (customer == null) {
                return ServiceResult.failure("账号缺少客户档案，请联系管理员。");
            }
            return ServiceResult.success("登录成功。", buildSessionUser(user, customer));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("登录失败，请检查数据库配置或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> setPayPassword(long userId, String loginPassword, String payPassword,
                                              String confirmPayPassword) {
        if (isBlank(loginPassword)) {
            return ServiceResult.failure("请输入登录密码。");
        }
        if (isBlank(payPassword) || !payPassword.matches("\\d{6}")) {
            return ServiceResult.failure("支付密码必须是 6 位数字。");
        }
        if (!payPassword.equals(confirmPayPassword)) {
            return ServiceResult.failure("两次输入的支付密码不一致。");
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            User user = userDao.findById(connection, userId);
            if (user == null) {
                connection.rollback();
                return ServiceResult.failure("用户不存在，请重新登录。");
            }
            if (!PasswordUtil.matches(loginPassword, user.getPasswordHash())) {
                connection.rollback();
                return ServiceResult.failure("登录密码不正确，不能设置支付密码。");
            }
            userDao.updatePayPassword(connection, userId, PasswordUtil.hash(payPassword));
            Long customerId = resolveCustomerId(connection, user);
            if (customerId != null) {
                notificationService.create(connection, customerId, userId, "SECURITY", "支付密码已更新",
                        "您的支付密码已设置或更新。如非本人操作，请立即修改登录密码并联系银行客服。",
                        "PAY_PASSWORD", String.valueOf(userId));
            }
            connection.commit();
            return ServiceResult.success("支付密码设置成功。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("支付密码设置失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    private String validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            return "注册信息不能为空。";
        }
        if (isBlank(request.getUsername()) || request.getUsername().length() < 3
                || request.getUsername().length() > 32) {
            return "用户名长度需为 3-32 个字符。";
        }
        if (!request.getUsername().matches("[A-Za-z0-9_]+")) {
            return "用户名只能包含字母、数字和下划线。";
        }
        if (isBlank(request.getPhone()) || !request.getPhone().matches("1\\d{10}")) {
            return "请输入 11 位手机号。";
        }
        if (isBlank(request.getFullName())) {
            return "请输入客户姓名。";
        }
        if (isBlank(request.getPassword()) || request.getPassword().length() < 6) {
            return "登录密码至少需要 6 位。";
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return "两次输入的密码不一致。";
        }
        return null;
    }

    private String generateUniqueAccountNo(Connection connection) throws SQLException {
        for (int i = 0; i < 10; i++) {
            String accountNo = AccountNoGenerator.generate();
            if (!accountDao.existsByAccountNo(connection, accountNo)) {
                return accountNo;
            }
        }
        throw new SQLException("Unable to generate unique account number");
    }

    private SessionUser buildSessionUser(User user, Customer customer) {
        SessionUser sessionUser = new SessionUser();
        sessionUser.setUserId(user.getUserId());
        sessionUser.setCustomerId(customer.getCustomerId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setFullName(customer.getFullName());
        sessionUser.setPhone(user.getPhone());
        sessionUser.setRole(user.getRole());
        return sessionUser;
    }

    private Long resolveCustomerId(Connection connection, User user) throws SQLException {
        if (user.getCustomerId() != null) {
            return user.getCustomerId();
        }
        Customer customer = customerDao.findByUserId(connection, user.getUserId());
        return customer == null ? null : customer.getCustomerId();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void restoreAutoCommit(Connection connection, boolean oldAutoCommit) {
        if (connection != null) {
            try {
                connection.setAutoCommit(oldAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
