package com.bank.dao.impl;

import com.bank.bean.RiskLimitRule;
import com.bank.bean.RiskActionLog;
import com.bank.bean.WealthProduct;
import com.bank.dao.AdminDao;
import com.bank.dto.AdminCustomerView;
import com.bank.dto.AdminDashboardMetrics;
import com.bank.dto.AdminRiskEventView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDaoImpl implements AdminDao {
    @Override
    public AdminDashboardMetrics loadDashboardMetrics(Connection connection) throws SQLException {
        AdminDashboardMetrics metrics = new AdminDashboardMetrics();
        metrics.setCustomerCount(count(connection, "SELECT COUNT(*) FROM t_customer"));
        metrics.setAccountCount(count(connection, "SELECT COUNT(*) FROM t_account"));
        metrics.setTodayTransactionCount(count(connection,
                "SELECT COUNT(*) FROM t_transaction WHERE DATE(created_at) = CURDATE()"));
        metrics.setTodayTransactionAmount(sum(connection,
                "SELECT COALESCE(SUM(amount), 0.00) FROM t_transaction WHERE DATE(created_at) = CURDATE() "
                        + "AND status = 'SUCCESS'"));
        metrics.setTodayRiskBlockCount(count(connection,
                "SELECT COUNT(*) FROM t_risk_event WHERE DATE(created_at) = CURDATE() AND decision = 'BLOCK'"));
        metrics.setUnreadNotificationCount(count(connection,
                "SELECT COUNT(*) FROM t_notification WHERE read_flag = 0"));
        metrics.setOpenAdminAlertCount(count(connection,
                "SELECT COUNT(*) FROM t_admin_alert WHERE status IN ('NEW', 'ACKED')"));
        metrics.setOpenServiceTicketCount(count(connection,
                "SELECT COUNT(*) FROM t_service_ticket "
                        + "WHERE status IN ('SUBMITTED', 'REOPENED', 'ACCEPTED', 'INVESTIGATING', 'WAITING_CUSTOMER')"));
        metrics.setOpenAdjustmentReviewCount(count(connection,
                "SELECT COUNT(*) FROM t_adjustment_request WHERE status IN ('PENDING_REVIEW', 'APPROVED')"));
        metrics.setOpenReconciliationItemCount(count(connection,
                "SELECT COUNT(*) FROM t_reconciliation_item WHERE status IN ('OPEN', 'INVESTIGATING', 'CONFIRMED_EXCEPTION')"));
        metrics.setOpenRiskGraphReviewCaseCount(count(connection,
                "SELECT COUNT(*) FROM t_risk_graph_review_case WHERE case_status = 'OPEN'"));
        metrics.setWealthHoldingPrincipal(sum(connection,
                "SELECT COALESCE(SUM(principal), 0.00) FROM t_wealth_holding WHERE status = 'HOLDING'"));
        return metrics;
    }

    @Override
    public List<AdminCustomerView> findCustomers(Connection connection, String keyword, String riskLevel, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.customer_id, u.username, c.full_name, c.phone, c.risk_level, c.created_at, ");
        sql.append("COUNT(a.account_id) AS account_count, COALESCE(SUM(a.available_balance), 0.00) AS total_balance ");
        sql.append("FROM t_customer c JOIN t_user u ON c.user_id = u.user_id ");
        sql.append("LEFT JOIN t_account a ON c.customer_id = a.customer_id WHERE 1 = 1 ");
        if (keyword != null && keyword.length() > 0) {
            sql.append("AND (c.full_name LIKE ? OR c.phone LIKE ? OR u.username LIKE ?) ");
        }
        if (riskLevel != null && riskLevel.length() > 0) {
            sql.append("AND c.risk_level = ? ");
        }
        sql.append("GROUP BY c.customer_id, u.username, c.full_name, c.phone, c.risk_level, c.created_at ");
        sql.append("ORDER BY c.created_at DESC, c.customer_id DESC LIMIT ?");

        List<AdminCustomerView> customers = new ArrayList<AdminCustomerView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (keyword != null && keyword.length() > 0) {
                String like = "%" + keyword + "%";
                statement.setString(index++, like);
                statement.setString(index++, like);
                statement.setString(index++, like);
            }
            if (riskLevel != null && riskLevel.length() > 0) {
                statement.setString(index++, riskLevel);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(mapCustomerView(resultSet));
                }
            }
        }
        return customers;
    }

    @Override
    public List<AdminRiskEventView> findRiskEvents(Connection connection, String decision, String handleStatus,
                                                   int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.event_id, e.customer_id, e.account_id, c.full_name, c.phone, ");
        sql.append("a.account_no, a.status AS account_status, e.transaction_no, ");
        sql.append("e.txn_type, e.amount, e.risk_score, e.risk_level, e.decision, e.hit_rules, ");
        sql.append("e.reason, e.ip_address, e.handle_status, e.handle_result, e.handler_admin_user_id, ");
        sql.append("u.username AS handler_username, e.handle_note, e.handled_at, e.created_at ");
        sql.append("FROM t_risk_event e JOIN t_customer c ON e.customer_id = c.customer_id ");
        sql.append("LEFT JOIN t_account a ON e.account_id = a.account_id ");
        sql.append("LEFT JOIN t_user u ON e.handler_admin_user_id = u.user_id WHERE 1 = 1 ");
        if (decision != null && decision.length() > 0) {
            sql.append("AND e.decision = ? ");
        }
        if (handleStatus != null && handleStatus.length() > 0) {
            sql.append("AND e.handle_status = ? ");
        }
        sql.append("ORDER BY e.created_at DESC, e.event_id DESC LIMIT ?");

        List<AdminRiskEventView> events = new ArrayList<AdminRiskEventView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (decision != null && decision.length() > 0) {
                statement.setString(index++, decision);
            }
            if (handleStatus != null && handleStatus.length() > 0) {
                statement.setString(index++, handleStatus);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapRiskEventView(resultSet));
                }
            }
        }
        return events;
    }

    @Override
    public AdminRiskEventView findRiskEventById(Connection connection, long eventId) throws SQLException {
        String sql = riskEventSelectSql() + " WHERE e.event_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRiskEventView(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public AdminRiskEventView findRiskEventByIdForUpdate(Connection connection, long eventId) throws SQLException {
        String sql = riskEventSelectSql() + " WHERE e.event_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRiskEventView(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateRiskEventHandling(Connection connection, long eventId, String handleStatus, String handleResult,
                                        long handlerAdminUserId, String handleNote) throws SQLException {
        String sql = "UPDATE t_risk_event SET handle_status = ?, handle_result = ?, handler_admin_user_id = ?, "
                + "handle_note = ?, handled_at = NOW() WHERE event_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, handleStatus);
            statement.setString(2, handleResult);
            statement.setLong(3, handlerAdminUserId);
            statement.setString(4, handleNote);
            statement.setLong(5, eventId);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertRiskActionLog(Connection connection, RiskActionLog actionLog) throws SQLException {
        String sql = "INSERT INTO t_risk_action_log (event_id, admin_user_id, action_type, before_status, "
                + "after_status, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actionLog.getEventId());
            statement.setLong(2, actionLog.getAdminUserId());
            statement.setString(3, actionLog.getActionType());
            statement.setString(4, actionLog.getBeforeStatus());
            statement.setString(5, actionLog.getAfterStatus());
            statement.setString(6, actionLog.getNote());
            statement.executeUpdate();
        }
    }

    @Override
    public List<RiskActionLog> findRiskActionLogs(Connection connection, long eventId) throws SQLException {
        String sql = "SELECT l.action_id, l.event_id, l.admin_user_id, u.username AS admin_username, "
                + "l.action_type, l.before_status, l.after_status, l.note, l.created_at "
                + "FROM t_risk_action_log l JOIN t_user u ON l.admin_user_id = u.user_id "
                + "WHERE l.event_id = ? ORDER BY l.created_at DESC, l.action_id DESC";
        List<RiskActionLog> logs = new ArrayList<RiskActionLog>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapRiskActionLog(resultSet));
                }
            }
        }
        return logs;
    }

    @Override
    public List<RiskLimitRule> findRiskRules(Connection connection, String txnType, String riskLevel, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT rule_id, rule_code, txn_type, customer_risk_level, single_limit, ");
        sql.append("daily_amount_limit, daily_count_limit, status, created_at, updated_at ");
        sql.append("FROM t_risk_limit_rule WHERE 1 = 1 ");
        if (txnType != null && txnType.length() > 0) {
            sql.append("AND txn_type = ? ");
        }
        if (riskLevel != null && riskLevel.length() > 0) {
            sql.append("AND customer_risk_level = ? ");
        }
        sql.append("ORDER BY txn_type ASC, customer_risk_level ASC LIMIT ?");

        List<RiskLimitRule> rules = new ArrayList<RiskLimitRule>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (txnType != null && txnType.length() > 0) {
                statement.setString(index++, txnType);
            }
            if (riskLevel != null && riskLevel.length() > 0) {
                statement.setString(index++, riskLevel);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rules.add(mapRiskLimitRule(resultSet));
                }
            }
        }
        return rules;
    }

    @Override
    public RiskLimitRule findRiskRuleByIdForUpdate(Connection connection, long ruleId) throws SQLException {
        String sql = "SELECT rule_id, rule_code, txn_type, customer_risk_level, single_limit, "
                + "daily_amount_limit, daily_count_limit, status, created_at, updated_at "
                + "FROM t_risk_limit_rule WHERE rule_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ruleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRiskLimitRule(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateRiskRule(Connection connection, RiskLimitRule rule) throws SQLException {
        String sql = "UPDATE t_risk_limit_rule SET single_limit = ?, daily_amount_limit = ?, "
                + "daily_count_limit = ?, status = ?, updated_at = NOW() WHERE rule_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, rule.getSingleLimit());
            statement.setBigDecimal(2, rule.getDailyAmountLimit());
            statement.setInt(3, rule.getDailyCountLimit());
            statement.setString(4, rule.getStatus());
            statement.setLong(5, rule.getRuleId());
            statement.executeUpdate();
        }
    }

    @Override
    public List<WealthProduct> findWealthProducts(Connection connection, String status, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT product_id, product_code, product_name, risk_level, expected_rate, period_days, ");
        sql.append("min_amount, max_amount, status, description, created_at, updated_at ");
        sql.append("FROM t_wealth_product WHERE 1 = 1 ");
        if (status != null && status.length() > 0) {
            sql.append("AND status = ? ");
        }
        sql.append("ORDER BY FIELD(status, 'ON_SALE', 'OFF_SALE'), risk_level ASC, expected_rate ASC LIMIT ?");

        List<WealthProduct> products = new ArrayList<WealthProduct>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapWealthProduct(resultSet));
                }
            }
        }
        return products;
    }

    @Override
    public WealthProduct findWealthProductByIdForUpdate(Connection connection, long productId) throws SQLException {
        String sql = "SELECT product_id, product_code, product_name, risk_level, expected_rate, period_days, "
                + "min_amount, max_amount, status, description, created_at, updated_at "
                + "FROM t_wealth_product WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapWealthProduct(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateWealthProduct(Connection connection, WealthProduct product) throws SQLException {
        String sql = "UPDATE t_wealth_product SET product_name = ?, risk_level = ?, expected_rate = ?, "
                + "period_days = ?, min_amount = ?, max_amount = ?, status = ?, description = ?, "
                + "updated_at = NOW() WHERE product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getProductName());
            statement.setString(2, product.getRiskLevel());
            statement.setBigDecimal(3, product.getExpectedRate());
            statement.setInt(4, product.getPeriodDays());
            statement.setBigDecimal(5, product.getMinAmount());
            statement.setBigDecimal(6, product.getMaxAmount());
            statement.setString(7, product.getStatus());
            statement.setString(8, product.getDescription());
            statement.setLong(9, product.getProductId());
            statement.executeUpdate();
        }
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private BigDecimal sum(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getBigDecimal(1) : BigDecimal.ZERO.setScale(2);
        }
    }

    private String riskEventSelectSql() {
        return "SELECT e.event_id, e.customer_id, e.account_id, c.full_name, c.phone, "
                + "a.account_no, a.status AS account_status, e.transaction_no, e.txn_type, e.amount, "
                + "e.risk_score, e.risk_level, e.decision, e.hit_rules, e.reason, e.ip_address, "
                + "e.handle_status, e.handle_result, e.handler_admin_user_id, "
                + "u.username AS handler_username, e.handle_note, e.handled_at, e.created_at "
                + "FROM t_risk_event e JOIN t_customer c ON e.customer_id = c.customer_id "
                + "LEFT JOIN t_account a ON e.account_id = a.account_id "
                + "LEFT JOIN t_user u ON e.handler_admin_user_id = u.user_id";
    }

    private AdminCustomerView mapCustomerView(ResultSet resultSet) throws SQLException {
        AdminCustomerView view = new AdminCustomerView();
        view.setCustomerId(resultSet.getLong("customer_id"));
        view.setUsername(resultSet.getString("username"));
        view.setFullName(resultSet.getString("full_name"));
        view.setPhone(resultSet.getString("phone"));
        view.setRiskLevel(resultSet.getString("risk_level"));
        view.setAccountCount(resultSet.getInt("account_count"));
        view.setTotalAvailableBalance(resultSet.getBigDecimal("total_balance"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        return view;
    }

    private AdminRiskEventView mapRiskEventView(ResultSet resultSet) throws SQLException {
        AdminRiskEventView view = new AdminRiskEventView();
        view.setEventId(resultSet.getLong("event_id"));
        view.setCustomerId(resultSet.getLong("customer_id"));
        long accountId = resultSet.getLong("account_id");
        view.setAccountId(resultSet.wasNull() ? null : accountId);
        view.setFullName(resultSet.getString("full_name"));
        view.setPhone(resultSet.getString("phone"));
        view.setAccountNo(resultSet.getString("account_no"));
        view.setAccountStatus(resultSet.getString("account_status"));
        view.setTransactionNo(resultSet.getString("transaction_no"));
        view.setTxnType(resultSet.getString("txn_type"));
        view.setAmount(resultSet.getBigDecimal("amount"));
        view.setRiskScore(resultSet.getInt("risk_score"));
        view.setRiskLevel(resultSet.getString("risk_level"));
        view.setDecision(resultSet.getString("decision"));
        view.setHitRules(resultSet.getString("hit_rules"));
        view.setReason(resultSet.getString("reason"));
        view.setIpAddress(resultSet.getString("ip_address"));
        view.setHandleStatus(resultSet.getString("handle_status"));
        view.setHandleResult(resultSet.getString("handle_result"));
        long handlerAdminUserId = resultSet.getLong("handler_admin_user_id");
        view.setHandlerAdminUserId(resultSet.wasNull() ? null : handlerAdminUserId);
        view.setHandlerUsername(resultSet.getString("handler_username"));
        view.setHandleNote(resultSet.getString("handle_note"));
        view.setHandledAt(resultSet.getTimestamp("handled_at"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        return view;
    }

    private RiskActionLog mapRiskActionLog(ResultSet resultSet) throws SQLException {
        RiskActionLog log = new RiskActionLog();
        log.setActionId(resultSet.getLong("action_id"));
        log.setEventId(resultSet.getLong("event_id"));
        log.setAdminUserId(resultSet.getLong("admin_user_id"));
        log.setAdminUsername(resultSet.getString("admin_username"));
        log.setActionType(resultSet.getString("action_type"));
        log.setBeforeStatus(resultSet.getString("before_status"));
        log.setAfterStatus(resultSet.getString("after_status"));
        log.setNote(resultSet.getString("note"));
        log.setCreatedAt(resultSet.getTimestamp("created_at"));
        return log;
    }

    private RiskLimitRule mapRiskLimitRule(ResultSet resultSet) throws SQLException {
        RiskLimitRule rule = new RiskLimitRule();
        rule.setRuleId(resultSet.getLong("rule_id"));
        rule.setRuleCode(resultSet.getString("rule_code"));
        rule.setTxnType(resultSet.getString("txn_type"));
        rule.setCustomerRiskLevel(resultSet.getString("customer_risk_level"));
        rule.setSingleLimit(resultSet.getBigDecimal("single_limit"));
        rule.setDailyAmountLimit(resultSet.getBigDecimal("daily_amount_limit"));
        rule.setDailyCountLimit(resultSet.getInt("daily_count_limit"));
        rule.setStatus(resultSet.getString("status"));
        rule.setCreatedAt(resultSet.getTimestamp("created_at"));
        rule.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return rule;
    }

    private WealthProduct mapWealthProduct(ResultSet resultSet) throws SQLException {
        WealthProduct product = new WealthProduct();
        product.setProductId(resultSet.getLong("product_id"));
        product.setProductCode(resultSet.getString("product_code"));
        product.setProductName(resultSet.getString("product_name"));
        product.setRiskLevel(resultSet.getString("risk_level"));
        product.setExpectedRate(resultSet.getBigDecimal("expected_rate"));
        product.setPeriodDays(resultSet.getInt("period_days"));
        product.setMinAmount(resultSet.getBigDecimal("min_amount"));
        product.setMaxAmount(resultSet.getBigDecimal("max_amount"));
        product.setStatus(resultSet.getString("status"));
        product.setDescription(resultSet.getString("description"));
        product.setCreatedAt(resultSet.getTimestamp("created_at"));
        product.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return product;
    }
}
