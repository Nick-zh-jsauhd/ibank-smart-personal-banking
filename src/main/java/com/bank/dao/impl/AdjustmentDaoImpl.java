package com.bank.dao.impl;

import com.bank.bean.AdjustmentActionLog;
import com.bank.bean.AdjustmentRequest;
import com.bank.dao.AdjustmentDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AdjustmentDaoImpl implements AdjustmentDao {
    @Override
    public long insertRequest(Connection connection, AdjustmentRequest request) throws SQLException {
        String sql = "INSERT INTO t_adjustment_request (adjustment_no, reconciliation_item_id, source_type, "
                + "source_ticket_id, account_id, customer_id, direction, amount, reason, evidence, status, "
                + "applicant_admin_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, request.getAdjustmentNo());
            if (request.getReconciliationItemId() == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, request.getReconciliationItemId());
            }
            statement.setString(3, request.getSourceType());
            if (request.getSourceTicketId() == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, request.getSourceTicketId());
            }
            statement.setLong(5, request.getAccountId());
            statement.setLong(6, request.getCustomerId());
            statement.setString(7, request.getDirection());
            statement.setBigDecimal(8, request.getAmount());
            statement.setString(9, request.getReason());
            statement.setString(10, request.getEvidence());
            statement.setString(11, request.getStatus());
            statement.setLong(12, request.getApplicantAdminUserId());
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
    public List<AdjustmentRequest> findRequests(Connection connection, String status, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append(selectSql());
        sql.append("WHERE 1 = 1 ");
        if (status != null && status.length() > 0) {
            sql.append("AND ar.status = ? ");
        }
        sql.append("ORDER BY FIELD(ar.status, 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EXECUTED', 'FAILED'), ");
        sql.append("ar.created_at DESC, ar.adjustment_id DESC LIMIT ?");

        List<AdjustmentRequest> requests = new ArrayList<AdjustmentRequest>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRequest(resultSet));
                }
            }
        }
        return requests;
    }

    @Override
    public List<AdjustmentRequest> findBySourceTicket(Connection connection, long ticketId, int limit)
            throws SQLException {
        String sql = selectSql() + "WHERE ar.source_type = 'SERVICE_TICKET' AND ar.source_ticket_id = ? "
                + "ORDER BY ar.created_at DESC, ar.adjustment_id DESC LIMIT ?";
        List<AdjustmentRequest> requests = new ArrayList<AdjustmentRequest>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapRequest(resultSet));
                }
            }
        }
        return requests;
    }

    @Override
    public AdjustmentRequest findById(Connection connection, long adjustmentId) throws SQLException {
        String sql = selectSql() + "WHERE ar.adjustment_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adjustmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRequest(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public AdjustmentRequest findByIdForUpdate(Connection connection, long adjustmentId) throws SQLException {
        String sql = selectSql() + "WHERE ar.adjustment_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adjustmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRequest(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateReview(Connection connection, long adjustmentId, long reviewerAdminUserId, String status,
                             String reviewNote, Timestamp reviewedAt) throws SQLException {
        String sql = "UPDATE t_adjustment_request SET reviewer_admin_user_id = ?, status = ?, "
                + "review_note = ?, reviewed_at = ? WHERE adjustment_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reviewerAdminUserId);
            statement.setString(2, status);
            statement.setString(3, reviewNote);
            statement.setTimestamp(4, reviewedAt);
            statement.setLong(5, adjustmentId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateExecuted(Connection connection, long adjustmentId, long transactionId, long ledgerId,
                               Timestamp executedAt) throws SQLException {
        String sql = "UPDATE t_adjustment_request SET status = 'EXECUTED', executed_transaction_id = ?, "
                + "executed_ledger_id = ?, executed_at = ? WHERE adjustment_id = ? AND status = 'APPROVED'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, ledgerId);
            statement.setTimestamp(3, executedAt);
            statement.setLong(4, adjustmentId);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertActionLog(Connection connection, AdjustmentActionLog actionLog) throws SQLException {
        String sql = "INSERT INTO t_adjustment_action_log (adjustment_id, admin_user_id, action_type, "
                + "before_status, after_status, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actionLog.getAdjustmentId());
            statement.setLong(2, actionLog.getAdminUserId());
            statement.setString(3, actionLog.getActionType());
            statement.setString(4, actionLog.getBeforeStatus());
            statement.setString(5, actionLog.getAfterStatus());
            statement.setString(6, actionLog.getNote());
            statement.executeUpdate();
        }
    }

    @Override
    public List<AdjustmentActionLog> findActionLogs(Connection connection, long adjustmentId) throws SQLException {
        String sql = "SELECT l.action_id, l.adjustment_id, l.admin_user_id, u.username AS admin_username, "
                + "l.action_type, l.before_status, l.after_status, l.note, l.created_at "
                + "FROM t_adjustment_action_log l "
                + "LEFT JOIN t_user u ON l.admin_user_id = u.user_id "
                + "WHERE l.adjustment_id = ? ORDER BY l.created_at DESC, l.action_id DESC";
        List<AdjustmentActionLog> logs = new ArrayList<AdjustmentActionLog>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adjustmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapActionLog(resultSet));
                }
            }
        }
        return logs;
    }

    private String selectSql() {
        return "SELECT ar.adjustment_id, ar.adjustment_no, ar.reconciliation_item_id, ar.source_type, "
                + "ar.source_ticket_id, "
                + "ar.account_id, ar.customer_id, ar.direction, ar.amount, ar.reason, ar.evidence, ar.status, "
                + "ar.applicant_admin_user_id, applicant.username AS applicant_username, "
                + "ar.reviewer_admin_user_id, reviewer.username AS reviewer_username, ar.review_note, "
                + "ar.reviewed_at, ar.executed_transaction_id, ar.executed_ledger_id, ar.executed_at, "
                + "ar.created_at, ar.updated_at, a.account_no, c.full_name AS customer_name, "
                + "ri.check_type AS reconciliation_check_type, ri.business_id AS reconciliation_business_id, "
                + "ri.status AS reconciliation_status "
                + "FROM t_adjustment_request ar "
                + "JOIN t_account a ON ar.account_id = a.account_id "
                + "JOIN t_customer c ON ar.customer_id = c.customer_id "
                + "LEFT JOIN t_reconciliation_item ri ON ar.reconciliation_item_id = ri.item_id "
                + "LEFT JOIN t_user applicant ON ar.applicant_admin_user_id = applicant.user_id "
                + "LEFT JOIN t_user reviewer ON ar.reviewer_admin_user_id = reviewer.user_id ";
    }

    private AdjustmentRequest mapRequest(ResultSet resultSet) throws SQLException {
        AdjustmentRequest request = new AdjustmentRequest();
        request.setAdjustmentId(resultSet.getLong("adjustment_id"));
        request.setAdjustmentNo(resultSet.getString("adjustment_no"));
        long itemId = resultSet.getLong("reconciliation_item_id");
        request.setReconciliationItemId(resultSet.wasNull() ? null : Long.valueOf(itemId));
        request.setSourceType(resultSet.getString("source_type"));
        long sourceTicketId = resultSet.getLong("source_ticket_id");
        request.setSourceTicketId(resultSet.wasNull() ? null : Long.valueOf(sourceTicketId));
        request.setAccountId(resultSet.getLong("account_id"));
        request.setCustomerId(resultSet.getLong("customer_id"));
        request.setDirection(resultSet.getString("direction"));
        request.setAmount(resultSet.getBigDecimal("amount"));
        request.setReason(resultSet.getString("reason"));
        request.setEvidence(resultSet.getString("evidence"));
        request.setStatus(resultSet.getString("status"));
        request.setApplicantAdminUserId(resultSet.getLong("applicant_admin_user_id"));
        long reviewerId = resultSet.getLong("reviewer_admin_user_id");
        request.setReviewerAdminUserId(resultSet.wasNull() ? null : reviewerId);
        request.setReviewNote(resultSet.getString("review_note"));
        request.setReviewedAt(resultSet.getTimestamp("reviewed_at"));
        long transactionId = resultSet.getLong("executed_transaction_id");
        request.setExecutedTransactionId(resultSet.wasNull() ? null : transactionId);
        long ledgerId = resultSet.getLong("executed_ledger_id");
        request.setExecutedLedgerId(resultSet.wasNull() ? null : ledgerId);
        request.setExecutedAt(resultSet.getTimestamp("executed_at"));
        request.setCreatedAt(resultSet.getTimestamp("created_at"));
        request.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        request.setAccountNo(resultSet.getString("account_no"));
        request.setCustomerName(resultSet.getString("customer_name"));
        request.setApplicantUsername(resultSet.getString("applicant_username"));
        request.setReviewerUsername(resultSet.getString("reviewer_username"));
        request.setReconciliationCheckType(resultSet.getString("reconciliation_check_type"));
        request.setReconciliationBusinessId(resultSet.getString("reconciliation_business_id"));
        request.setReconciliationStatus(resultSet.getString("reconciliation_status"));
        return request;
    }

    private AdjustmentActionLog mapActionLog(ResultSet resultSet) throws SQLException {
        AdjustmentActionLog actionLog = new AdjustmentActionLog();
        actionLog.setActionId(resultSet.getLong("action_id"));
        actionLog.setAdjustmentId(resultSet.getLong("adjustment_id"));
        actionLog.setAdminUserId(resultSet.getLong("admin_user_id"));
        actionLog.setAdminUsername(resultSet.getString("admin_username"));
        actionLog.setActionType(resultSet.getString("action_type"));
        actionLog.setBeforeStatus(resultSet.getString("before_status"));
        actionLog.setAfterStatus(resultSet.getString("after_status"));
        actionLog.setNote(resultSet.getString("note"));
        actionLog.setCreatedAt(resultSet.getTimestamp("created_at"));
        return actionLog;
    }
}
