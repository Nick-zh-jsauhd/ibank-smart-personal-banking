package com.bank.dao;

import com.bank.dto.CategorySummary;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.TimeBucketSummary;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface BillDao {
    List<CategorySummary> findCategorySummary(Connection connection, long customerId, Long accountId,
                                              Timestamp startAt, Timestamp endAt) throws SQLException;

    List<LedgerEntryView> findMonthlyEntries(Connection connection, long customerId, Long accountId,
                                             Timestamp startAt, Timestamp endAt, int limit) throws SQLException;

    List<CategorySummary> findCategorySummaryByRange(Connection connection, long customerId, Long accountId,
                                                     Timestamp startAt, Timestamp endAt, String direction,
                                                     String txnType) throws SQLException;

    List<TimeBucketSummary> findTimeBucketSummary(Connection connection, long customerId, Long accountId,
                                                  Timestamp startAt, Timestamp endAt, String direction,
                                                  String txnType, String bucketType) throws SQLException;

    List<LedgerEntryView> findEntriesByRange(Connection connection, long customerId, Long accountId,
                                             Timestamp startAt, Timestamp endAt, String direction, String txnType,
                                             int limit) throws SQLException;

    List<LedgerEntryView> findTopMovements(Connection connection, long customerId, Long accountId,
                                           Timestamp startAt, Timestamp endAt, String direction, String txnType,
                                           int limit) throws SQLException;
}
