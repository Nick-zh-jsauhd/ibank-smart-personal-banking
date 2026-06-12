package com.bank.dao.impl;

import com.bank.bean.LedgerEntry;
import com.bank.dao.LedgerDao;
import com.bank.dto.LedgerEntryView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LedgerDaoImpl implements LedgerDao {
    @Override
    public long insert(Connection connection, LedgerEntry ledgerEntry) throws SQLException {
        String sql = "INSERT INTO t_ledger_entry (transaction_id, account_id, direction, amount, balance_after, summary) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, ledgerEntry.getTransactionId());
            statement.setLong(2, ledgerEntry.getAccountId());
            statement.setString(3, ledgerEntry.getDirection());
            statement.setBigDecimal(4, ledgerEntry.getAmount());
            statement.setBigDecimal(5, ledgerEntry.getBalanceAfter());
            statement.setString(6, ledgerEntry.getSummary());
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
    public List<LedgerEntryView> findByCustomer(Connection connection, long customerId, Long accountId,
                                                String direction, String txnType, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.ledger_id, t.transaction_no, t.txn_type, a.account_no, l.direction, l.amount, ");
        sql.append("l.balance_after, t.status, l.summary, t.remark, l.created_at ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        sql.append("WHERE a.customer_id = ? ");
        if (accountId != null) {
            sql.append("AND l.account_id = ? ");
        }
        if (direction != null && direction.length() > 0) {
            sql.append("AND l.direction = ? ");
        }
        if (txnType != null && txnType.length() > 0) {
            sql.append("AND t.txn_type = ? ");
        }
        sql.append("ORDER BY l.created_at DESC, l.ledger_id DESC LIMIT ?");

        List<LedgerEntryView> entries = new ArrayList<LedgerEntryView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, customerId);
            if (accountId != null) {
                statement.setLong(index++, accountId);
            }
            if (direction != null && direction.length() > 0) {
                statement.setString(index++, direction);
            }
            if (txnType != null && txnType.length() > 0) {
                statement.setString(index++, txnType);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapLedgerEntryView(resultSet));
                }
            }
        }
        return entries;
    }

    private LedgerEntryView mapLedgerEntryView(ResultSet resultSet) throws SQLException {
        LedgerEntryView view = new LedgerEntryView();
        view.setLedgerId(resultSet.getLong("ledger_id"));
        view.setTransactionNo(resultSet.getString("transaction_no"));
        view.setTxnType(resultSet.getString("txn_type"));
        view.setAccountNo(resultSet.getString("account_no"));
        view.setDirection(resultSet.getString("direction"));
        view.setAmount(resultSet.getBigDecimal("amount"));
        view.setBalanceAfter(resultSet.getBigDecimal("balance_after"));
        view.setStatus(resultSet.getString("status"));
        view.setSummary(resultSet.getString("summary"));
        view.setRemark(resultSet.getString("remark"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        return view;
    }
}
