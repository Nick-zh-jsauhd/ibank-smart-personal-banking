package com.bank.dao.impl;

import com.bank.bean.CustomerLimit;
import com.bank.bean.RiskLimitRule;
import com.bank.bean.RiskLimitUsage;
import com.bank.dao.RiskDao;
import com.bank.dto.RiskCheckRequest;
import com.bank.dto.RiskDecision;
import com.bank.dto.RiskEventView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RiskDaoImpl implements RiskDao {
    @Override
    public RiskLimitRule findActiveRule(Connection connection, String txnType, String customerRiskLevel)
            throws SQLException {
        String sql = "SELECT rule_id, rule_code, txn_type, customer_risk_level, single_limit, "
                + "daily_amount_limit, daily_count_limit, status, created_at, updated_at "
                + "FROM t_risk_limit_rule WHERE txn_type = ? AND customer_risk_level = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, txnType);
            statement.setString(2, customerRiskLevel);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRule(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public CustomerLimit findActiveCustomerLimit(Connection connection, long customerId, String txnType)
            throws SQLException {
        String sql = "SELECT customer_limit_id, customer_id, txn_type, single_limit, daily_amount_limit, "
                + "daily_count_limit, status, created_at, updated_at "
                + "FROM t_customer_limit WHERE customer_id = ? AND txn_type = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, txnType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomerLimit(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public RiskLimitUsage lockUsage(Connection connection, long customerId, LocalDate txnDate, String txnType)
            throws SQLException {
        Date date = Date.valueOf(txnDate);
        String insertSql = "INSERT INTO t_risk_limit_usage (customer_id, txn_date, txn_type, used_amount, used_count) "
                + "VALUES (?, ?, ?, 0.00, 0) ON DUPLICATE KEY UPDATE usage_id = LAST_INSERT_ID(usage_id)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setLong(1, customerId);
            statement.setDate(2, date);
            statement.setString(3, txnType);
            statement.executeUpdate();
        }

        String selectSql = "SELECT usage_id, customer_id, txn_date, txn_type, used_amount, used_count "
                + "FROM t_risk_limit_usage WHERE customer_id = ? AND txn_date = ? AND txn_type = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setLong(1, customerId);
            statement.setDate(2, date);
            statement.setString(3, txnType);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUsage(resultSet);
                }
            }
        }
        throw new SQLException("Unable to lock risk usage row");
    }

    @Override
    public void updateUsage(Connection connection, long usageId, BigDecimal usedAmount, int usedCount)
            throws SQLException {
        String sql = "UPDATE t_risk_limit_usage SET used_amount = ?, used_count = ? WHERE usage_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, usedAmount);
            statement.setInt(2, usedCount);
            statement.setLong(3, usageId);
            statement.executeUpdate();
        }
    }

    @Override
    public int countRecentOutflow(Connection connection, long customerId, Timestamp since) throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_transaction WHERE customer_id = ? AND status = 'SUCCESS' "
                + "AND txn_type IN ('WITHDRAW', 'TRANSFER_INNER', 'PAYMENT', 'BUY_WEALTH') "
                + "AND created_at >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setTimestamp(2, since);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public BigDecimal averageSuccessfulAmount(Connection connection, long customerId, String txnType, Timestamp since)
            throws SQLException {
        String sql = "SELECT AVG(amount) FROM t_transaction WHERE customer_id = ? AND txn_type = ? "
                + "AND status = 'SUCCESS' AND created_at >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setString(2, txnType);
            statement.setTimestamp(3, since);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
            }
        }
        return null;
    }

    @Override
    public int countSuccessfulTransferToAccount(Connection connection, long customerId, long targetAccountId)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_transaction WHERE customer_id = ? AND to_account_id = ? "
                + "AND txn_type = 'TRANSFER_INNER' AND status = 'SUCCESS'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setLong(2, targetAccountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public void insertEvent(Connection connection, RiskCheckRequest request, RiskDecision decision)
            throws SQLException {
        String sql = "INSERT INTO t_risk_event (customer_id, account_id, transaction_no, txn_type, amount, "
                + "risk_score, risk_level, decision, hit_rules, reason, ip_address) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.getCustomerId());
            if (request.getAccountId() == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, request.getAccountId());
            }
            statement.setString(3, request.getTransactionNo());
            statement.setString(4, request.getTxnType());
            statement.setBigDecimal(5, request.getAmount());
            statement.setInt(6, decision.getRiskScore());
            statement.setString(7, decision.getRiskLevel());
            statement.setString(8, decision.getDecision());
            statement.setString(9, decision.getHitRules());
            statement.setString(10, decision.getReason());
            statement.setString(11, request.getIpAddress());
            statement.executeUpdate();
        }
    }

    @Override
    public List<RiskEventView> findEventsByCustomer(Connection connection, long customerId, String decision, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.event_id, e.transaction_no, e.txn_type, a.account_no, e.amount, e.risk_score, ");
        sql.append("e.risk_level, e.decision, e.hit_rules, e.reason, e.ip_address, e.created_at ");
        sql.append("FROM t_risk_event e LEFT JOIN t_account a ON e.account_id = a.account_id ");
        sql.append("WHERE e.customer_id = ? ");
        if (decision != null && decision.length() > 0) {
            sql.append("AND e.decision = ? ");
        }
        sql.append("ORDER BY e.created_at DESC, e.event_id DESC LIMIT ?");

        List<RiskEventView> events = new ArrayList<RiskEventView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, customerId);
            if (decision != null && decision.length() > 0) {
                statement.setString(index++, decision);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEventView(resultSet));
                }
            }
        }
        return events;
    }

    private RiskLimitRule mapRule(ResultSet resultSet) throws SQLException {
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

    private CustomerLimit mapCustomerLimit(ResultSet resultSet) throws SQLException {
        CustomerLimit limit = new CustomerLimit();
        limit.setCustomerLimitId(resultSet.getLong("customer_limit_id"));
        limit.setCustomerId(resultSet.getLong("customer_id"));
        limit.setTxnType(resultSet.getString("txn_type"));
        limit.setSingleLimit(resultSet.getBigDecimal("single_limit"));
        limit.setDailyAmountLimit(resultSet.getBigDecimal("daily_amount_limit"));
        limit.setDailyCountLimit(resultSet.getInt("daily_count_limit"));
        limit.setStatus(resultSet.getString("status"));
        limit.setCreatedAt(resultSet.getTimestamp("created_at"));
        limit.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return limit;
    }

    private RiskLimitUsage mapUsage(ResultSet resultSet) throws SQLException {
        RiskLimitUsage usage = new RiskLimitUsage();
        usage.setUsageId(resultSet.getLong("usage_id"));
        usage.setCustomerId(resultSet.getLong("customer_id"));
        usage.setTxnDate(resultSet.getDate("txn_date"));
        usage.setTxnType(resultSet.getString("txn_type"));
        usage.setUsedAmount(resultSet.getBigDecimal("used_amount"));
        usage.setUsedCount(resultSet.getInt("used_count"));
        return usage;
    }

    private RiskEventView mapEventView(ResultSet resultSet) throws SQLException {
        RiskEventView event = new RiskEventView();
        event.setEventId(resultSet.getLong("event_id"));
        event.setTransactionNo(resultSet.getString("transaction_no"));
        event.setTxnType(resultSet.getString("txn_type"));
        event.setAccountNo(resultSet.getString("account_no"));
        event.setAmount(resultSet.getBigDecimal("amount"));
        event.setRiskScore(resultSet.getInt("risk_score"));
        event.setRiskLevel(resultSet.getString("risk_level"));
        event.setDecision(resultSet.getString("decision"));
        event.setHitRules(resultSet.getString("hit_rules"));
        event.setReason(resultSet.getString("reason"));
        event.setIpAddress(resultSet.getString("ip_address"));
        event.setCreatedAt(resultSet.getTimestamp("created_at"));
        return event;
    }
}
