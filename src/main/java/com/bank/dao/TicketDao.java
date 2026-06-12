package com.bank.dao;

import com.bank.bean.ServiceTicket;
import com.bank.bean.TicketActionLog;
import com.bank.bean.TicketReply;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

public interface TicketDao {
    long insertTicket(Connection connection, ServiceTicket ticket) throws SQLException;

    ServiceTicket findById(Connection connection, long ticketId) throws SQLException;

    ServiceTicket findByIdForUpdate(Connection connection, long ticketId) throws SQLException;

    List<ServiceTicket> findByCustomer(Connection connection, long customerId, int limit) throws SQLException;

    List<ServiceTicket> findForAdmin(Connection connection, Set<String> roleCodes, long adminUserId,
                                     boolean viewAll, String status, String ticketType, String priority,
                                     boolean assignedToMe, String keyword, int limit) throws SQLException;

    void updateTicketState(Connection connection, long ticketId, String status, String assignedRoleCode,
                           Long assignedAdminUserId, Timestamp acceptedAt, Timestamp resolvedAt,
                           Timestamp closedAt) throws SQLException;

    void insertReply(Connection connection, TicketReply reply) throws SQLException;

    List<TicketReply> findReplies(Connection connection, long ticketId) throws SQLException;

    void insertActionLog(Connection connection, TicketActionLog actionLog) throws SQLException;

    List<TicketActionLog> findActionLogs(Connection connection, long ticketId) throws SQLException;
}
