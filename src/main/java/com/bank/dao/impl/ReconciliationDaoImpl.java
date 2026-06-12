package com.bank.dao.impl;

import com.bank.bean.ReconciliationBatch;
import com.bank.bean.ReconciliationActionLog;
import com.bank.bean.ReconciliationItem;
import com.bank.dao.ReconciliationDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationDaoImpl implements ReconciliationDao {
    @Override
    public long insertBatch(Connection connection, ReconciliationBatch batch) throws SQLException {
        String sql = "INSERT INTO t_reconciliation_batch (recon_date, status, total_checks, exception_count, "
                + "created_by_admin_user_id, started_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDate(1, batch.getReconDate());
            statement.setString(2, batch.getStatus());
            statement.setInt(3, batch.getTotalChecks());
            statement.setInt(4, batch.getExceptionCount());
            statement.setLong(5, batch.getCreatedByAdminUserId());
            statement.setTimestamp(6, batch.getStartedAt());
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
    public void completeBatch(Connection connection, long batchId, String status, int totalChecks,
                              int exceptionCount, Timestamp finishedAt) throws SQLException {
        String sql = "UPDATE t_reconciliation_batch SET status = ?, total_checks = ?, exception_count = ?, "
                + "finished_at = ? WHERE batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, totalChecks);
            statement.setInt(3, exceptionCount);
            statement.setTimestamp(4, finishedAt);
            statement.setLong(5, batchId);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertItem(Connection connection, ReconciliationItem item) throws SQLException {
        String sql = "INSERT INTO t_reconciliation_item (batch_id, check_type, severity, business_type, "
                + "business_id, expected_value, actual_value, description, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.getBatchId());
            statement.setString(2, item.getCheckType());
            statement.setString(3, item.getSeverity());
            statement.setString(4, item.getBusinessType());
            statement.setString(5, item.getBusinessId());
            statement.setString(6, item.getExpectedValue());
            statement.setString(7, item.getActualValue());
            statement.setString(8, item.getDescription());
            statement.setString(9, item.getStatus());
            statement.executeUpdate();
        }
    }

    @Override
    public List<ReconciliationBatch> findRecentBatches(Connection connection, int limit) throws SQLException {
        String sql = "SELECT b.batch_id, b.recon_date, b.status, b.total_checks, b.exception_count, "
                + "b.created_by_admin_user_id, u.username AS created_by_username, b.started_at, "
                + "b.finished_at, b.created_at "
                + "FROM t_reconciliation_batch b "
                + "LEFT JOIN t_user u ON b.created_by_admin_user_id = u.user_id "
                + "ORDER BY b.started_at DESC, b.batch_id DESC LIMIT ?";
        List<ReconciliationBatch> batches = new ArrayList<ReconciliationBatch>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    batches.add(mapBatch(resultSet));
                }
            }
        }
        return batches;
    }

    @Override
    public ReconciliationBatch findBatchById(Connection connection, long batchId) throws SQLException {
        String sql = "SELECT b.batch_id, b.recon_date, b.status, b.total_checks, b.exception_count, "
                + "b.created_by_admin_user_id, u.username AS created_by_username, b.started_at, "
                + "b.finished_at, b.created_at "
                + "FROM t_reconciliation_batch b "
                + "LEFT JOIN t_user u ON b.created_by_admin_user_id = u.user_id "
                + "WHERE b.batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBatch(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<ReconciliationItem> findItemsByBatch(Connection connection, long batchId) throws SQLException {
        String sql = itemSelectSql()
                + "WHERE i.batch_id = ? "
                + "ORDER BY FIELD(i.severity, 'CRITICAL', 'WARN'), i.item_id";
        List<ReconciliationItem> items = new ArrayList<ReconciliationItem>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
            }
        }
        return items;
    }

    @Override
    public List<ReconciliationItem> findItems(Connection connection, String status, String severity,
                                              String checkType, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append(itemSelectSql());
        sql.append("WHERE 1 = 1 ");
        if (status != null && status.length() > 0) {
            sql.append("AND i.status = ? ");
        }
        if (severity != null && severity.length() > 0) {
            sql.append("AND i.severity = ? ");
        }
        if (checkType != null && checkType.length() > 0) {
            sql.append("AND i.check_type = ? ");
        }
        sql.append("ORDER BY FIELD(i.status, 'OPEN', 'INVESTIGATING', 'CONFIRMED_EXCEPTION', 'FIXED', 'ACCEPTED', 'CLOSED'), ");
        sql.append("FIELD(i.severity, 'CRITICAL', 'WARN'), i.created_at DESC, i.item_id DESC LIMIT ?");

        List<ReconciliationItem> items = new ArrayList<ReconciliationItem>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            if (severity != null && severity.length() > 0) {
                statement.setString(index++, severity);
            }
            if (checkType != null && checkType.length() > 0) {
                statement.setString(index++, checkType);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
            }
        }
        return items;
    }

    @Override
    public int countUnfinishedItemsByBatch(Connection connection, long batchId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_reconciliation_item "
                + "WHERE batch_id = ? AND status IN ('OPEN', 'INVESTIGATING', 'CONFIRMED_EXCEPTION')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public ReconciliationItem findItemById(Connection connection, long itemId) throws SQLException {
        String sql = itemSelectSql() + "WHERE i.item_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapItem(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public void updateItemHandling(Connection connection, long itemId, long adminUserId, String status,
                                   String handleResult, String handleNote, Timestamp handledAt) throws SQLException {
        String sql = "UPDATE t_reconciliation_item SET status = ?, handler_admin_user_id = ?, "
                + "handle_result = ?, handle_note = ?, handled_at = ? WHERE item_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, adminUserId);
            statement.setString(3, handleResult);
            statement.setString(4, handleNote);
            statement.setTimestamp(5, handledAt);
            statement.setLong(6, itemId);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertActionLog(Connection connection, ReconciliationActionLog actionLog) throws SQLException {
        String sql = "INSERT INTO t_reconciliation_action_log (item_id, admin_user_id, action_type, "
                + "before_status, after_status, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actionLog.getItemId());
            statement.setLong(2, actionLog.getAdminUserId());
            statement.setString(3, actionLog.getActionType());
            statement.setString(4, actionLog.getBeforeStatus());
            statement.setString(5, actionLog.getAfterStatus());
            statement.setString(6, actionLog.getNote());
            statement.executeUpdate();
        }
    }

    @Override
    public List<ReconciliationActionLog> findActionLogsByItem(Connection connection, long itemId)
            throws SQLException {
        String sql = "SELECT l.action_id, l.item_id, l.admin_user_id, u.username AS admin_username, "
                + "l.action_type, l.before_status, l.after_status, l.note, l.created_at "
                + "FROM t_reconciliation_action_log l "
                + "LEFT JOIN t_user u ON l.admin_user_id = u.user_id "
                + "WHERE l.item_id = ? ORDER BY l.created_at DESC, l.action_id DESC";
        List<ReconciliationActionLog> logs = new ArrayList<ReconciliationActionLog>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapActionLog(resultSet));
                }
            }
        }
        return logs;
    }

    private String itemSelectSql() {
        return "SELECT i.item_id, i.batch_id, b.recon_date, i.check_type, i.severity, "
                + "i.business_type, i.business_id, i.expected_value, i.actual_value, i.description, "
                + "i.status, i.handler_admin_user_id, u.username AS handler_username, "
                + "i.handle_result, i.handle_note, i.handled_at, i.created_at, i.updated_at "
                + "FROM t_reconciliation_item i "
                + "JOIN t_reconciliation_batch b ON i.batch_id = b.batch_id "
                + "LEFT JOIN t_user u ON i.handler_admin_user_id = u.user_id ";
    }

    private ReconciliationBatch mapBatch(ResultSet resultSet) throws SQLException {
        ReconciliationBatch batch = new ReconciliationBatch();
        batch.setBatchId(resultSet.getLong("batch_id"));
        batch.setReconDate(resultSet.getDate("recon_date"));
        batch.setStatus(resultSet.getString("status"));
        batch.setTotalChecks(resultSet.getInt("total_checks"));
        batch.setExceptionCount(resultSet.getInt("exception_count"));
        batch.setCreatedByAdminUserId(resultSet.getLong("created_by_admin_user_id"));
        batch.setCreatedByUsername(resultSet.getString("created_by_username"));
        batch.setStartedAt(resultSet.getTimestamp("started_at"));
        batch.setFinishedAt(resultSet.getTimestamp("finished_at"));
        batch.setCreatedAt(resultSet.getTimestamp("created_at"));
        return batch;
    }

    private ReconciliationItem mapItem(ResultSet resultSet) throws SQLException {
        ReconciliationItem item = new ReconciliationItem();
        item.setItemId(resultSet.getLong("item_id"));
        item.setBatchId(resultSet.getLong("batch_id"));
        item.setReconDate(resultSet.getDate("recon_date"));
        item.setCheckType(resultSet.getString("check_type"));
        item.setSeverity(resultSet.getString("severity"));
        item.setBusinessType(resultSet.getString("business_type"));
        item.setBusinessId(resultSet.getString("business_id"));
        item.setExpectedValue(resultSet.getString("expected_value"));
        item.setActualValue(resultSet.getString("actual_value"));
        item.setDescription(resultSet.getString("description"));
        item.setStatus(resultSet.getString("status"));
        long handlerAdminUserId = resultSet.getLong("handler_admin_user_id");
        item.setHandlerAdminUserId(resultSet.wasNull() ? null : handlerAdminUserId);
        item.setHandlerUsername(resultSet.getString("handler_username"));
        item.setHandleResult(resultSet.getString("handle_result"));
        item.setHandleNote(resultSet.getString("handle_note"));
        item.setHandledAt(resultSet.getTimestamp("handled_at"));
        item.setCreatedAt(resultSet.getTimestamp("created_at"));
        item.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return item;
    }

    private ReconciliationActionLog mapActionLog(ResultSet resultSet) throws SQLException {
        ReconciliationActionLog actionLog = new ReconciliationActionLog();
        actionLog.setActionId(resultSet.getLong("action_id"));
        actionLog.setItemId(resultSet.getLong("item_id"));
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
