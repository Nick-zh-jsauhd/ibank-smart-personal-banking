package com.bank.dao;

import com.bank.bean.AdjustmentActionLog;
import com.bank.bean.AdjustmentRequest;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface AdjustmentDao {
    long insertRequest(Connection connection, AdjustmentRequest request) throws SQLException;

    List<AdjustmentRequest> findRequests(Connection connection, String status, int limit) throws SQLException;

    List<AdjustmentRequest> findBySourceTicket(Connection connection, long ticketId, int limit) throws SQLException;

    AdjustmentRequest findById(Connection connection, long adjustmentId) throws SQLException;

    AdjustmentRequest findByIdForUpdate(Connection connection, long adjustmentId) throws SQLException;

    void updateReview(Connection connection, long adjustmentId, long reviewerAdminUserId, String status,
                      String reviewNote, Timestamp reviewedAt) throws SQLException;

    void updateExecuted(Connection connection, long adjustmentId, long transactionId, long ledgerId,
                        Timestamp executedAt) throws SQLException;

    void insertActionLog(Connection connection, AdjustmentActionLog actionLog) throws SQLException;

    List<AdjustmentActionLog> findActionLogs(Connection connection, long adjustmentId) throws SQLException;
}
