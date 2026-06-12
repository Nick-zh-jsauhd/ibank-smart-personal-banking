package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.bean.Customer;
import com.bank.bean.RiskActionLog;
import com.bank.bean.RiskAssessment;
import com.bank.bean.RiskLimitRule;
import com.bank.bean.User;
import com.bank.bean.WealthProduct;
import com.bank.dao.AccountDao;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.AdminDao;
import com.bank.dao.CustomerDao;
import com.bank.dao.LedgerDao;
import com.bank.dao.LoginLogDao;
import com.bank.dao.RiskAssessmentDao;
import com.bank.dao.UserDao;
import com.bank.dao.WealthDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.AdminDaoImpl;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dao.impl.LedgerDaoImpl;
import com.bank.dao.impl.LoginLogDaoImpl;
import com.bank.dao.impl.RiskAssessmentDaoImpl;
import com.bank.dao.impl.UserDaoImpl;
import com.bank.dao.impl.WealthDaoImpl;
import com.bank.dto.AdminCustomerDetail;
import com.bank.dto.AdminCustomerView;
import com.bank.dto.AdminDashboardMetrics;
import com.bank.dto.AdminRiskEventDetail;
import com.bank.dto.AdminRiskEventView;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.RiskEventView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAlertService;
import com.bank.service.AdminService;
import com.bank.service.NotificationService;
import com.bank.service.PermissionService;
import com.bank.service.RiskService;
import com.bank.util.DBUtil;
import com.bank.util.MoneyUtil;
import com.bank.util.PasswordUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class AdminServiceImpl implements AdminService {
    private static final int MAX_FAILED_LOGIN = 5;
    private static final long LOCK_MINUTES = 15L;
    private static final int CUSTOMER_QUERY_LIMIT = 100;
    private static final int RISK_EVENT_QUERY_LIMIT = 100;
    private static final int RISK_RULE_QUERY_LIMIT = 100;
    private static final int WEALTH_PRODUCT_QUERY_LIMIT = 100;
    private static final BigDecimal MAX_EXPECTED_RATE = new BigDecimal("0.3000");

    private final UserDao userDao = new UserDaoImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final LedgerDao ledgerDao = new LedgerDaoImpl();
    private final LoginLogDao loginLogDao = new LoginLogDaoImpl();
    private final RiskAssessmentDao riskAssessmentDao = new RiskAssessmentDaoImpl();
    private final WealthDao wealthDao = new WealthDaoImpl();
    private final AdminDao adminDao = new AdminDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final RiskService riskService = new RiskServiceImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();
    private final PermissionService permissionService = new PermissionServiceImpl();

    @Override
    public ServiceResult<AdminSessionUser> login(String identity, String password, String ipAddress,
                                                 String userAgent) {
        if (isBlank(identity) || isBlank(password)) {
            return ServiceResult.failure("请输入管理员账号和密码。");
        }

        try (Connection connection = DBUtil.getConnection()) {
            User user = userDao.findByIdentity(connection, identity);
            if (user == null) {
                loginLogDao.insert(connection, null, identity, ipAddress, userAgent, false, "ADMIN_BAD_CREDENTIALS", 0);
                return ServiceResult.failure("管理员账号或密码错误。");
            }
            if (!"ADMIN".equals(user.getRole())) {
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "NOT_ADMIN", 0);
                return ServiceResult.failure("该账号不是管理员账号。");
            }
            if (!"NORMAL".equals(user.getStatus())) {
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "ADMIN_NOT_NORMAL", 0);
                return ServiceResult.failure("管理员账号状态不可用。");
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (user.getLockedUntil() != null && user.getLockedUntil().after(now)) {
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "ADMIN_LOCKED", 0);
                return ServiceResult.failure("连续登录失败次数过多，管理员账号已临时锁定。");
            }

            if (!PasswordUtil.matches(password, user.getPasswordHash())) {
                int failedCount = user.getFailedLoginCount() + 1;
                Timestamp lockedUntil = null;
                if (failedCount >= MAX_FAILED_LOGIN) {
                    lockedUntil = new Timestamp(System.currentTimeMillis() + LOCK_MINUTES * 60L * 1000L);
                }
                userDao.recordLoginFailure(connection, user.getUserId(), failedCount, lockedUntil);
                loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, false,
                        "ADMIN_BAD_CREDENTIALS", 0);
                if (lockedUntil != null) {
                    return ServiceResult.failure("密码错误次数过多，管理员账号已锁定 15 分钟。");
                }
                return ServiceResult.failure("管理员账号或密码错误。");
            }

            userDao.recordLoginSuccess(connection, user.getUserId());
            loginLogDao.insert(connection, user.getUserId(), identity, ipAddress, userAgent, true, null, 0);
            adminAuditLogDao.insert(connection, user.getUserId(), "ADMIN_LOGIN", "ADMIN",
                    String.valueOf(user.getUserId()), "管理员登录成功", ipAddress);
            return ServiceResult.success("管理员登录成功。", buildAdminSessionUser(user));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("管理员登录失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdminDashboardMetrics> dashboardMetrics() {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("后台指标查询成功。", adminDao.loadDashboardMetrics(connection));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("后台指标查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<AdminCustomerView>> listCustomers(String keyword, String riskLevel) {
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        String normalizedKeyword = isBlank(keyword) ? null : keyword.trim();
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("客户查询成功。",
                    adminDao.findCustomers(connection, normalizedKeyword, normalizedRiskLevel, CUSTOMER_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("客户查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdminCustomerDetail> getCustomerDetail(long customerId, long adminUserId, String ipAddress) {
        try (Connection connection = DBUtil.getConnection()) {
            Customer customer = customerDao.findById(connection, customerId);
            if (customer == null) {
                return ServiceResult.failure("客户不存在。");
            }
            AdminCustomerDetail detail = new AdminCustomerDetail();
            detail.setCustomer(customer);
            detail.setUser(userDao.findById(connection, customer.getUserId()));
            detail.setAccounts(accountDao.findByCustomerId(connection, customerId));
            detail.setRecentLedgers(ledgerDao.findByCustomer(connection, customerId, null, null, null, 20));

            ServiceResult<List<RiskEventView>> riskResult = riskService.listEvents(customerId, null);
            detail.setRiskEvents(riskResult.isSuccess() ? riskResult.getData() : Collections.<RiskEventView>emptyList());

            ServiceResult<List<com.bank.bean.Notification>> notificationResult =
                    notificationService.listNotifications(customerId);
            detail.setNotifications(notificationResult.isSuccess()
                    ? notificationResult.getData() : Collections.<com.bank.bean.Notification>emptyList());
            detail.setRiskAssessments(riskAssessmentDao.findByCustomer(connection, customerId, 10));
            detail.setWealthOrderConfirms(wealthDao.findOrderConfirmsByCustomer(connection, customerId, 10));

            adminAuditLogDao.insert(connection, adminUserId, "VIEW_CUSTOMER_DETAIL", "CUSTOMER",
                    String.valueOf(customerId), "查看客户详情：" + customer.getFullName(), ipAddress);
            return ServiceResult.success("客户详情查询成功。", detail);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("客户详情查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> adjustCustomerRiskLevel(long adminUserId, long customerId, String riskLevel,
                                                       String reason, String ipAddress) {
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        String normalizedReason;
        try {
            normalizedReason = normalizeRequiredReason(reason);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (normalizedRiskLevel == null) {
            return ServiceResult.failure("请选择正确的客户风险等级。");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            Customer customer = customerDao.findById(connection, customerId);
            if (customer == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("客户不存在。");
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            String oldRiskLevel = customer.getRiskLevel();
            customerDao.updateRiskProfile(connection, customerId, normalizedRiskLevel, "ADMIN", now);
            adminAuditLogDao.insert(connection, adminUserId, "ADJUST_CUSTOMER_RISK_LEVEL", "CUSTOMER",
                    String.valueOf(customerId), "调整客户风险等级 " + oldRiskLevel + " -> "
                            + normalizedRiskLevel + "，原因：" + normalizedReason, ipAddress);
            notificationService.create(connection, customerId, customer.getUserId(), "RISK", "风险等级已调整",
                    "您的风险承受能力等级已由管理员调整为 " + normalizedRiskLevel + "。如有疑问请联系客户服务。",
                    "CUSTOMER_RISK_LEVEL", String.valueOf(customerId));

            connection.commit();
            return ServiceResult.success("客户风险等级调整成功。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("客户风险等级调整失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<AdminRiskEventView>> listRiskEvents(String decision, String handleStatus) {
        String normalizedDecision = normalizeDecision(decision);
        String normalizedHandleStatus = normalizeHandleStatus(handleStatus);
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("风险事件查询成功。",
                    adminDao.findRiskEvents(connection, normalizedDecision, normalizedHandleStatus,
                            RISK_EVENT_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风险事件查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdminRiskEventDetail> getRiskEventDetail(long eventId, long adminUserId,
                                                                  String ipAddress) {
        if (eventId <= 0) {
            return ServiceResult.failure("风险事件不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            AdminRiskEventView event = adminDao.findRiskEventById(connection, eventId);
            if (event == null) {
                return ServiceResult.failure("风险事件不存在。");
            }
            Customer customer = customerDao.findById(connection, event.getCustomerId());
            Account account = event.getAccountId() == null ? null : accountDao.findById(connection,
                    event.getAccountId());
            List<LedgerEntryView> recentLedgers = ledgerDao.findByCustomer(connection, event.getCustomerId(),
                    event.getAccountId(), null, null, 20);

            AdminRiskEventDetail detail = new AdminRiskEventDetail();
            detail.setEvent(event);
            detail.setCustomer(customer);
            detail.setAccount(account);
            detail.setRecentLedgers(recentLedgers);
            detail.setActionLogs(adminDao.findRiskActionLogs(connection, eventId));

            adminAuditLogDao.insert(connection, adminUserId, "VIEW_RISK_EVENT_DETAIL", "RISK_EVENT",
                    String.valueOf(eventId), "查看风险事件详情：" + event.getTransactionNo(), ipAddress);
            return ServiceResult.success("风险事件详情查询成功。", detail);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风险事件详情查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> handleRiskEvent(long adminUserId, long eventId, String handleResult,
                                               String accountAction, String note, String ipAddress) {
        String normalizedHandleResult = normalizeHandleResult(handleResult);
        String normalizedAccountAction = normalizeAccountAction(accountAction);
        String normalizedNote;
        try {
            normalizedNote = normalizeHandleNote(note);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (eventId <= 0) {
            return ServiceResult.failure("风险事件不存在。");
        }
        if (normalizedHandleResult == null) {
            return ServiceResult.failure("请选择正确的处置结论。");
        }
        if (normalizedAccountAction == null) {
            return ServiceResult.failure("请选择正确的账户动作。");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            AdminRiskEventView event = adminDao.findRiskEventByIdForUpdate(connection, eventId);
            if (event == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("风险事件不存在。");
            }

            String oldHandleStatus = event.getHandleStatus();
            String newHandleStatus = "FOLLOW_UP".equals(normalizedHandleResult) ? "FOLLOW_UP" : "HANDLED";
            adminDao.updateRiskEventHandling(connection, eventId, newHandleStatus, normalizedHandleResult,
                    adminUserId, normalizedNote);
            insertRiskActionLog(connection, eventId, adminUserId, "HANDLE_EVENT", oldHandleStatus,
                    newHandleStatus, resultLabel(normalizedHandleResult) + "：" + normalizedNote);

            Account account = null;
            if (!"NONE".equals(normalizedAccountAction)) {
                if (event.getAccountId() == null) {
                    rollbackQuietly(connection);
                    return ServiceResult.failure("该风险事件没有关联账户，不能执行账户控制。");
                }
                account = accountDao.findByIdForUpdate(connection, event.getAccountId());
                if (account == null) {
                    rollbackQuietly(connection);
                    return ServiceResult.failure("风险事件关联账户不存在。");
                }
                ServiceResult<Void> accountResult = applyAccountAction(connection, event, account,
                        adminUserId, normalizedAccountAction, normalizedNote);
                if (!accountResult.isSuccess()) {
                    rollbackQuietly(connection);
                    return accountResult;
                }
            } else if (event.getAccountId() != null) {
                account = accountDao.findById(connection, event.getAccountId());
            }

            Customer customer = customerDao.findById(connection, event.getCustomerId());
            Long userId = customer == null ? null : customer.getUserId();
            String accountNo = account == null ? event.getAccountNo() : account.getAccountNo();
            notificationService.create(connection, event.getCustomerId(), userId, "RISK",
                    notificationTitle(normalizedHandleResult, normalizedAccountAction),
                    notificationContent(event, accountNo, normalizedHandleResult, normalizedAccountAction),
                    "RISK_EVENT", String.valueOf(eventId));

            adminAuditLogDao.insert(connection, adminUserId, "HANDLE_RISK_EVENT", "RISK_EVENT",
                    String.valueOf(eventId), "处置风险事件 " + event.getTransactionNo()
                            + "，结论：" + resultLabel(normalizedHandleResult)
                            + "，账户动作：" + accountActionLabel(normalizedAccountAction),
                    ipAddress);
            syncRiskAlert(connection, event, adminUserId, newHandleStatus);
            connection.commit();
            return ServiceResult.success("风险事件处置成功。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("风险事件处置失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<RiskLimitRule>> listRiskRules(String txnType, String riskLevel) {
        String normalizedTxnType = normalizeTxnType(txnType);
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("风控规则查询成功。",
                    adminDao.findRiskRules(connection, normalizedTxnType, normalizedRiskLevel, RISK_RULE_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风控规则查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> updateRiskRule(long adminUserId, long ruleId, String singleLimit,
                                              String dailyAmountLimit, String dailyCountLimit,
                                              String status, String ipAddress) {
        if (ruleId <= 0) {
            return ServiceResult.failure("风控规则不存在。");
        }

        BigDecimal normalizedSingleLimit;
        BigDecimal normalizedDailyAmountLimit;
        int normalizedDailyCountLimit;
        String normalizedStatus = normalizeRuleStatus(status);
        try {
            normalizedSingleLimit = MoneyUtil.parseAmount(singleLimit);
            normalizedDailyAmountLimit = MoneyUtil.parseAmount(dailyAmountLimit);
            normalizedDailyCountLimit = parsePositiveInt(dailyCountLimit, "日笔数上限");
            validateRiskRuleInput(normalizedSingleLimit, normalizedDailyAmountLimit, normalizedStatus);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            RiskLimitRule rule = adminDao.findRiskRuleByIdForUpdate(connection, ruleId);
            if (rule == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("风控规则不存在。");
            }

            String detail = "更新风控规则 " + rule.getRuleCode()
                    + "，单笔 " + moneyText(rule.getSingleLimit()) + " -> " + moneyText(normalizedSingleLimit)
                    + "，日累计 " + moneyText(rule.getDailyAmountLimit()) + " -> "
                    + moneyText(normalizedDailyAmountLimit)
                    + "，日笔数 " + rule.getDailyCountLimit() + " -> " + normalizedDailyCountLimit
                    + "，状态 " + rule.getStatus() + " -> " + normalizedStatus;

            rule.setSingleLimit(normalizedSingleLimit);
            rule.setDailyAmountLimit(normalizedDailyAmountLimit);
            rule.setDailyCountLimit(normalizedDailyCountLimit);
            rule.setStatus(normalizedStatus);
            adminDao.updateRiskRule(connection, rule);
            adminAuditLogDao.insert(connection, adminUserId, "UPDATE_RISK_RULE", "RISK_RULE",
                    String.valueOf(ruleId), detail, ipAddress);
            connection.commit();
            return ServiceResult.success("风控规则更新成功。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("风控规则更新失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<WealthProduct>> listWealthProducts(String status) {
        String normalizedStatus = normalizeProductStatus(status);
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("理财产品查询成功。",
                    adminDao.findWealthProducts(connection, normalizedStatus, WEALTH_PRODUCT_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("理财产品查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> updateWealthProduct(long adminUserId, long productId, String productName,
                                                   String riskLevel, String expectedRate, String periodDays,
                                                   String minAmount, String maxAmount, String status,
                                                   String description, String ipAddress) {
        if (productId <= 0) {
            return ServiceResult.failure("理财产品不存在。");
        }

        String normalizedName;
        String normalizedRiskLevel = normalizeProductRiskLevel(riskLevel);
        BigDecimal normalizedExpectedRate;
        int normalizedPeriodDays;
        BigDecimal normalizedMinAmount;
        BigDecimal normalizedMaxAmount;
        String normalizedStatus = normalizeProductStatus(status);
        String normalizedDescription;
        try {
            normalizedName = normalizeProductName(productName);
            normalizedDescription = normalizeDescription(description);
            normalizedExpectedRate = parseExpectedRate(expectedRate);
            normalizedPeriodDays = parsePositiveInt(periodDays, "产品期限");
            normalizedMinAmount = MoneyUtil.parseAmount(minAmount);
            normalizedMaxAmount = MoneyUtil.parseAmount(maxAmount);
            validateWealthProductInput(normalizedRiskLevel, normalizedMinAmount, normalizedMaxAmount,
                    normalizedStatus);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            WealthProduct product = adminDao.findWealthProductByIdForUpdate(connection, productId);
            if (product == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("理财产品不存在。");
            }

            String detail = "更新理财产品 " + product.getProductCode()
                    + "，名称 " + product.getProductName() + " -> " + normalizedName
                    + "，风险 " + product.getRiskLevel() + " -> " + normalizedRiskLevel
                    + "，状态 " + product.getStatus() + " -> " + normalizedStatus;

            product.setProductName(normalizedName);
            product.setRiskLevel(normalizedRiskLevel);
            product.setExpectedRate(normalizedExpectedRate);
            product.setPeriodDays(normalizedPeriodDays);
            product.setMinAmount(normalizedMinAmount);
            product.setMaxAmount(normalizedMaxAmount);
            product.setStatus(normalizedStatus);
            product.setDescription(normalizedDescription);
            adminDao.updateWealthProduct(connection, product);
            adminAuditLogDao.insert(connection, adminUserId, "UPDATE_WEALTH_PRODUCT", "WEALTH_PRODUCT",
                    String.valueOf(productId), detail, ipAddress);
            connection.commit();
            return ServiceResult.success("理财产品更新成功。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("理财产品更新失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public void audit(long adminUserId, String operationType, String targetType, String targetId,
                      String detail, String ipAddress) {
        try (Connection connection = DBUtil.getConnection()) {
            adminAuditLogDao.insert(connection, adminUserId, operationType, targetType, targetId, detail, ipAddress);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private AdminSessionUser buildAdminSessionUser(User user) {
        AdminSessionUser sessionUser = new AdminSessionUser();
        sessionUser.setUserId(user.getUserId());
        sessionUser.setUsername(user.getUsername());
        sessionUser.setRole(user.getRole());
        sessionUser.setRoleCodes(permissionService.rolesFor(user.getUserId()));
        sessionUser.setPermissionCodes(permissionService.permissionsFor(user.getUserId()));
        return sessionUser;
    }

    private String normalizeRiskLevel(String riskLevel) {
        if ("C1".equals(riskLevel) || "C2".equals(riskLevel) || "C3".equals(riskLevel)
                || "C4".equals(riskLevel) || "C5".equals(riskLevel)) {
            return riskLevel;
        }
        return null;
    }

    private String normalizeDecision(String decision) {
        if ("WARN".equals(decision) || "BLOCK".equals(decision)) {
            return decision;
        }
        return null;
    }

    private String normalizeHandleStatus(String handleStatus) {
        if ("PENDING".equals(handleStatus) || "FOLLOW_UP".equals(handleStatus)
                || "HANDLED".equals(handleStatus)) {
            return handleStatus;
        }
        return null;
    }

    private String normalizeHandleResult(String handleResult) {
        if ("FALSE_POSITIVE".equals(handleResult) || "CONFIRMED_RISK".equals(handleResult)
                || "CUSTOMER_VERIFIED".equals(handleResult) || "FOLLOW_UP".equals(handleResult)) {
            return handleResult;
        }
        return null;
    }

    private String normalizeAccountAction(String accountAction) {
        if ("NONE".equals(accountAction) || "FREEZE".equals(accountAction) || "UNFREEZE".equals(accountAction)) {
            return accountAction;
        }
        return null;
    }

    private String normalizeHandleNote(String note) {
        if (note == null) {
            return "";
        }
        String normalized = note.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("处置备注不能超过 500 个字符。");
        }
        return normalized;
    }

    private String normalizeRequiredReason(String reason) {
        if (isBlank(reason)) {
            throw new IllegalArgumentException("请输入调整原因。");
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("调整原因不能超过 500 个字符。");
        }
        return normalized;
    }

    private ServiceResult<Void> applyAccountAction(Connection connection, AdminRiskEventView event, Account account,
                                                   long adminUserId, String accountAction, String note)
            throws SQLException {
        String beforeStatus = account.getStatus();
        if ("FREEZE".equals(accountAction)) {
            if ("FROZEN".equals(beforeStatus)) {
                return ServiceResult.failure("账户已经处于冻结状态。");
            }
            if (!"NORMAL".equals(beforeStatus)) {
                return ServiceResult.failure("账户当前状态不允许冻结：" + beforeStatus);
            }
            accountDao.updateStatus(connection, account.getAccountId(), "FROZEN");
            insertRiskActionLog(connection, event.getEventId(), adminUserId, "FREEZE_ACCOUNT",
                    beforeStatus, "FROZEN", "冻结账户 " + account.getAccountNo() + "：" + note);
            return ServiceResult.success("账户已冻结。", null);
        }
        if ("UNFREEZE".equals(accountAction)) {
            if (!"FROZEN".equals(beforeStatus)) {
                return ServiceResult.failure("只有冻结账户可以解冻。");
            }
            accountDao.updateStatus(connection, account.getAccountId(), "NORMAL");
            insertRiskActionLog(connection, event.getEventId(), adminUserId, "UNFREEZE_ACCOUNT",
                    beforeStatus, "NORMAL", "解冻账户 " + account.getAccountNo() + "：" + note);
            return ServiceResult.success("账户已解冻。", null);
        }
        return ServiceResult.success("无需账户动作。", null);
    }

    private void insertRiskActionLog(Connection connection, long eventId, long adminUserId, String actionType,
                                     String beforeStatus, String afterStatus, String note) throws SQLException {
        RiskActionLog log = new RiskActionLog();
        log.setEventId(eventId);
        log.setAdminUserId(adminUserId);
        log.setActionType(actionType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setNote(trimToLength(note, 500));
        adminDao.insertRiskActionLog(connection, log);
    }

    private void syncRiskAlert(Connection connection, AdminRiskEventView event, long adminUserId,
                               String newHandleStatus) throws SQLException {
        if ("HANDLED".equals(newHandleStatus)) {
            adminAlertService.resolveByTarget(connection, "RISK_BLOCK", event.getTxnType(),
                    event.getTransactionNo(), adminUserId,
                    "风险事件 " + event.getEventId() + " 已处置完成");
            adminAlertService.resolveByTarget(connection, "RISK_WARN", event.getTxnType(),
                    event.getTransactionNo(), adminUserId,
                    "风险事件 " + event.getEventId() + " 已处置完成");
        } else if ("FOLLOW_UP".equals(newHandleStatus)) {
            adminAlertService.ackByTarget(connection, "RISK_BLOCK", event.getTxnType(),
                    event.getTransactionNo(), adminUserId,
                    "风险事件 " + event.getEventId() + " 已进入继续跟进");
            adminAlertService.ackByTarget(connection, "RISK_WARN", event.getTxnType(),
                    event.getTransactionNo(), adminUserId,
                    "风险事件 " + event.getEventId() + " 已进入继续跟进");
        }
    }

    private String notificationTitle(String handleResult, String accountAction) {
        if ("FREEZE".equals(accountAction)) {
            return "账户已冻结";
        }
        if ("UNFREEZE".equals(accountAction)) {
            return "账户已解冻";
        }
        if ("FOLLOW_UP".equals(handleResult)) {
            return "风险事件待进一步核实";
        }
        return "风险事件已处理";
    }

    private String notificationContent(AdminRiskEventView event, String accountNo, String handleResult,
                                       String accountAction) {
        StringBuilder builder = new StringBuilder();
        builder.append("交易 ").append(event.getTransactionNo()).append(" 的风险事件已处理，结论：")
                .append(resultLabel(handleResult)).append("。");
        if (accountNo != null && accountNo.length() > 0) {
            builder.append("关联账户：").append(accountNo).append("。");
        }
        if ("FREEZE".equals(accountAction)) {
            builder.append("为保障资金安全，该账户已临时冻结。");
        } else if ("UNFREEZE".equals(accountAction)) {
            builder.append("该账户已恢复正常使用。");
        }
        return trimToLength(builder.toString(), 500);
    }

    private String resultLabel(String handleResult) {
        if ("FALSE_POSITIVE".equals(handleResult)) {
            return "误报";
        }
        if ("CONFIRMED_RISK".equals(handleResult)) {
            return "确认风险";
        }
        if ("CUSTOMER_VERIFIED".equals(handleResult)) {
            return "客户已核实";
        }
        if ("FOLLOW_UP".equals(handleResult)) {
            return "继续跟进";
        }
        return handleResult == null ? "" : handleResult;
    }

    private String accountActionLabel(String accountAction) {
        if ("FREEZE".equals(accountAction)) {
            return "冻结账户";
        }
        if ("UNFREEZE".equals(accountAction)) {
            return "解冻账户";
        }
        return "无";
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String normalizeTxnType(String txnType) {
        if ("WITHDRAW".equals(txnType) || "TRANSFER_INNER".equals(txnType) || "PAYMENT".equals(txnType)
                || "BUY_WEALTH".equals(txnType)) {
            return txnType;
        }
        return null;
    }

    private String normalizeRuleStatus(String status) {
        if ("ACTIVE".equals(status) || "DISABLED".equals(status)) {
            return status;
        }
        return null;
    }

    private String normalizeProductStatus(String status) {
        if ("ON_SALE".equals(status) || "OFF_SALE".equals(status)) {
            return status;
        }
        return null;
    }

    private String normalizeProductRiskLevel(String riskLevel) {
        if ("R1".equals(riskLevel) || "R2".equals(riskLevel) || "R3".equals(riskLevel)
                || "R4".equals(riskLevel) || "R5".equals(riskLevel)) {
            return riskLevel;
        }
        return null;
    }

    private String normalizeProductName(String productName) {
        if (isBlank(productName)) {
            throw new IllegalArgumentException("请输入产品名称。");
        }
        String normalized = productName.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("产品名称不能超过 120 个字符。");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String normalized = description.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("产品说明不能超过 500 个字符。");
        }
        return normalized;
    }

    private int parsePositiveInt(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("请输入" + fieldName + "。");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(fieldName + "必须大于 0。");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式不正确。");
        }
    }

    private BigDecimal parseExpectedRate(String expectedRate) {
        if (isBlank(expectedRate)) {
            throw new IllegalArgumentException("请输入预期年化收益率。");
        }
        try {
            BigDecimal parsed = new BigDecimal(expectedRate.trim()).setScale(4, RoundingMode.HALF_UP);
            if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(MAX_EXPECTED_RATE) >= 0) {
                throw new IllegalArgumentException("预期年化收益率需在 0 到 0.3000 之间。");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("预期年化收益率格式不正确。");
        }
    }

    private void validateRiskRuleInput(BigDecimal singleLimit, BigDecimal dailyAmountLimit, String status) {
        if (!MoneyUtil.isPositive(singleLimit)) {
            throw new IllegalArgumentException("单笔限额必须大于 0。");
        }
        if (!MoneyUtil.isPositive(dailyAmountLimit)) {
            throw new IllegalArgumentException("日累计限额必须大于 0。");
        }
        if (dailyAmountLimit.compareTo(singleLimit) < 0) {
            throw new IllegalArgumentException("日累计限额不能小于单笔限额。");
        }
        if (status == null) {
            throw new IllegalArgumentException("规则状态不正确。");
        }
    }

    private void validateWealthProductInput(String riskLevel, BigDecimal minAmount, BigDecimal maxAmount,
                                            String status) {
        if (riskLevel == null) {
            throw new IllegalArgumentException("产品风险等级不正确。");
        }
        if (!MoneyUtil.isPositive(minAmount)) {
            throw new IllegalArgumentException("起购金额必须大于 0。");
        }
        if (!MoneyUtil.isPositive(maxAmount)) {
            throw new IllegalArgumentException("单人上限必须大于 0。");
        }
        if (maxAmount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("单人上限不能小于起购金额。");
        }
        if (status == null) {
            throw new IllegalArgumentException("产品状态不正确。");
        }
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // ignore rollback failure
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignore close failure
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
