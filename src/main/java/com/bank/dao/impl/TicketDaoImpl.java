package com.bank.dao.impl;

import com.bank.bean.ServiceTicket;
import com.bank.bean.TicketActionLog;
import com.bank.bean.TicketReply;
import com.bank.dao.TicketDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TicketDaoImpl implements TicketDao {
    @Override
    public long insertTicket(Connection connection, ServiceTicket ticket) throws SQLException {
        String sql = "INSERT INTO t_service_ticket (ticket_no, customer_id, user_id, ticket_type, priority, "
                + "status, title, description, related_business_type, related_business_id, assigned_role_code) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, ticket.getTicketNo());
            statement.setLong(2, ticket.getCustomerId());
            statement.setLong(3, ticket.getUserId());
            statement.setString(4, ticket.getTicketType());
            statement.setString(5, ticket.getPriority());
            statement.setString(6, ticket.getStatus());
            statement.setString(7, ticket.getTitle());
            statement.setString(8, ticket.getDescription());
            statement.setString(9, ticket.getRelatedBusinessType());
            statement.setString(10, ticket.getRelatedBusinessId());
            statement.setString(11, ticket.getAssignedRoleCode());
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
    public ServiceTicket findById(Connection connection, long ticketId) throws SQLException {
        return findById(connection, ticketId, false);
    }

    @Override
    public ServiceTicket findByIdForUpdate(Connection connection, long ticketId) throws SQLException {
        return findById(connection, ticketId, true);
    }

    @Override
    public List<ServiceTicket> findByCustomer(Connection connection, long customerId, int limit)
            throws SQLException {
        String sql = selectTicketSql() + " WHERE t.customer_id = ? "
                + "ORDER BY t.updated_at DESC, t.ticket_id DESC LIMIT ?";
        List<ServiceTicket> tickets = new ArrayList<ServiceTicket>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tickets.add(mapTicket(resultSet));
                }
            }
        }
        return tickets;
    }

    @Override
    public List<ServiceTicket> findForAdmin(Connection connection, Set<String> roleCodes, long adminUserId,
                                            boolean viewAll, String status, String ticketType, String priority,
                                            boolean assignedToMe, String keyword, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(selectTicketSql());
        List<Object> params = new ArrayList<Object>();
        sql.append(" WHERE 1 = 1 ");
        if (!viewAll) {
            appendVisibility(sql, params, roleCodes, adminUserId);
        }
        if (assignedToMe) {
            sql.append("AND t.assigned_admin_user_id = ? ");
            params.add(Long.valueOf(adminUserId));
        }
        if (status != null && status.length() > 0) {
            sql.append("AND t.status = ? ");
            params.add(status);
        }
        if (ticketType != null && ticketType.length() > 0) {
            sql.append("AND t.ticket_type = ? ");
            params.add(ticketType);
        }
        if (priority != null && priority.length() > 0) {
            sql.append("AND t.priority = ? ");
            params.add(priority);
        }
        if (keyword != null && keyword.length() > 0) {
            sql.append("AND (t.ticket_no LIKE ? OR t.title LIKE ? OR c.full_name LIKE ? OR c.phone LIKE ? ")
                    .append("OR t.related_business_id LIKE ?) ");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append("ORDER BY FIELD(t.status, 'SUBMITTED', 'REOPENED', 'ACCEPTED', 'INVESTIGATING', ")
                .append("'WAITING_CUSTOMER', 'RESOLVED', 'REJECTED', 'CLOSED'), ")
                .append("FIELD(t.priority, 'URGENT', 'HIGH', 'NORMAL', 'LOW'), ")
                .append("t.updated_at DESC, t.ticket_id DESC LIMIT ?");
        params.add(Integer.valueOf(limit));

        List<ServiceTicket> tickets = new ArrayList<ServiceTicket>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tickets.add(mapTicket(resultSet));
                }
            }
        }
        return tickets;
    }

    @Override
    public void updateTicketState(Connection connection, long ticketId, String status, String assignedRoleCode,
                                  Long assignedAdminUserId, Timestamp acceptedAt, Timestamp resolvedAt,
                                  Timestamp closedAt) throws SQLException {
        String sql = "UPDATE t_service_ticket SET status = ?, assigned_role_code = ?, "
                + "assigned_admin_user_id = ?, accepted_at = COALESCE(accepted_at, ?), "
                + "resolved_at = ?, closed_at = ?, updated_at = NOW() WHERE ticket_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, assignedRoleCode);
            if (assignedAdminUserId == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, assignedAdminUserId);
            }
            statement.setTimestamp(4, acceptedAt);
            statement.setTimestamp(5, resolvedAt);
            statement.setTimestamp(6, closedAt);
            statement.setLong(7, ticketId);
            statement.executeUpdate();
        }
    }

    @Override
    public void insertReply(Connection connection, TicketReply reply) throws SQLException {
        String sql = "INSERT INTO t_ticket_reply (ticket_id, sender_type, sender_user_id, content) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reply.getTicketId());
            statement.setString(2, reply.getSenderType());
            if (reply.getSenderUserId() == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, reply.getSenderUserId());
            }
            statement.setString(4, reply.getContent());
            statement.executeUpdate();
        }
    }

    @Override
    public List<TicketReply> findReplies(Connection connection, long ticketId) throws SQLException {
        String sql = "SELECT r.reply_id, r.ticket_id, r.sender_type, r.sender_user_id, u.username AS sender_username, "
                + "r.content, r.created_at FROM t_ticket_reply r "
                + "LEFT JOIN t_user u ON r.sender_user_id = u.user_id "
                + "WHERE r.ticket_id = ? ORDER BY r.created_at ASC, r.reply_id ASC";
        List<TicketReply> replies = new ArrayList<TicketReply>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    replies.add(mapReply(resultSet));
                }
            }
        }
        return replies;
    }

    @Override
    public void insertActionLog(Connection connection, TicketActionLog actionLog) throws SQLException {
        String sql = "INSERT INTO t_ticket_action_log (ticket_id, admin_user_id, action_type, before_status, "
                + "after_status, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actionLog.getTicketId());
            if (actionLog.getAdminUserId() == null) {
                statement.setNull(2, Types.BIGINT);
            } else {
                statement.setLong(2, actionLog.getAdminUserId());
            }
            statement.setString(3, actionLog.getActionType());
            statement.setString(4, actionLog.getBeforeStatus());
            statement.setString(5, actionLog.getAfterStatus());
            statement.setString(6, actionLog.getNote());
            statement.executeUpdate();
        }
    }

    @Override
    public List<TicketActionLog> findActionLogs(Connection connection, long ticketId) throws SQLException {
        String sql = "SELECT l.log_id, l.ticket_id, l.admin_user_id, u.username AS admin_username, "
                + "l.action_type, l.before_status, l.after_status, l.note, l.created_at "
                + "FROM t_ticket_action_log l LEFT JOIN t_user u ON l.admin_user_id = u.user_id "
                + "WHERE l.ticket_id = ? ORDER BY l.created_at DESC, l.log_id DESC";
        List<TicketActionLog> logs = new ArrayList<TicketActionLog>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapActionLog(resultSet));
                }
            }
        }
        return logs;
    }

    private ServiceTicket findById(Connection connection, long ticketId, boolean forUpdate) throws SQLException {
        String sql = selectTicketSql() + " WHERE t.ticket_id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTicket(resultSet);
                }
            }
        }
        return null;
    }

    private String selectTicketSql() {
        return "SELECT t.ticket_id, t.ticket_no, t.customer_id, t.user_id, c.full_name AS customer_name, "
                + "c.phone, t.ticket_type, t.priority, t.status, t.title, t.description, "
                + "t.related_business_type, t.related_business_id, t.assigned_role_code, "
                + "t.assigned_admin_user_id, au.username AS assigned_admin_username, "
                + "t.accepted_at, t.resolved_at, t.closed_at, t.created_at, t.updated_at "
                + "FROM t_service_ticket t "
                + "JOIN t_customer c ON t.customer_id = c.customer_id "
                + "LEFT JOIN t_user au ON t.assigned_admin_user_id = au.user_id";
    }

    private void appendVisibility(StringBuilder sql, List<Object> params, Set<String> roleCodes, long adminUserId) {
        sql.append("AND (t.assigned_admin_user_id = ? ");
        params.add(Long.valueOf(adminUserId));
        if (roleCodes != null && !roleCodes.isEmpty()) {
            sql.append("OR t.assigned_role_code IN (");
            int index = 0;
            for (String roleCode : roleCodes) {
                if (index++ > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                params.add(roleCode);
            }
            sql.append(") ");
        }
        sql.append(") ");
    }

    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Integer) {
                statement.setInt(i + 1, ((Integer) param).intValue());
            } else if (param instanceof Long) {
                statement.setLong(i + 1, ((Long) param).longValue());
            } else {
                statement.setString(i + 1, String.valueOf(param));
            }
        }
    }

    private ServiceTicket mapTicket(ResultSet resultSet) throws SQLException {
        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketId(resultSet.getLong("ticket_id"));
        ticket.setTicketNo(resultSet.getString("ticket_no"));
        ticket.setCustomerId(resultSet.getLong("customer_id"));
        ticket.setUserId(resultSet.getLong("user_id"));
        ticket.setCustomerName(resultSet.getString("customer_name"));
        ticket.setPhone(resultSet.getString("phone"));
        ticket.setTicketType(resultSet.getString("ticket_type"));
        ticket.setPriority(resultSet.getString("priority"));
        ticket.setStatus(resultSet.getString("status"));
        ticket.setTitle(resultSet.getString("title"));
        ticket.setDescription(resultSet.getString("description"));
        ticket.setRelatedBusinessType(resultSet.getString("related_business_type"));
        ticket.setRelatedBusinessId(resultSet.getString("related_business_id"));
        ticket.setAssignedRoleCode(resultSet.getString("assigned_role_code"));
        long assignedAdminUserId = resultSet.getLong("assigned_admin_user_id");
        ticket.setAssignedAdminUserId(resultSet.wasNull() ? null : Long.valueOf(assignedAdminUserId));
        ticket.setAssignedAdminUsername(resultSet.getString("assigned_admin_username"));
        ticket.setAcceptedAt(resultSet.getTimestamp("accepted_at"));
        ticket.setResolvedAt(resultSet.getTimestamp("resolved_at"));
        ticket.setClosedAt(resultSet.getTimestamp("closed_at"));
        ticket.setCreatedAt(resultSet.getTimestamp("created_at"));
        ticket.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return ticket;
    }

    private TicketReply mapReply(ResultSet resultSet) throws SQLException {
        TicketReply reply = new TicketReply();
        reply.setReplyId(resultSet.getLong("reply_id"));
        reply.setTicketId(resultSet.getLong("ticket_id"));
        reply.setSenderType(resultSet.getString("sender_type"));
        long senderUserId = resultSet.getLong("sender_user_id");
        reply.setSenderUserId(resultSet.wasNull() ? null : Long.valueOf(senderUserId));
        reply.setSenderUsername(resultSet.getString("sender_username"));
        reply.setContent(resultSet.getString("content"));
        reply.setCreatedAt(resultSet.getTimestamp("created_at"));
        return reply;
    }

    private TicketActionLog mapActionLog(ResultSet resultSet) throws SQLException {
        TicketActionLog log = new TicketActionLog();
        log.setLogId(resultSet.getLong("log_id"));
        log.setTicketId(resultSet.getLong("ticket_id"));
        long adminUserId = resultSet.getLong("admin_user_id");
        log.setAdminUserId(resultSet.wasNull() ? null : Long.valueOf(adminUserId));
        log.setAdminUsername(resultSet.getString("admin_username"));
        log.setActionType(resultSet.getString("action_type"));
        log.setBeforeStatus(resultSet.getString("before_status"));
        log.setAfterStatus(resultSet.getString("after_status"));
        log.setNote(resultSet.getString("note"));
        log.setCreatedAt(resultSet.getTimestamp("created_at"));
        return log;
    }
}
