package com.bank.dao;

import com.bank.bean.Customer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

public interface CustomerDao {
    long insert(Connection connection, Customer customer) throws SQLException;

    Customer findByUserId(Connection connection, long userId) throws SQLException;

    Customer findById(Connection connection, long customerId) throws SQLException;

    void updateRiskProfile(Connection connection, long customerId, String riskLevel,
                           String riskLevelSource, Timestamp riskLevelUpdatedAt) throws SQLException;
}
