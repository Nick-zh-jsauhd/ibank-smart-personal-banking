package com.bank.dao;

import com.bank.bean.TransactionRecord;

import java.sql.Connection;
import java.sql.SQLException;

public interface TransactionDao {
    long insert(Connection connection, TransactionRecord transactionRecord) throws SQLException;

    void updateStatus(Connection connection, long transactionId, String status) throws SQLException;
}
