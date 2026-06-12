package com.bank.dao.impl;

import com.bank.dao.BillDao;
import com.bank.dto.CategorySummary;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.TimeBucketSummary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BillDaoImpl implements BillDao {
    @Override
    public List<CategorySummary> findCategorySummary(Connection connection, long customerId, Long accountId,
                                                     Timestamp startAt, Timestamp endAt) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.txn_type, l.direction, COUNT(*) AS entry_count, ");
        sql.append("COALESCE(SUM(l.amount), 0) AS total_amount ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        sql.append("WHERE a.customer_id = ? AND l.created_at >= ? AND l.created_at < ? ");
        if (accountId != null) {
            sql.append("AND l.account_id = ? ");
        }
        sql.append("GROUP BY t.txn_type, l.direction ");
        sql.append("ORDER BY l.direction ASC, total_amount DESC");

        List<CategorySummary> summaries = new ArrayList<CategorySummary>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, customerId);
            statement.setTimestamp(index++, startAt);
            statement.setTimestamp(index++, endAt);
            if (accountId != null) {
                statement.setLong(index, accountId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    CategorySummary summary = new CategorySummary();
                    summary.setTxnType(resultSet.getString("txn_type"));
                    summary.setDirection(resultSet.getString("direction"));
                    summary.setEntryCount(resultSet.getInt("entry_count"));
                    summary.setTotalAmount(resultSet.getBigDecimal("total_amount"));
                    summaries.add(summary);
                }
            }
        }
        return summaries;
    }

    @Override
    public List<LedgerEntryView> findMonthlyEntries(Connection connection, long customerId, Long accountId,
                                                    Timestamp startAt, Timestamp endAt, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.ledger_id, t.transaction_no, t.txn_type, a.account_no, l.direction, l.amount, ");
        sql.append("l.balance_after, t.status, l.summary, t.remark, l.created_at ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        sql.append("WHERE a.customer_id = ? AND l.created_at >= ? AND l.created_at < ? ");
        if (accountId != null) {
            sql.append("AND l.account_id = ? ");
        }
        sql.append("ORDER BY l.created_at DESC, l.ledger_id DESC LIMIT ?");

        List<LedgerEntryView> entries = new ArrayList<LedgerEntryView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, customerId);
            statement.setTimestamp(index++, startAt);
            statement.setTimestamp(index++, endAt);
            if (accountId != null) {
                statement.setLong(index++, accountId);
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

    @Override
    public List<CategorySummary> findCategorySummaryByRange(Connection connection, long customerId, Long accountId,
                                                            Timestamp startAt, Timestamp endAt, String direction,
                                                            String txnType) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.txn_type, l.direction, COUNT(*) AS entry_count, ");
        sql.append("COALESCE(SUM(l.amount), 0) AS total_amount ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        appendReportWhere(sql, accountId, direction, txnType);
        sql.append("GROUP BY t.txn_type, l.direction ");
        sql.append("ORDER BY l.direction ASC, total_amount DESC");

        List<CategorySummary> summaries = new ArrayList<CategorySummary>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindReportWhere(statement, customerId, accountId, startAt, endAt, direction, txnType);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    CategorySummary summary = new CategorySummary();
                    summary.setTxnType(resultSet.getString("txn_type"));
                    summary.setDirection(resultSet.getString("direction"));
                    summary.setEntryCount(resultSet.getInt("entry_count"));
                    summary.setTotalAmount(resultSet.getBigDecimal("total_amount"));
                    summaries.add(summary);
                }
            }
        }
        return summaries;
    }

    @Override
    public List<TimeBucketSummary> findTimeBucketSummary(Connection connection, long customerId, Long accountId,
                                                         Timestamp startAt, Timestamp endAt, String direction,
                                                         String txnType, String bucketType) throws SQLException {
        String bucketKeySql = "DATE_FORMAT(l.created_at, '%Y-%m-%d')";
        String bucketLabelSql = "DATE_FORMAT(l.created_at, '%m-%d')";
        if ("DAY".equals(bucketType)) {
            bucketKeySql = "LPAD(HOUR(l.created_at), 2, '0')";
            bucketLabelSql = "CONCAT(LPAD(HOUR(l.created_at), 2, '0'), ':00')";
        } else if ("YEAR".equals(bucketType)) {
            bucketKeySql = "DATE_FORMAT(l.created_at, '%Y-%m')";
            bucketLabelSql = "DATE_FORMAT(l.created_at, '%Y-%m')";
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(bucketKeySql).append(" AS bucket_key, ");
        sql.append(bucketLabelSql).append(" AS bucket_label, ");
        sql.append("COUNT(*) AS entry_count, ");
        sql.append("COALESCE(SUM(CASE WHEN l.direction = 'IN' THEN l.amount ELSE 0 END), 0) AS total_income, ");
        sql.append("COALESCE(SUM(CASE WHEN l.direction = 'OUT' THEN l.amount ELSE 0 END), 0) AS total_expense ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        appendReportWhere(sql, accountId, direction, txnType);
        sql.append("GROUP BY bucket_key, bucket_label ");
        sql.append("ORDER BY bucket_key ASC");

        List<TimeBucketSummary> summaries = new ArrayList<TimeBucketSummary>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindReportWhere(statement, customerId, accountId, startAt, endAt, direction, txnType);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TimeBucketSummary summary = new TimeBucketSummary();
                    summary.setBucketKey(resultSet.getString("bucket_key"));
                    summary.setBucketLabel(resultSet.getString("bucket_label"));
                    summary.setEntryCount(resultSet.getInt("entry_count"));
                    summary.setTotalIncome(resultSet.getBigDecimal("total_income"));
                    summary.setTotalExpense(resultSet.getBigDecimal("total_expense"));
                    summary.setNetIncome(summary.getTotalIncome().subtract(summary.getTotalExpense()));
                    summaries.add(summary);
                }
            }
        }
        return summaries;
    }

    @Override
    public List<LedgerEntryView> findEntriesByRange(Connection connection, long customerId, Long accountId,
                                                    Timestamp startAt, Timestamp endAt, String direction,
                                                    String txnType, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.ledger_id, t.transaction_no, t.txn_type, a.account_no, l.direction, l.amount, ");
        sql.append("l.balance_after, t.status, l.summary, t.remark, l.created_at ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        appendReportWhere(sql, accountId, direction, txnType);
        sql.append("ORDER BY l.created_at DESC, l.ledger_id DESC LIMIT ?");
        return findLedgerEntries(connection, sql.toString(), customerId, accountId, startAt, endAt,
                direction, txnType, limit);
    }

    @Override
    public List<LedgerEntryView> findTopMovements(Connection connection, long customerId, Long accountId,
                                                  Timestamp startAt, Timestamp endAt, String direction,
                                                  String txnType, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.ledger_id, t.transaction_no, t.txn_type, a.account_no, l.direction, l.amount, ");
        sql.append("l.balance_after, t.status, l.summary, t.remark, l.created_at ");
        sql.append("FROM t_ledger_entry l ");
        sql.append("JOIN t_transaction t ON l.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON l.account_id = a.account_id ");
        appendReportWhere(sql, accountId, direction, txnType);
        sql.append("ORDER BY l.amount DESC, l.created_at DESC, l.ledger_id DESC LIMIT ?");
        return findLedgerEntries(connection, sql.toString(), customerId, accountId, startAt, endAt,
                direction, txnType, limit);
    }

    private List<LedgerEntryView> findLedgerEntries(Connection connection, String sql, long customerId, Long accountId,
                                                    Timestamp startAt, Timestamp endAt, String direction,
                                                    String txnType, int limit) throws SQLException {
        List<LedgerEntryView> entries = new ArrayList<LedgerEntryView>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindReportWhere(statement, customerId, accountId, startAt, endAt, direction, txnType);
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(mapLedgerEntryView(resultSet));
                }
            }
        }
        return entries;
    }

    private void appendReportWhere(StringBuilder sql, Long accountId, String direction, String txnType) {
        sql.append("WHERE a.customer_id = ? AND l.created_at >= ? AND l.created_at < ? ");
        if (accountId != null) {
            sql.append("AND l.account_id = ? ");
        }
        if (hasText(direction)) {
            sql.append("AND l.direction = ? ");
        }
        if (hasText(txnType)) {
            sql.append("AND t.txn_type = ? ");
        }
    }

    private int bindReportWhere(PreparedStatement statement, long customerId, Long accountId,
                                Timestamp startAt, Timestamp endAt, String direction, String txnType)
            throws SQLException {
        int index = 1;
        statement.setLong(index++, customerId);
        statement.setTimestamp(index++, startAt);
        statement.setTimestamp(index++, endAt);
        if (accountId != null) {
            statement.setLong(index++, accountId);
        }
        if (hasText(direction)) {
            statement.setString(index++, direction);
        }
        if (hasText(txnType)) {
            statement.setString(index++, txnType);
        }
        return index;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
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
