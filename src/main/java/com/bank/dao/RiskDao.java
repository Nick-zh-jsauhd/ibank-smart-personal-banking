package com.bank.dao;

import com.bank.bean.CustomerLimit;
import com.bank.bean.RiskLimitRule;
import com.bank.bean.RiskLimitUsage;
import com.bank.dto.RiskCheckRequest;
import com.bank.dto.RiskDecision;
import com.bank.dto.RiskEventView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public interface RiskDao {
    RiskLimitRule findActiveRule(Connection connection, String txnType, String customerRiskLevel)
            throws SQLException;

    CustomerLimit findActiveCustomerLimit(Connection connection, long customerId, String txnType)
            throws SQLException;

    RiskLimitUsage lockUsage(Connection connection, long customerId, LocalDate txnDate, String txnType)
            throws SQLException;

    void updateUsage(Connection connection, long usageId, BigDecimal usedAmount, int usedCount)
            throws SQLException;

    int countRecentOutflow(Connection connection, long customerId, Timestamp since) throws SQLException;

    BigDecimal averageSuccessfulAmount(Connection connection, long customerId, String txnType, Timestamp since)
            throws SQLException;

    int countSuccessfulTransferToAccount(Connection connection, long customerId, long targetAccountId)
            throws SQLException;

    void insertEvent(Connection connection, RiskCheckRequest request, RiskDecision decision) throws SQLException;

    List<RiskEventView> findEventsByCustomer(Connection connection, long customerId, String decision, int limit)
            throws SQLException;
}
