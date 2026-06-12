package com.bank.dao;

import com.bank.bean.LedgerEntry;
import com.bank.dto.LedgerEntryView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface LedgerDao {
    long insert(Connection connection, LedgerEntry ledgerEntry) throws SQLException;

    List<LedgerEntryView> findByCustomer(Connection connection, long customerId, Long accountId,
                                         String direction, String txnType, int limit) throws SQLException;
}
