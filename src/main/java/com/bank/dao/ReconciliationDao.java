package com.bank.dao;

import com.bank.bean.ReconciliationBatch;
import com.bank.bean.ReconciliationActionLog;
import com.bank.bean.ReconciliationItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface ReconciliationDao {
    long insertBatch(Connection connection, ReconciliationBatch batch) throws SQLException;

    void completeBatch(Connection connection, long batchId, String status, int totalChecks,
                       int exceptionCount, Timestamp finishedAt) throws SQLException;

    void insertItem(Connection connection, ReconciliationItem item) throws SQLException;

    List<ReconciliationBatch> findRecentBatches(Connection connection, int limit) throws SQLException;

    ReconciliationBatch findBatchById(Connection connection, long batchId) throws SQLException;

    List<ReconciliationItem> findItemsByBatch(Connection connection, long batchId) throws SQLException;

    List<ReconciliationItem> findItems(Connection connection, String status, String severity,
                                       String checkType, int limit) throws SQLException;

    int countUnfinishedItemsByBatch(Connection connection, long batchId) throws SQLException;

    ReconciliationItem findItemById(Connection connection, long itemId) throws SQLException;

    void updateItemHandling(Connection connection, long itemId, long adminUserId, String status,
                            String handleResult, String handleNote, Timestamp handledAt) throws SQLException;

    void insertActionLog(Connection connection, ReconciliationActionLog actionLog) throws SQLException;

    List<ReconciliationActionLog> findActionLogsByItem(Connection connection, long itemId) throws SQLException;
}
