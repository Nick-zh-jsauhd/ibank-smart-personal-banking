package com.bank.dao.impl;

import com.bank.bean.Customer;
import com.bank.dao.CustomerDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class CustomerDaoImpl implements CustomerDao {
    @Override
    public long insert(Connection connection, Customer customer) throws SQLException {
        String sql = "INSERT INTO t_customer (user_id, full_name, id_card_no, phone, email, address, risk_level) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customer.getUserId());
            statement.setString(2, customer.getFullName());
            statement.setString(3, customer.getIdCardNo());
            statement.setString(4, customer.getPhone());
            statement.setString(5, customer.getEmail());
            statement.setString(6, customer.getAddress());
            statement.setString(7, customer.getRiskLevel());
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    @Override
    public Customer findByUserId(Connection connection, long userId) throws SQLException {
        String sql = "SELECT customer_id, user_id, full_name, id_card_no, phone, email, address, risk_level, "
                + "risk_level_source, risk_level_updated_at, "
                + "created_at, updated_at FROM t_customer WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public Customer findById(Connection connection, long customerId) throws SQLException {
        String sql = "SELECT customer_id, user_id, full_name, id_card_no, phone, email, address, risk_level, "
                + "risk_level_source, risk_level_updated_at, "
                + "created_at, updated_at FROM t_customer WHERE customer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateRiskProfile(Connection connection, long customerId, String riskLevel,
                                  String riskLevelSource, Timestamp riskLevelUpdatedAt) throws SQLException {
        String sql = "UPDATE t_customer SET risk_level = ?, risk_level_source = ?, "
                + "risk_level_updated_at = ? WHERE customer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, riskLevel);
            statement.setString(2, riskLevelSource);
            statement.setTimestamp(3, riskLevelUpdatedAt);
            statement.setLong(4, customerId);
            statement.executeUpdate();
        }
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("No generated key returned");
    }

    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(resultSet.getLong("customer_id"));
        customer.setUserId(resultSet.getLong("user_id"));
        customer.setFullName(resultSet.getString("full_name"));
        customer.setIdCardNo(resultSet.getString("id_card_no"));
        customer.setPhone(resultSet.getString("phone"));
        customer.setEmail(resultSet.getString("email"));
        customer.setAddress(resultSet.getString("address"));
        customer.setRiskLevel(resultSet.getString("risk_level"));
        customer.setRiskLevelSource(resultSet.getString("risk_level_source"));
        customer.setRiskLevelUpdatedAt(resultSet.getTimestamp("risk_level_updated_at"));
        customer.setCreatedAt(resultSet.getTimestamp("created_at"));
        customer.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return customer;
    }
}
