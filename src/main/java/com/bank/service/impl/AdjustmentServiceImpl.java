package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.bean.AdjustmentActionLog;
import com.bank.bean.AdjustmentRequest;
import com.bank.bean.LedgerEntry;
import com.bank.bean.ReconciliationActionLog;
import com.bank.bean.ReconciliationItem;
import com.bank.bean.ServiceTicket;
import com.bank.bean.TicketActionLog;
import com.bank.bean.TicketReply;
import com.bank.bean.TransactionRecord;
import com.bank.dao.AccountDao;
import com.bank.dao.AdjustmentDao;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.LedgerDao;
import com.bank.dao.ReconciliationDao;
import com.bank.dao.TicketDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dao.impl.AdjustmentDaoImpl;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.LedgerDaoImpl;
import com.bank.dao.impl.ReconciliationDaoImpl;
import com.bank.dao.impl.TicketDaoImpl;
import com.bank.dao.impl.TransactionDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAlertService;
import com.bank.service.AdjustmentService;
import com.bank.service.NotificationService;
import com.bank.service.PermissionService;
import com.bank.util.AdjustmentNoGenerator;
import com.bank.util.DBUtil;
import com.bank.util.MoneyUtil;
import com.bank.util.TransactionNoGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

public class AdjustmentServiceImpl implements AdjustmentService {
    private static final int REQUEST_QUERY_LIMIT = 100;
    private static final BigDecimal MAX_ADJUSTMENT_AMOUNT = new BigDecimal("100000.00");

    private final AdjustmentDao adjustmentDao = new AdjustmentDaoImpl();
    private final ReconciliationDao reconciliationDao = new ReconciliationDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final LedgerDao ledgerDao = new LedgerDaoImpl();
    private final TicketDao ticketDao = new TicketDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();
    private final PermissionService permissionService = new PermissionServiceImpl();

    @Override
    public ServiceResult<AdjustmentRequest> createRequest(long itemId, long adminUserId, String accountNo,
                                                          String direction, String amountText, String reason,
                                                          String evidence, String ipAddress) {
        CreateInput input = normalizeCreateInput(accountNo, direction, amountText, reason, evidence);
        if (input.errorMessage != null) {
            return ServiceResult.failure(input.errorMessage);
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ReconciliationItem item = reconciliationDao.findItemById(connection, itemId);
            if (item == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("对账异常不存在。");
            }
            if (!"CONFIRMED_EXCEPTION".equals(item.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("只有已确认异常才能发起调账申请。");
            }

            Account account = accountDao.findByAccountNo(connection, input.accountNo);
            if (account == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户不存在。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户状态不可用。");
            }

            AdjustmentRequest request = buildRequest(adminUserId, account, input);
            request.setReconciliationItemId(Long.valueOf(itemId));
            request.setSourceType("RECONCILIATION_ITEM");
            long adjustmentId = adjustmentDao.insertRequest(connection, request);

            insertActionLog(connection, adjustmentId, adminUserId, "CREATE", null, "PENDING_REVIEW",
                    "创建调账申请：" + input.reason);
            adminAuditLogDao.insert(connection, adminUserId, "CREATE_ADJUSTMENT", "ADJUSTMENT_REQUEST",
                    String.valueOf(adjustmentId), "创建调账申请，账户：" + account.getAccountNo()
                            + "，方向：" + input.direction + "，金额：" + moneyText(input.amount)
                            + "，关联异常：" + itemId, ipAddress);
            createReviewAlert(connection, request.getAdjustmentNo(), adjustmentId, account.getAccountNo(),
                    input.direction, input.amount, "调账申请待复核");

            connection.commit();
            return ServiceResult.success("调账申请已提交，等待复核。",
                    adjustmentDao.findById(connection, adjustmentId));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("调账申请创建失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<AdjustmentRequest> createFromTicket(long ticketId, long adminUserId, String accountNo,
                                                            String direction, String amountText, String reason,
                                                            String evidence, String ipAddress) {
        CreateInput input = normalizeCreateInput(accountNo, direction, amountText, reason, evidence);
        if (input.errorMessage != null) {
            return ServiceResult.failure(input.errorMessage);
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, ticketId);
            if (ticket == null || !canAdminHandleTicket(adminUserId, ticket)) {
                rollbackQuietly(connection);
                return ServiceResult.failure("工单不存在或无权发起处置。");
            }
            if (!"TRANSACTION_DISPUTE".equals(ticket.getTicketType())
                    && !"ADJUSTMENT_INQUIRY".equals(ticket.getTicketType())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("只有交易争议或调账咨询工单可以直接发起调账申请。");
            }
            if ("CLOSED".equals(ticket.getStatus()) || "REJECTED".equals(ticket.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("已关闭或不予受理的工单不能发起调账申请。");
            }

            Account account = accountDao.findByAccountNo(connection, input.accountNo);
            if (account == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户不存在。");
            }
            if (!Long.valueOf(account.getCustomerId()).equals(ticket.getCustomerId())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户不属于该工单客户。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户状态不可用。");
            }

            AdjustmentRequest request = buildRequest(adminUserId, account, input);
            request.setSourceType("SERVICE_TICKET");
            request.setSourceTicketId(Long.valueOf(ticketId));
            long adjustmentId = adjustmentDao.insertRequest(connection, request);

            insertActionLog(connection, adjustmentId, adminUserId, "CREATE", null, "PENDING_REVIEW",
                    "由服务工单 " + ticket.getTicketNo() + " 发起调账申请：" + input.reason);
            String beforeStatus = ticket.getStatus();
            updateTicketState(connection, ticket, "INVESTIGATING", adminUserId);
            insertTicketActionLog(connection, ticketId, adminUserId, "CREATE_ADJUSTMENT",
                    beforeStatus, "INVESTIGATING",
                    "已发起调账申请 " + request.getAdjustmentNo() + "，金额 " + moneyText(input.amount));
            insertTicketReply(connection, ticketId, "ADMIN", adminUserId,
                    "已基于本工单发起调账申请 " + request.getAdjustmentNo()
                            + "，进入账务复核流程。处理完成后会同步结果。");

            adminAuditLogDao.insert(connection, adminUserId, "CREATE_ADJUSTMENT", "ADJUSTMENT_REQUEST",
                    String.valueOf(adjustmentId), "从服务工单发起调账申请，工单：" + ticket.getTicketNo()
                            + "，账户：" + account.getAccountNo() + "，方向：" + input.direction
                            + "，金额：" + moneyText(input.amount), ipAddress);
            adminAlertService.ackByTarget(connection, "TICKET_NEW", "SERVICE_TICKET",
                    String.valueOf(ticketId), adminUserId, "工单已发起调账申请");
            adminAlertService.ackByTarget(connection, "TICKET_FOLLOW_UP", "SERVICE_TICKET",
                    String.valueOf(ticketId), adminUserId, "工单已发起调账申请");
            createReviewAlert(connection, request.getAdjustmentNo(), adjustmentId, account.getAccountNo(),
                    input.direction, input.amount, "服务工单调账申请待复核");
            notificationService.create(connection, ticket.getCustomerId(), ticket.getUserId(), "SERVICE",
                    "工单已进入调账复核",
                    "您的工单 " + ticket.getTicketNo() + " 已进入调账复核流程，申请编号 "
                            + request.getAdjustmentNo() + "。",
                    "SERVICE_TICKET", ticket.getTicketNo());

            connection.commit();
            return ServiceResult.success("已从工单发起调账申请，等待复核。",
                    adjustmentDao.findById(connection, adjustmentId));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("从工单发起调账申请失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<AdjustmentRequest>> listRequests(String status) {
        String normalizedStatus;
        try {
            normalizedStatus = normalizeStatusFilter(status);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("调账申请查询成功。",
                    adjustmentDao.findRequests(connection, normalizedStatus, REQUEST_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("调账申请查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdjustmentRequest> getRequest(long adjustmentId) {
        if (adjustmentId <= 0) {
            return ServiceResult.failure("调账申请不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            AdjustmentRequest request = adjustmentDao.findById(connection, adjustmentId);
            if (request == null) {
                return ServiceResult.failure("调账申请不存在。");
            }
            return ServiceResult.success("调账申请查询成功。", request);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("调账申请查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<AdjustmentActionLog>> listActionLogs(long adjustmentId) {
        if (adjustmentId <= 0) {
            return ServiceResult.failure("调账申请不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("调账操作日志查询成功。",
                    adjustmentDao.findActionLogs(connection, adjustmentId));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("调账操作日志查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> review(long adjustmentId, long adminUserId, String decision, String note,
                                      String ipAddress) {
        String targetStatus;
        String normalizedNote;
        try {
            targetStatus = normalizeReviewDecision(decision);
            normalizedNote = normalizeRequiredText(note, "请填写复核意见。");
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            AdjustmentRequest request = adjustmentDao.findByIdForUpdate(connection, adjustmentId);
            if (request == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账申请不存在。");
            }
            if (!"PENDING_REVIEW".equals(request.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("只有待复核的调账申请可以审核。");
            }
            if (Long.valueOf(adminUserId).equals(request.getApplicantAdminUserId())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("申请人不能复核自己的调账申请。");
            }

            Timestamp reviewedAt = new Timestamp(System.currentTimeMillis());
            adjustmentDao.updateReview(connection, adjustmentId, adminUserId, targetStatus,
                    normalizedNote, reviewedAt);
            insertActionLog(connection, adjustmentId, adminUserId, "REVIEW", request.getStatus(), targetStatus,
                    normalizedNote);
            adminAuditLogDao.insert(connection, adminUserId, "REVIEW_ADJUSTMENT", "ADJUSTMENT_REQUEST",
                    String.valueOf(adjustmentId), "复核调账申请：" + request.getAdjustmentNo()
                            + "，结论：" + targetStatus + "，意见：" + normalizedNote, ipAddress);
            adminAlertService.resolveByTarget(connection, "ADJUSTMENT_REVIEW", "ADJUSTMENT_REQUEST",
                    String.valueOf(adjustmentId), adminUserId,
                    "调账申请 " + request.getAdjustmentNo() + " 已完成复核，结论：" + targetStatus);
            if ("APPROVED".equals(targetStatus)) {
                adminAlertService.create(connection, "ADJUSTMENT_EXECUTE", "WARNING",
                        "调账申请待执行",
                        "调账申请 " + request.getAdjustmentNo() + " 已复核通过，金额 ¥"
                                + moneyText(request.getAmount()) + "，需要继续执行入账。",
                        "ADJUSTMENT_REQUEST", String.valueOf(adjustmentId), "ACCOUNTING_REVIEWER");
            }
            syncTicketAfterAdjustmentReview(connection, request, adminUserId, targetStatus, normalizedNote);

            connection.commit();
            return ServiceResult.success("调账申请复核完成。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("调账申请复核失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<Void> execute(long adjustmentId, long adminUserId, String ipAddress) {
        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            AdjustmentRequest request = adjustmentDao.findByIdForUpdate(connection, adjustmentId);
            if (request == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账申请不存在。");
            }
            if (!"APPROVED".equals(request.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("只有复核通过的调账申请可以执行。");
            }
            if (Long.valueOf(adminUserId).equals(request.getApplicantAdminUserId())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("申请人不能执行自己的调账申请。");
            }

            Account account = accountDao.findByIdForUpdate(connection, request.getAccountId());
            if (account == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户不存在。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                rollbackQuietly(connection);
                return ServiceResult.failure("调账账户状态不可用。");
            }

            BigDecimal balanceAfter;
            String ledgerDirection;
            if ("INCREASE".equals(request.getDirection())) {
                balanceAfter = scale(account.getAvailableBalance().add(request.getAmount()));
                ledgerDirection = "IN";
            } else {
                if (account.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                    rollbackQuietly(connection);
                    return ServiceResult.failure("账户可用余额不足，不能执行调减。");
                }
                balanceAfter = scale(account.getAvailableBalance().subtract(request.getAmount()));
                ledgerDirection = "OUT";
            }

            accountDao.updateAvailableBalance(connection, account.getAccountId(), balanceAfter);

            String transactionNo = TransactionNoGenerator.generate();
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setTransactionNo(transactionNo);
            transactionRecord.setCustomerId(account.getCustomerId());
            if ("INCREASE".equals(request.getDirection())) {
                transactionRecord.setToAccountId(account.getAccountId());
            } else {
                transactionRecord.setFromAccountId(account.getAccountId());
            }
            transactionRecord.setTxnType("ACCOUNT_ADJUSTMENT");
            transactionRecord.setAmount(request.getAmount());
            transactionRecord.setStatus("SUCCESS");
            transactionRecord.setRiskScore(0);
            transactionRecord.setRemark("调账申请：" + request.getAdjustmentNo() + "，原因：" + request.getReason());
            long transactionId = transactionDao.insert(connection, transactionRecord);

            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setTransactionId(transactionId);
            ledgerEntry.setAccountId(account.getAccountId());
            ledgerEntry.setDirection(ledgerDirection);
            ledgerEntry.setAmount(request.getAmount());
            ledgerEntry.setBalanceAfter(balanceAfter);
            ledgerEntry.setSummary("账务调账：" + request.getAdjustmentNo());
            long ledgerId = ledgerDao.insert(connection, ledgerEntry);

            Timestamp executedAt = new Timestamp(System.currentTimeMillis());
            adjustmentDao.updateExecuted(connection, adjustmentId, transactionId, ledgerId, executedAt);
            insertActionLog(connection, adjustmentId, adminUserId, "EXECUTE", "APPROVED", "EXECUTED",
                    "执行调账，交易号：" + transactionNo + "，调账后余额：" + moneyText(balanceAfter));
            markReconciliationFixed(connection, request, adminUserId);

            adminAuditLogDao.insert(connection, adminUserId, "EXECUTE_ADJUSTMENT", "ADJUSTMENT_REQUEST",
                    String.valueOf(adjustmentId), "执行调账申请：" + request.getAdjustmentNo()
                            + "，交易号：" + transactionNo + "，账户：" + account.getAccountNo()
                            + "，方向：" + request.getDirection()
                            + "，金额：" + moneyText(request.getAmount()), ipAddress);
            adminAlertService.resolveByTarget(connection, "ADJUSTMENT_EXECUTE", "ADJUSTMENT_REQUEST",
                    String.valueOf(adjustmentId), adminUserId,
                    "调账申请 " + request.getAdjustmentNo() + " 已执行，交易号：" + transactionNo);
            syncTicketAfterAdjustmentExecuted(connection, request, adminUserId, transactionNo, balanceAfter);

            connection.commit();
            return ServiceResult.success("调账已执行，账户余额、交易流水和关联业务状态已更新。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("调账执行失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    private AdjustmentRequest buildRequest(long adminUserId, Account account, CreateInput input) {
        AdjustmentRequest request = new AdjustmentRequest();
        request.setAdjustmentNo(AdjustmentNoGenerator.generate());
        request.setAccountId(account.getAccountId());
        request.setCustomerId(account.getCustomerId());
        request.setDirection(input.direction);
        request.setAmount(input.amount);
        request.setReason(input.reason);
        request.setEvidence(input.evidence);
        request.setStatus("PENDING_REVIEW");
        request.setApplicantAdminUserId(adminUserId);
        return request;
    }

    private void createReviewAlert(Connection connection, String adjustmentNo, long adjustmentId, String accountNo,
                                   String direction, BigDecimal amount, String title) throws SQLException {
        adminAlertService.create(connection, "ADJUSTMENT_REVIEW", "WARNING", title,
                "调账申请 " + adjustmentNo + " 已提交，账户 " + accountNo
                        + "，方向 " + direction + "，金额 ¥" + moneyText(amount)
                        + "，需要账务复核员复核。",
                "ADJUSTMENT_REQUEST", String.valueOf(adjustmentId), "ACCOUNTING_REVIEWER");
    }

    private void markReconciliationFixed(Connection connection, AdjustmentRequest request, long adminUserId)
            throws SQLException {
        if (request.getReconciliationItemId() == null) {
            return;
        }
        ReconciliationItem item = reconciliationDao.findItemById(connection, request.getReconciliationItemId());
        if (item == null || "FIXED".equals(item.getStatus())) {
            return;
        }
        String beforeStatus = item.getStatus();
        String note = "调账申请 " + request.getAdjustmentNo() + " 已执行，自动标记为已修复。";
        reconciliationDao.updateItemHandling(connection, item.getItemId(), adminUserId, "FIXED",
                "FIXED", note, new Timestamp(System.currentTimeMillis()));

        ReconciliationActionLog actionLog = new ReconciliationActionLog();
        actionLog.setItemId(item.getItemId());
        actionLog.setAdminUserId(adminUserId);
        actionLog.setActionType("ADJUSTMENT_EXECUTED");
        actionLog.setBeforeStatus(beforeStatus);
        actionLog.setAfterStatus("FIXED");
        actionLog.setNote(note);
        reconciliationDao.insertActionLog(connection, actionLog);
        syncReconciliationBatchAlert(connection, item.getBatchId(), adminUserId);
    }

    private void syncReconciliationBatchAlert(Connection connection, long batchId, long adminUserId)
            throws SQLException {
        int unfinishedCount = reconciliationDao.countUnfinishedItemsByBatch(connection, batchId);
        if (unfinishedCount == 0) {
            adminAlertService.resolveByTarget(connection, "RECONCILIATION_EXCEPTION", "RECONCILIATION_BATCH",
                    String.valueOf(batchId), adminUserId, "该对账批次异常项已全部处理完成。");
        } else {
            adminAlertService.ackByTarget(connection, "RECONCILIATION_EXCEPTION", "RECONCILIATION_BATCH",
                    String.valueOf(batchId), adminUserId, "对账批次处理中，剩余未完成异常 " + unfinishedCount + " 项。");
        }
    }

    private void syncTicketAfterAdjustmentReview(Connection connection, AdjustmentRequest request,
                                                long adminUserId, String targetStatus, String note)
            throws SQLException {
        if (!"SERVICE_TICKET".equals(request.getSourceType()) || request.getSourceTicketId() == null) {
            return;
        }
        ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, request.getSourceTicketId());
        if (ticket == null) {
            return;
        }
        if ("APPROVED".equals(targetStatus)) {
            insertTicketReply(connection, ticket.getTicketId(), "ADMIN", adminUserId,
                    "关联调账申请 " + request.getAdjustmentNo() + " 已复核通过，等待执行入账。");
            insertTicketActionLog(connection, ticket.getTicketId(), adminUserId, "ADJUSTMENT_REVIEW",
                    ticket.getStatus(), ticket.getStatus(), "调账复核通过：" + note);
            notificationService.create(connection, ticket.getCustomerId(), ticket.getUserId(), "SERVICE",
                    "工单关联调账已复核通过",
                    "您的工单 " + ticket.getTicketNo() + " 关联的调账申请已复核通过，等待执行。",
                    "SERVICE_TICKET", ticket.getTicketNo());
            return;
        }
        String beforeStatus = ticket.getStatus();
        updateTicketState(connection, ticket, "RESOLVED", adminUserId);
        insertTicketReply(connection, ticket.getTicketId(), "ADMIN", adminUserId,
                "关联调账申请 " + request.getAdjustmentNo() + " 复核未通过。复核意见：" + note);
        insertTicketActionLog(connection, ticket.getTicketId(), adminUserId, "ADJUSTMENT_REJECTED",
                beforeStatus, "RESOLVED", "调账复核未通过：" + note);
        adminAlertService.resolveByTarget(connection, "TICKET_NEW", "SERVICE_TICKET",
                String.valueOf(ticket.getTicketId()), adminUserId, "关联调账复核未通过，工单已处理");
        adminAlertService.resolveByTarget(connection, "TICKET_FOLLOW_UP", "SERVICE_TICKET",
                String.valueOf(ticket.getTicketId()), adminUserId, "关联调账复核未通过，工单已处理");
        notificationService.create(connection, ticket.getCustomerId(), ticket.getUserId(), "SERVICE",
                "工单调账复核未通过",
                "您的工单 " + ticket.getTicketNo() + " 关联的调账申请复核未通过。复核意见：" + note,
                "SERVICE_TICKET", ticket.getTicketNo());
    }

    private void syncTicketAfterAdjustmentExecuted(Connection connection, AdjustmentRequest request,
                                                  long adminUserId, String transactionNo, BigDecimal balanceAfter)
            throws SQLException {
        if (!"SERVICE_TICKET".equals(request.getSourceType()) || request.getSourceTicketId() == null) {
            return;
        }
        ServiceTicket ticket = ticketDao.findByIdForUpdate(connection, request.getSourceTicketId());
        if (ticket == null) {
            return;
        }
        String beforeStatus = ticket.getStatus();
        updateTicketState(connection, ticket, "RESOLVED", adminUserId);
        insertTicketReply(connection, ticket.getTicketId(), "ADMIN", adminUserId,
                "关联调账申请 " + request.getAdjustmentNo() + " 已执行完成，交易号："
                        + transactionNo + "，调账后余额：" + moneyText(balanceAfter) + "。请确认处理结果。");
        insertTicketActionLog(connection, ticket.getTicketId(), adminUserId, "ADJUSTMENT_EXECUTED",
                beforeStatus, "RESOLVED", "调账执行完成，交易号：" + transactionNo);
        adminAlertService.resolveByTarget(connection, "TICKET_NEW", "SERVICE_TICKET",
                String.valueOf(ticket.getTicketId()), adminUserId, "关联调账已执行，工单已处理");
        adminAlertService.resolveByTarget(connection, "TICKET_FOLLOW_UP", "SERVICE_TICKET",
                String.valueOf(ticket.getTicketId()), adminUserId, "关联调账已执行，工单已处理");
        notificationService.create(connection, ticket.getCustomerId(), ticket.getUserId(), "SERVICE",
                "工单关联调账已执行",
                "您的工单 " + ticket.getTicketNo() + " 关联的调账申请已执行完成，交易号："
                        + transactionNo + "。请确认处理结果。",
                "SERVICE_TICKET", ticket.getTicketNo());
    }

    private boolean canAdminHandleTicket(long adminUserId, ServiceTicket ticket) {
        Set<String> permissions = permissionService.permissionsFor(adminUserId);
        if (!permissions.contains("TICKET_HANDLE") && !permissions.contains("TICKET_ALL_VIEW")) {
            return false;
        }
        Set<String> roles = permissionService.rolesFor(adminUserId);
        if (permissions.contains("TICKET_ALL_VIEW") || roles.contains("SUPER_ADMIN") || roles.contains("AUDITOR")) {
            return true;
        }
        if (ticket.getAssignedAdminUserId() != null
                && Long.valueOf(adminUserId).equals(ticket.getAssignedAdminUserId())) {
            return true;
        }
        return ticket.getAssignedRoleCode() != null && roles.contains(ticket.getAssignedRoleCode());
    }

    private void updateTicketState(Connection connection, ServiceTicket ticket, String status, long adminUserId)
            throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp acceptedAt = ticket.getAcceptedAt();
        Timestamp resolvedAt = ticket.getResolvedAt();
        Timestamp closedAt = ticket.getClosedAt();
        if (acceptedAt == null) {
            acceptedAt = now;
        }
        if ("RESOLVED".equals(status)) {
            resolvedAt = now;
            closedAt = null;
        }
        ticketDao.updateTicketState(connection, ticket.getTicketId(), status, ticket.getAssignedRoleCode(),
                Long.valueOf(adminUserId), acceptedAt, resolvedAt, closedAt);
    }

    private void insertTicketReply(Connection connection, long ticketId, String senderType, long senderUserId,
                                   String content) throws SQLException {
        TicketReply reply = new TicketReply();
        reply.setTicketId(ticketId);
        reply.setSenderType(senderType);
        reply.setSenderUserId(Long.valueOf(senderUserId));
        reply.setContent(trim(content, 1000));
        ticketDao.insertReply(connection, reply);
    }

    private void insertTicketActionLog(Connection connection, long ticketId, long adminUserId, String actionType,
                                       String beforeStatus, String afterStatus, String note) throws SQLException {
        TicketActionLog log = new TicketActionLog();
        log.setTicketId(ticketId);
        log.setAdminUserId(Long.valueOf(adminUserId));
        log.setActionType(actionType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setNote(trim(note, 500));
        ticketDao.insertActionLog(connection, log);
    }

    private void insertActionLog(Connection connection, long adjustmentId, long adminUserId, String actionType,
                                 String beforeStatus, String afterStatus, String note) throws SQLException {
        AdjustmentActionLog actionLog = new AdjustmentActionLog();
        actionLog.setAdjustmentId(adjustmentId);
        actionLog.setAdminUserId(adminUserId);
        actionLog.setActionType(actionType);
        actionLog.setBeforeStatus(beforeStatus);
        actionLog.setAfterStatus(afterStatus);
        actionLog.setNote(trim(note, 500));
        adjustmentDao.insertActionLog(connection, actionLog);
    }

    private CreateInput normalizeCreateInput(String accountNo, String direction, String amountText, String reason,
                                             String evidence) {
        CreateInput input = new CreateInput();
        try {
            input.accountNo = normalizeAccountNo(accountNo);
            input.direction = normalizeDirection(direction);
            input.amount = MoneyUtil.parseAmount(amountText);
            input.reason = normalizeRequiredText(reason, "请填写调账原因。");
            input.evidence = normalizeRequiredText(evidence, "请填写业务依据。");
        } catch (IllegalArgumentException e) {
            input.errorMessage = e.getMessage();
            return input;
        }
        if (!MoneyUtil.isPositive(input.amount)) {
            input.errorMessage = "调账金额必须大于 0。";
        } else if (input.amount.compareTo(MAX_ADJUSTMENT_AMOUNT) > 0) {
            input.errorMessage = "单笔调账金额不能超过 100,000.00。";
        }
        return input;
    }

    private String normalizeAccountNo(String accountNo) {
        String value = accountNo == null ? "" : accountNo.trim();
        if (value.length() == 0) {
            throw new IllegalArgumentException("请填写调账账号。");
        }
        return trim(value, 32);
    }

    private String normalizeDirection(String direction) {
        if ("INCREASE".equals(direction) || "DECREASE".equals(direction)) {
            return direction;
        }
        throw new IllegalArgumentException("请选择正确的调账方向。");
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.trim().length() == 0 || "ALL".equals(status)) {
            return null;
        }
        String value = status.trim();
        if ("PENDING_REVIEW".equals(value) || "APPROVED".equals(value) || "REJECTED".equals(value)
                || "EXECUTED".equals(value) || "FAILED".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的调账状态。");
    }

    private String normalizeReviewDecision(String decision) {
        if ("APPROVE".equals(decision)) {
            return "APPROVED";
        }
        if ("REJECT".equals(decision)) {
            return "REJECTED";
        }
        throw new IllegalArgumentException("请选择正确的复核结论。");
    }

    private String normalizeRequiredText(String value, String emptyMessage) {
        String text = value == null ? "" : value.trim();
        if (text.length() < 5) {
            throw new IllegalArgumentException(emptyMessage + " 至少 5 个字。");
        }
        return trim(text, 500);
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return scale(amount).toPlainString();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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

    private static class CreateInput {
        private String accountNo;
        private String direction;
        private BigDecimal amount;
        private String reason;
        private String evidence;
        private String errorMessage;
    }
}
