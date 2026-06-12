package com.bank.service.impl;

import com.bank.bean.ServiceTicket;
import com.bank.bean.TicketActionLog;
import com.bank.bean.TicketReply;
import com.bank.dao.AdjustmentDao;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.TicketDao;
import com.bank.dao.impl.AdjustmentDaoImpl;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.TicketDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.dto.TicketDetail;
import com.bank.service.AdminAlertService;
import com.bank.service.NotificationService;
import com.bank.service.PermissionService;
import com.bank.service.TicketService;
import com.bank.util.DBUtil;
import com.bank.util.TicketNoGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

public class TicketServiceImpl implements TicketService {
    private static final int CUSTOMER_TICKET_LIMIT = 100;
    private static final int ADMIN_TICKET_LIMIT = 200;
    private static final int TICKET_ADJUSTMENT_LIMIT = 20;

    private final AdjustmentDao adjustmentDao = new AdjustmentDaoImpl();
    private final TicketDao ticketDao = new TicketDaoImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final PermissionService permissionService = new PermissionServiceImpl();

    @Override
    public ServiceResult<ServiceTicket> createTicket(long customerId, long userId, String ticketType, String priority,
                                                     String title, String description, String relatedBusinessType,
                                                     String relatedBusinessId) {
        String normalizedType = normalizeTicketType(ticketType);
        String normalizedPriority = normalizePriority(priority);
        String normalizedTitle;
        String normalizedDescription;
        try {
            normalizedTitle = normalizeRequired(title, 120, "请填写工单标题。", 4);
            normalizedDescription = normalizeRequired(description, 1000, "请填写问题描述。", 8);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (normalizedType == null) {
            return ServiceResult.failure("请选择正确的问题类型。");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ServiceTicket ticket = new ServiceTicket();
            ticket.setTicketNo(TicketNoGenerator.generate());
            ticket.setCustomerId(customerId);
            ticket.setUserId(userId);
            ticket.setTicketType(normalizedType);
            ticket.setPriority(normalizedPriority);
            ticket.setStatus("SUBMITTED");
            ticket.setTitle(normalizedTitle);
            ticket.setDescription(normalizedDescription);
            ticket.setRelatedBusinessType(trimToLength(relatedBusinessType, 50, null));
            ticket.setRelatedBusinessId(trimToLength(relatedBusinessId, 64, null));
            ticket.setAssignedRoleCode(roleForType(normalizedType));
            long ticketId = ticketDao.insertTicket(connection, ticket);

            insertReply(connection, ticketId, "CUSTOMER", userId, normalizedDescription);
            insertActionLog(connection, ticketId, null, "CREATE", null, "SUBMITTED", "客户提交工单");
            notificationService.create(connection, customerId, userId, "SERVICE", "工单已提交",
                    "您的服务工单 " + ticket.getTicketNo() + " 已提交，后台将尽快处理。",
                    "SERVICE_TICKET", ticket.getTicketNo());
            adminAlertService.createOrReuse(connection, "TICKET_NEW", alertSeverity(normalizedPriority),
                    "新客户服务工单",
                    "客户提交了 " + typeLabel(normalizedType) + " 工单：" + normalizedTitle,
                    "SERVICE_TICKET", String.valueOf(ticketId), ticket.getAssignedRoleCode());

            connection.commit();
            ServiceTicket created = ticketDao.findById(connection, ticketId);
            return ServiceResult.success("工单已提交。", created);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("工单创建失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<ServiceTicket>> listCustomerTickets(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("工单查询成功。",
                    ticketDao.findByCustomer(connection, customerId, CUSTOMER_TICKET_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("工单查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<TicketDetail> getCustomerTicket(long customerId, long ticketId) {
        if (ticketId <= 0) {
            return ServiceResult.failure("工单不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            ServiceTicket ticket = ticketDao.findById(connection, ticketId);
            if (ticket == null || !Long.valueOf(customerId).equals(ticket.getCustomerId())) {
                return ServiceResult.failure("工单不存在或不属于当前客户。");
            }
            return ServiceResult.success("工单详情查询成功。", buildDetail(connection, ticket));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("工单详情查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> addCustomerReply(long customerId, long userId, long ticketId, String content) {
        String normalizedContent;
        try {
            normalizedContent = normalizeRequired(content, 1000, "请填写补充说明。", 4);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, ticketId);
            if (ticket == null || !Long.valueOf(customerId).equals(ticket.getCustomerId())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("工单不存在或不属于当前客户。");
            }
            if ("CLOSED".equals(ticket.getStatus()) || "REJECTED".equals(ticket.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("已关闭或不予受理的工单不能继续补充。");
            }

            insertReply(connection, ticketId, "CUSTOMER", userId, normalizedContent);
            if ("WAITING_CUSTOMER".equals(ticket.getStatus())) {
                updateTicket(connection, ticket, "REOPENED", ticket.getAssignedRoleCode(), null);
                insertActionLog(connection, ticketId, null, "CUSTOMER_REPLY", ticket.getStatus(), "REOPENED",
                        "客户已补充材料");
                adminAlertService.createOrReuse(connection, "TICKET_NEW", alertSeverity(ticket.getPriority()),
                        "客户已补充工单材料", "客户已补充工单 " + ticket.getTicketNo() + "，请继续处理。",
                        "SERVICE_TICKET", String.valueOf(ticketId), ticket.getAssignedRoleCode());
            } else {
                insertActionLog(connection, ticketId, null, "CUSTOMER_REPLY", ticket.getStatus(), ticket.getStatus(),
                        "客户追加说明");
                adminAlertService.createOrReuse(connection, "TICKET_FOLLOW_UP", alertSeverity(ticket.getPriority()),
                        "客户追加了工单说明",
                        "客户在工单 " + ticket.getTicketNo() + " 追加了说明，请继续跟进。",
                        "SERVICE_TICKET", String.valueOf(ticketId), ticket.getAssignedRoleCode());
            }

            connection.commit();
            return ServiceResult.success("补充说明已提交。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("工单补充失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<Void> closeByCustomer(long customerId, long userId, long ticketId, String note) {
        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, ticketId);
            if (ticket == null || !Long.valueOf(customerId).equals(ticket.getCustomerId())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("工单不存在或不属于当前客户。");
            }
            if (!"RESOLVED".equals(ticket.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("只有已处理完成的工单可以确认关闭。");
            }
            String normalizedNote = trimToLength(note, 500, "客户确认问题已解决");
            updateTicket(connection, ticket, "CLOSED", ticket.getAssignedRoleCode(), ticket.getAssignedAdminUserId());
            insertActionLog(connection, ticketId, null, "CUSTOMER_CLOSE", "RESOLVED", "CLOSED", normalizedNote);

            connection.commit();
            return ServiceResult.success("工单已关闭。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("工单关闭失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<Void> reopenByCustomer(long customerId, long userId, long ticketId, String note) {
        String normalizedNote;
        try {
            normalizedNote = normalizeRequired(note, 500, "请填写重新打开原因。", 5);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, ticketId);
            if (ticket == null || !Long.valueOf(customerId).equals(ticket.getCustomerId())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("工单不存在或不属于当前客户。");
            }
            if (!"RESOLVED".equals(ticket.getStatus()) && !"CLOSED".equals(ticket.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("只有已解决或已关闭的工单可以重新打开。");
            }
            String beforeStatus = ticket.getStatus();
            updateTicket(connection, ticket, "REOPENED", ticket.getAssignedRoleCode(), null);
            insertReply(connection, ticketId, "CUSTOMER", userId, normalizedNote);
            insertActionLog(connection, ticketId, null, "CUSTOMER_REOPEN", beforeStatus, "REOPENED", normalizedNote);
            adminAlertService.createOrReuse(connection, "TICKET_NEW", alertSeverity(ticket.getPriority()),
                    "客户重新打开工单", "客户重新打开工单 " + ticket.getTicketNo() + "，原因：" + normalizedNote,
                    "SERVICE_TICKET", String.valueOf(ticketId), ticket.getAssignedRoleCode());

            connection.commit();
            return ServiceResult.success("工单已重新打开。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("工单重新打开失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<ServiceTicket>> listAdminTickets(long adminUserId, String status, String ticketType,
                                                               String priority, boolean assignedToMe,
                                                               String keyword) {
        String normalizedStatus = normalizeStatusFilter(status);
        String normalizedType = normalizeTicketType(ticketType);
        String normalizedPriority = normalizePriorityFilter(priority);
        try (Connection connection = DBUtil.getConnection()) {
            Set<String> roles = permissionService.rolesFor(adminUserId);
            Set<String> permissions = permissionService.permissionsFor(adminUserId);
            boolean viewAll = permissions.contains("TICKET_ALL_VIEW") || roles.contains("SUPER_ADMIN")
                    || roles.contains("AUDITOR");
            return ServiceResult.success("后台工单查询成功。",
                    ticketDao.findForAdmin(connection, roles, adminUserId, viewAll, normalizedStatus,
                            normalizedType, normalizedPriority, assignedToMe, trimToLength(keyword, 80, null),
                            ADMIN_TICKET_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("后台工单查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<TicketDetail> getAdminTicket(long adminUserId, long ticketId) {
        if (ticketId <= 0) {
            return ServiceResult.failure("工单不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            ServiceTicket ticket = ticketDao.findById(connection, ticketId);
            if (ticket == null || !canAdminView(adminUserId, ticket)) {
                return ServiceResult.failure("工单不存在或无权查看。");
            }
            return ServiceResult.success("后台工单详情查询成功。", buildDetail(connection, ticket));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("后台工单详情查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> handleAdminAction(long adminUserId, long ticketId, String action,
                                                 String assignedRoleCode, String replyContent, String note,
                                                 String ipAddress) {
        String normalizedAction = normalizeAdminAction(action);
        if (normalizedAction == null) {
            return ServiceResult.failure("请选择正确的工单处理动作。");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, ticketId);
            if (ticket == null || !canAdminView(adminUserId, ticket)) {
                rollbackQuietly(connection);
                return ServiceResult.failure("工单不存在或无权处理。");
            }
            if (isTerminal(ticket.getStatus()) && !"ASSIGN".equals(normalizedAction)) {
                rollbackQuietly(connection);
                return ServiceResult.failure("已关闭或不予受理的工单不能继续处理。");
            }

            ServiceResult<Void> result = applyAdminAction(connection, ticket, adminUserId, normalizedAction,
                    assignedRoleCode, replyContent, note, ipAddress);
            if (!result.isSuccess()) {
                rollbackQuietly(connection);
                return result;
            }
            connection.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("工单处理失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    private ServiceResult<Void> applyAdminAction(Connection connection, ServiceTicket ticket, long adminUserId,
                                                String action, String assignedRoleCode, String replyContent,
                                                String note, String ipAddress) throws SQLException {
        String beforeStatus = ticket.getStatus();
        String targetStatus = targetStatusFor(action, beforeStatus);
        String targetRole = "ASSIGN".equals(action) ? normalizeAssignableRole(assignedRoleCode)
                : ticket.getAssignedRoleCode();
        if ("ASSIGN".equals(action) && targetRole == null) {
            return ServiceResult.failure("请选择正确的转派角色。");
        }
        String normalizedNote = trimToLength(note, 500, actionLabel(action));
        String normalizedReply = trimToLength(replyContent, 1000, null);
        if (requiresReply(action) && normalizedReply == null) {
            return ServiceResult.failure("该处理动作需要填写给客户的回复内容。");
        }

        Long assignedAdmin = ("ACCEPT".equals(action) || "INVESTIGATE".equals(action))
                ? Long.valueOf(adminUserId) : ticket.getAssignedAdminUserId();
        if ("ASSIGN".equals(action)) {
            assignedAdmin = null;
        }
        updateTicket(connection, ticket, targetStatus, targetRole, assignedAdmin);

        if (normalizedReply != null) {
            insertReply(connection, ticket.getTicketId(), "ADMIN", adminUserId, normalizedReply);
        }
        insertActionLog(connection, ticket.getTicketId(), Long.valueOf(adminUserId), action, beforeStatus,
                targetStatus, normalizedNote);
        adminAuditLogDao.insert(connection, adminUserId, "HANDLE_SERVICE_TICKET", "SERVICE_TICKET",
                String.valueOf(ticket.getTicketId()), "处理服务工单 " + ticket.getTicketNo()
                        + "，动作：" + action + "，状态：" + beforeStatus + " -> " + targetStatus,
                ipAddress);

        syncAlertForAdminAction(connection, ticket, adminUserId, action, targetStatus, targetRole);
        notifyCustomerForAdminAction(connection, ticket, adminUserId, action, targetStatus, normalizedReply);
        return ServiceResult.success("工单处理成功。", null);
    }

    private void syncAlertForAdminAction(Connection connection, ServiceTicket ticket, long adminUserId,
                                         String action, String targetStatus, String targetRole) throws SQLException {
        String targetId = String.valueOf(ticket.getTicketId());
        if ("WAIT_CUSTOMER".equals(action) || "RESOLVE".equals(action) || "REJECT".equals(action)
                || "CLOSE".equals(action)) {
            adminAlertService.resolveByTarget(connection, "TICKET_NEW", "SERVICE_TICKET", targetId,
                    adminUserId, "工单已流转为 " + targetStatus);
            adminAlertService.resolveByTarget(connection, "TICKET_FOLLOW_UP", "SERVICE_TICKET", targetId,
                    adminUserId, "工单已流转为 " + targetStatus);
            return;
        }
        if ("ASSIGN".equals(action)) {
            adminAlertService.closeByTarget(connection, "TICKET_NEW", "SERVICE_TICKET", targetId,
                    adminUserId, "工单已转派给 " + targetRole);
            adminAlertService.closeByTarget(connection, "TICKET_FOLLOW_UP", "SERVICE_TICKET", targetId,
                    adminUserId, "工单已转派给 " + targetRole);
            adminAlertService.createOrReuse(connection, "TICKET_NEW", alertSeverity(ticket.getPriority()),
                    "服务工单已转派", "工单 " + ticket.getTicketNo() + " 已转派，请继续处理。",
                    "SERVICE_TICKET", targetId, targetRole);
            return;
        }
        adminAlertService.ackByTarget(connection, "TICKET_NEW", "SERVICE_TICKET", targetId,
                adminUserId, "工单已进入 " + targetStatus);
        adminAlertService.ackByTarget(connection, "TICKET_FOLLOW_UP", "SERVICE_TICKET", targetId,
                adminUserId, "工单已进入 " + targetStatus);
    }

    private void notifyCustomerForAdminAction(Connection connection, ServiceTicket ticket, long adminUserId,
                                              String action, String targetStatus, String replyContent)
            throws SQLException {
        String title;
        if ("RESOLVE".equals(action)) {
            title = "工单已处理";
        } else if ("WAIT_CUSTOMER".equals(action)) {
            title = "工单需要补充材料";
        } else if ("REJECT".equals(action)) {
            title = "工单不予受理";
        } else if ("CLOSE".equals(action)) {
            title = "工单已关闭";
        } else {
            title = "工单状态已更新";
        }
        String content = "您的工单 " + ticket.getTicketNo() + " 状态已更新为 " + statusLabel(targetStatus) + "。";
        if (replyContent != null && replyContent.length() > 0) {
            content = content + " 回复：" + replyContent;
        }
        notificationService.create(connection, ticket.getCustomerId(), ticket.getUserId(), "SERVICE",
                title, trimToLength(content, 500, content), "SERVICE_TICKET", ticket.getTicketNo());
    }

    private TicketDetail buildDetail(Connection connection, ServiceTicket ticket) throws SQLException {
        TicketDetail detail = new TicketDetail();
        detail.setTicket(ticket);
        detail.setAdjustmentRequests(adjustmentDao.findBySourceTicket(connection, ticket.getTicketId(),
                TICKET_ADJUSTMENT_LIMIT));
        detail.setReplies(ticketDao.findReplies(connection, ticket.getTicketId()));
        detail.setActionLogs(ticketDao.findActionLogs(connection, ticket.getTicketId()));
        return detail;
    }

    private boolean canAdminView(long adminUserId, ServiceTicket ticket) {
        Set<String> roles = permissionService.rolesFor(adminUserId);
        Set<String> permissions = permissionService.permissionsFor(adminUserId);
        if (permissions.contains("TICKET_ALL_VIEW") || roles.contains("SUPER_ADMIN") || roles.contains("AUDITOR")) {
            return true;
        }
        if (ticket.getAssignedAdminUserId() != null && Long.valueOf(adminUserId).equals(ticket.getAssignedAdminUserId())) {
            return true;
        }
        return ticket.getAssignedRoleCode() != null && roles.contains(ticket.getAssignedRoleCode());
    }

    private void updateTicket(Connection connection, ServiceTicket ticket, String status, String roleCode,
                              Long assignedAdminUserId) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp acceptedAt = ticket.getAcceptedAt();
        Timestamp resolvedAt = ticket.getResolvedAt();
        Timestamp closedAt = ticket.getClosedAt();
        if (("ACCEPTED".equals(status) || "INVESTIGATING".equals(status)) && acceptedAt == null) {
            acceptedAt = now;
        }
        if ("RESOLVED".equals(status)) {
            resolvedAt = now;
            closedAt = null;
        } else if ("CLOSED".equals(status) || "REJECTED".equals(status)) {
            closedAt = now;
        } else if ("REOPENED".equals(status) || "SUBMITTED".equals(status)) {
            resolvedAt = null;
            closedAt = null;
        }
        ticketDao.updateTicketState(connection, ticket.getTicketId(), status, roleCode, assignedAdminUserId,
                acceptedAt, resolvedAt, closedAt);
    }

    private void insertReply(Connection connection, long ticketId, String senderType, Long senderUserId,
                             String content) throws SQLException {
        TicketReply reply = new TicketReply();
        reply.setTicketId(ticketId);
        reply.setSenderType(senderType);
        reply.setSenderUserId(senderUserId);
        reply.setContent(content);
        ticketDao.insertReply(connection, reply);
    }

    private void insertActionLog(Connection connection, long ticketId, Long adminUserId, String actionType,
                                 String beforeStatus, String afterStatus, String note) throws SQLException {
        TicketActionLog log = new TicketActionLog();
        log.setTicketId(ticketId);
        log.setAdminUserId(adminUserId);
        log.setActionType(actionType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setNote(trimToLength(note, 500, ""));
        ticketDao.insertActionLog(connection, log);
    }

    private String normalizeTicketType(String type) {
        if ("TRANSACTION_DISPUTE".equals(type) || "RISK_APPEAL".equals(type)
                || "ACCOUNT_SERVICE".equals(type) || "WEALTH_SERVICE".equals(type)
                || "ADJUSTMENT_INQUIRY".equals(type) || "GENERAL".equals(type)) {
            return type;
        }
        return null;
    }

    private String normalizePriority(String priority) {
        if ("LOW".equals(priority) || "NORMAL".equals(priority)
                || "HIGH".equals(priority) || "URGENT".equals(priority)) {
            return priority;
        }
        return "NORMAL";
    }

    private String normalizePriorityFilter(String priority) {
        if (priority == null || priority.length() == 0 || "ALL".equals(priority)) {
            return null;
        }
        return normalizePriority(priority);
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.length() == 0 || "ALL".equals(status)) {
            return null;
        }
        return isValidStatus(status) ? status : null;
    }

    private String normalizeAdminAction(String action) {
        if ("ACCEPT".equals(action) || "INVESTIGATE".equals(action) || "WAIT_CUSTOMER".equals(action)
                || "RESOLVE".equals(action) || "REJECT".equals(action) || "CLOSE".equals(action)
                || "ASSIGN".equals(action)) {
            return action;
        }
        return null;
    }

    private String normalizeAssignableRole(String roleCode) {
        if ("CUSTOMER_OPERATOR".equals(roleCode) || "RISK_OPERATOR".equals(roleCode)
                || "ACCOUNTING_OPERATOR".equals(roleCode) || "ACCOUNTING_REVIEWER".equals(roleCode)
                || "PRODUCT_MANAGER".equals(roleCode)) {
            return roleCode;
        }
        return null;
    }

    private String targetStatusFor(String action, String beforeStatus) {
        if ("ACCEPT".equals(action)) return "ACCEPTED";
        if ("INVESTIGATE".equals(action)) return "INVESTIGATING";
        if ("WAIT_CUSTOMER".equals(action)) return "WAITING_CUSTOMER";
        if ("RESOLVE".equals(action)) return "RESOLVED";
        if ("REJECT".equals(action)) return "REJECTED";
        if ("CLOSE".equals(action)) return "CLOSED";
        return beforeStatus;
    }

    private boolean requiresReply(String action) {
        return "WAIT_CUSTOMER".equals(action) || "RESOLVE".equals(action) || "REJECT".equals(action);
    }

    private boolean isTerminal(String status) {
        return "CLOSED".equals(status) || "REJECTED".equals(status);
    }

    private boolean isValidStatus(String status) {
        return "SUBMITTED".equals(status) || "ACCEPTED".equals(status) || "INVESTIGATING".equals(status)
                || "WAITING_CUSTOMER".equals(status) || "RESOLVED".equals(status) || "CLOSED".equals(status)
                || "REOPENED".equals(status) || "REJECTED".equals(status);
    }

    private String roleForType(String ticketType) {
        if ("TRANSACTION_DISPUTE".equals(ticketType)) return "ACCOUNTING_OPERATOR";
        if ("RISK_APPEAL".equals(ticketType)) return "RISK_OPERATOR";
        if ("WEALTH_SERVICE".equals(ticketType)) return "PRODUCT_MANAGER";
        if ("ADJUSTMENT_INQUIRY".equals(ticketType)) return "ACCOUNTING_REVIEWER";
        return "CUSTOMER_OPERATOR";
    }

    private String alertSeverity(String priority) {
        if ("URGENT".equals(priority)) return "CRITICAL";
        if ("HIGH".equals(priority)) return "HIGH";
        return "WARNING";
    }

    private String typeLabel(String type) {
        if ("TRANSACTION_DISPUTE".equals(type)) return "交易争议";
        if ("RISK_APPEAL".equals(type)) return "风控申诉";
        if ("ACCOUNT_SERVICE".equals(type)) return "账户服务";
        if ("WEALTH_SERVICE".equals(type)) return "理财服务";
        if ("ADJUSTMENT_INQUIRY".equals(type)) return "调账咨询";
        return "其他问题";
    }

    private String statusLabel(String status) {
        if ("SUBMITTED".equals(status)) return "已提交";
        if ("ACCEPTED".equals(status)) return "已受理";
        if ("INVESTIGATING".equals(status)) return "调查中";
        if ("WAITING_CUSTOMER".equals(status)) return "等待客户补充";
        if ("RESOLVED".equals(status)) return "已处理";
        if ("CLOSED".equals(status)) return "已关闭";
        if ("REOPENED".equals(status)) return "已重新打开";
        if ("REJECTED".equals(status)) return "不予受理";
        return status;
    }

    private String actionLabel(String action) {
        if ("ACCEPT".equals(action)) return "后台已受理";
        if ("INVESTIGATE".equals(action)) return "进入调查处理";
        if ("WAIT_CUSTOMER".equals(action)) return "等待客户补充材料";
        if ("RESOLVE".equals(action)) return "后台已给出处理结果";
        if ("REJECT".equals(action)) return "后台不予受理";
        if ("CLOSE".equals(action)) return "后台关闭工单";
        if ("ASSIGN".equals(action)) return "后台转派工单";
        return action;
    }

    private String normalizeRequired(String value, int maxLength, String emptyMessage, int minLength) {
        String text = value == null ? "" : value.trim();
        if (text.length() < minLength) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String trimToLength(String value, int maxLength, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return defaultValue;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
