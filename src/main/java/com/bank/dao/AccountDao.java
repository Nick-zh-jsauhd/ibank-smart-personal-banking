package com.bank.dao;

import com.bank.bean.Account;

import java.sql.Connection;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

public interface AccountDao {
    long insert(Connection connection, Account account) throws SQLException;

    boolean existsByAccountNo(Connection connection, String accountNo) throws SQLException;

    Account findById(Connection connection, long accountId) throws SQLException;

    Account findByAccountNo(Connection connection, String accountNo) throws SQLException;

    Account findByIdForUpdate(Connection connection, long accountId) throws SQLException;

    List<Account> findByCustomerId(Connection connection, long customerId) throws SQLException;

    void updateAvailableBalance(Connection connection, long accountId, BigDecimal availableBalance) throws SQLException;

    void updateBalances(Connection connection, long accountId, BigDecimal availableBalance,
                        BigDecimal frozenBalance) throws SQLException;

    void updateStatus(Connection connection, long accountId, String status) throws SQLException;
}
