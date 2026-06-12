package com.bank.dao.impl;

import com.bank.bean.Account;
import com.bank.dao.AccountDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccountDaoImpl implements AccountDao {
    @Override
    public long insert(Connection connection, Account account) throws SQLException {
        String sql = "INSERT INTO t_account (customer_id, account_no, account_type, branch_name, "
                + "available_balance, frozen_balance, status, default_flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, account.getCustomerId());
            statement.setString(2, account.getAccountNo());
            statement.setString(3, account.getAccountType());
            statement.setString(4, account.getBranchName());
            statement.setBigDecimal(5, account.getAvailableBalance());
            statement.setBigDecimal(6, account.getFrozenBalance());
            statement.setString(7, account.getStatus());
            statement.setBoolean(8, account.isDefaultFlag());
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    @Override
    public boolean existsByAccountNo(Connection connection, String accountNo) throws SQLException {
        String sql = "SELECT 1 FROM t_account WHERE account_no = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public Account findById(Connection connection, long accountId) throws SQLException {
        String sql = "SELECT account_id, customer_id, account_no, account_type, branch_name, available_balance, "
                + "frozen_balance, status, default_flag, opened_at, updated_at FROM t_account WHERE account_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccount(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public Account findByAccountNo(Connection connection, String accountNo) throws SQLException {
        String sql = "SELECT account_id, customer_id, account_no, account_type, branch_name, available_balance, "
                + "frozen_balance, status, default_flag, opened_at, updated_at FROM t_account WHERE account_no = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccount(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public Account findByIdForUpdate(Connection connection, long accountId) throws SQLException {
        String sql = "SELECT account_id, customer_id, account_no, account_type, branch_name, available_balance, "
                + "frozen_balance, status, default_flag, opened_at, updated_at FROM t_account "
                + "WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccount(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<Account> findByCustomerId(Connection connection, long customerId) throws SQLException {
        String sql = "SELECT account_id, customer_id, account_no, account_type, branch_name, available_balance, "
                + "frozen_balance, status, default_flag, opened_at, updated_at FROM t_account "
                + "WHERE customer_id = ? ORDER BY default_flag DESC, opened_at ASC";
        List<Account> accounts = new ArrayList<Account>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(mapAccount(resultSet));
                }
            }
        }
        return accounts;
    }

    @Override
    public void updateAvailableBalance(Connection connection, long accountId, BigDecimal availableBalance)
            throws SQLException {
        String sql = "UPDATE t_account SET available_balance = ? WHERE account_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, availableBalance);
            statement.setLong(2, accountId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateBalances(Connection connection, long accountId, BigDecimal availableBalance,
                               BigDecimal frozenBalance) throws SQLException {
        String sql = "UPDATE t_account SET available_balance = ?, frozen_balance = ? WHERE account_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, availableBalance);
            statement.setBigDecimal(2, frozenBalance);
            statement.setLong(3, accountId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateStatus(Connection connection, long accountId, String status) throws SQLException {
        String sql = "UPDATE t_account SET status = ? WHERE account_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, accountId);
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

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        Account account = new Account();
        account.setAccountId(resultSet.getLong("account_id"));
        account.setCustomerId(resultSet.getLong("customer_id"));
        account.setAccountNo(resultSet.getString("account_no"));
        account.setAccountType(resultSet.getString("account_type"));
        account.setBranchName(resultSet.getString("branch_name"));
        account.setAvailableBalance(resultSet.getBigDecimal("available_balance"));
        account.setFrozenBalance(resultSet.getBigDecimal("frozen_balance"));
        account.setStatus(resultSet.getString("status"));
        account.setDefaultFlag(resultSet.getBoolean("default_flag"));
        account.setOpenedAt(resultSet.getTimestamp("opened_at"));
        account.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return account;
    }
}
