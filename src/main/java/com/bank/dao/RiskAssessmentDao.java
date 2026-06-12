package com.bank.dao;

import com.bank.bean.RiskAssessment;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface RiskAssessmentDao {
    long insert(Connection connection, RiskAssessment assessment) throws SQLException;

    RiskAssessment findLatestByCustomer(Connection connection, long customerId) throws SQLException;

    RiskAssessment findLatestValidByCustomer(Connection connection, long customerId, Timestamp now)
            throws SQLException;

    List<RiskAssessment> findByCustomer(Connection connection, long customerId, int limit) throws SQLException;
}
