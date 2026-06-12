package com.bank.dao.impl;

import com.bank.bean.TransactionRecord;
import com.bank.dao.TransactionDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class TransactionDaoImpl implements TransactionDao {
    @Override
    public long insert(Connection connection, TransactionRecord transactionRecord) throws SQLException {
        String sql = "INSERT INTO t_transaction (transaction_no, customer_id, from_account_id, to_account_id, "
                + "txn_type, amount, status, risk_score, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transactionRecord.getTransactionNo());
            statement.setLong(2, transactionRecord.getCustomerId());
            if (transactionRecord.getFromAccountId() == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, transactionRecord.getFromAccountId());
            }
            if (transactionRecord.getToAccountId() == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, transactionRecord.getToAccountId());
            }
            statement.setString(5, transactionRecord.getTxnType());
            statement.setBigDecimal(6, transactionRecord.getAmount());
            statement.setString(7, transactionRecord.getStatus());
            statement.setInt(8, transactionRecord.getRiskScore());
            statement.setString(9, transactionRecord.getRemark());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No generated key returned");
    }

    @Override
    public void updateStatus(Connection connection, long transactionId, String status) throws SQLException {
        String sql = "UPDATE t_transaction SET status = ? WHERE transaction_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, transactionId);
            statement.executeUpdate();
        }
    }
}
