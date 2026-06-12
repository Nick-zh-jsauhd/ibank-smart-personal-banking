package com.bank.dao;

import com.bank.bean.RiskLimitRule;
import com.bank.bean.RiskActionLog;
import com.bank.bean.WealthProduct;
import com.bank.dto.AdminCustomerView;
import com.bank.dto.AdminDashboardMetrics;
import com.bank.dto.AdminRiskEventView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface AdminDao {
    AdminDashboardMetrics loadDashboardMetrics(Connection connection) throws SQLException;

    List<AdminCustomerView> findCustomers(Connection connection, String keyword, String riskLevel, int limit)
            throws SQLException;

    List<AdminRiskEventView> findRiskEvents(Connection connection, String decision, String handleStatus, int limit)
            throws SQLException;

    AdminRiskEventView findRiskEventById(Connection connection, long eventId) throws SQLException;

    AdminRiskEventView findRiskEventByIdForUpdate(Connection connection, long eventId) throws SQLException;

    void updateRiskEventHandling(Connection connection, long eventId, String handleStatus, String handleResult,
                                 long handlerAdminUserId, String handleNote) throws SQLException;

    void insertRiskActionLog(Connection connection, RiskActionLog actionLog) throws SQLException;

    List<RiskActionLog> findRiskActionLogs(Connection connection, long eventId) throws SQLException;

    List<RiskLimitRule> findRiskRules(Connection connection, String txnType, String riskLevel, int limit)
            throws SQLException;

    RiskLimitRule findRiskRuleByIdForUpdate(Connection connection, long ruleId) throws SQLException;

    void updateRiskRule(Connection connection, RiskLimitRule rule) throws SQLException;

    List<WealthProduct> findWealthProducts(Connection connection, String status, int limit) throws SQLException;

    WealthProduct findWealthProductByIdForUpdate(Connection connection, long productId) throws SQLException;

    void updateWealthProduct(Connection connection, WealthProduct product) throws SQLException;
}
