package com.bank.service.impl;

import com.bank.bean.ReconciliationBatch;
import com.bank.bean.ReconciliationActionLog;
import com.bank.bean.ReconciliationItem;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.ReconciliationDao;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.ReconciliationDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAlertService;
import com.bank.service.ReconciliationService;
import com.bank.util.DBUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationServiceImpl implements ReconciliationService {
    private static final int BATCH_QUERY_LIMIT = 50;
    private static final int ITEM_QUERY_LIMIT = 200;

    private final ReconciliationDao reconciliationDao = new ReconciliationDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();

    @Override
    public ServiceResult<ReconciliationBatch> run(LocalDate reconDate, long adminUserId, String ipAddress) {
        if (reconDate == null) {
            return ServiceResult.failure("请选择对账日期。");
        }
        if (reconDate.isAfter(LocalDate.now())) {
            return ServiceResult.failure("不能对未来日期发起对账。");
        }

        long batchId = 0L;
        Timestamp startedAt = new Timestamp(System.currentTimeMillis());
        try (Connection connection = DBUtil.getConnection()) {
            ReconciliationBatch batch = new ReconciliationBatch();
            batch.setReconDate(Date.valueOf(reconDate));
            batch.setStatus("RUNNING");
            batch.setTotalChecks(0);
            batch.setExceptionCount(0);
            batch.setCreatedByAdminUserId(adminUserId);
            batch.setStartedAt(startedAt);
            batchId = reconciliationDao.insertBatch(connection, batch);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账批次创建失败，请检查数据库状态或稍后重试。");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            CheckCounter counter = new CheckCounter();
            checkTransactionLedger(connection, batchId, reconDate, counter);
            checkCurrentAccountBalance(connection, batchId, counter);
            checkWealthBuy(connection, batchId, reconDate, counter);
            checkWealthRedeem(connection, batchId, reconDate, counter);
            checkRiskBlock(connection, batchId, reconDate, counter);

            String status = counter.exceptionCount > 0 ? "HAS_EXCEPTION" : "SUCCESS";
            Timestamp finishedAt = new Timestamp(System.currentTimeMillis());
            reconciliationDao.completeBatch(connection, batchId, status, counter.totalChecks,
                    counter.exceptionCount, finishedAt);
            adminAuditLogDao.insert(connection, adminUserId, "RUN_RECONCILIATION", "RECONCILIATION_BATCH",
                    String.valueOf(batchId), "发起账务对账，日期：" + reconDate
                            + "，检查项：" + counter.totalChecks
                            + "，异常：" + counter.exceptionCount, ipAddress);
            if (counter.exceptionCount > 0) {
                adminAlertService.create(connection, "RECONCILIATION_EXCEPTION", "HIGH",
                        "账务对账出现异常",
                        "对账日期 " + reconDate + " 共发现 " + counter.exceptionCount
                                + " 个异常项，请账务运营在异常处理中心跟进。",
                        "RECONCILIATION_BATCH", String.valueOf(batchId), "ACCOUNTING_OPERATOR");
            }
            connection.commit();

            ReconciliationBatch batch = reconciliationDao.findBatchById(connection, batchId);
            return ServiceResult.success("账务对账完成。", batch);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            markFailed(batchId);
            e.printStackTrace();
            return ServiceResult.failure("账务对账执行失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<ReconciliationBatch>> listRecentBatches() {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("对账批次查询成功。",
                    reconciliationDao.findRecentBatches(connection, BATCH_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账批次查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<ReconciliationBatch> getBatch(long batchId) {
        if (batchId <= 0) {
            return ServiceResult.failure("对账批次不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            ReconciliationBatch batch = reconciliationDao.findBatchById(connection, batchId);
            if (batch == null) {
                return ServiceResult.failure("对账批次不存在。");
            }
            return ServiceResult.success("对账批次查询成功。", batch);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账批次查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<ReconciliationItem>> listItems(long batchId) {
        if (batchId <= 0) {
            return ServiceResult.failure("对账批次不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("对账明细查询成功。",
                    reconciliationDao.findItemsByBatch(connection, batchId));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账明细查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<ReconciliationItem>> listExceptionItems(String status, String severity,
                                                                      String checkType) {
        String normalizedStatus;
        String normalizedSeverity;
        String normalizedCheckType;
        try {
            normalizedStatus = normalizeStatus(status);
            normalizedSeverity = normalizeSeverity(severity);
            normalizedCheckType = normalizeCheckType(checkType);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("对账异常查询成功。",
                    reconciliationDao.findItems(connection, normalizedStatus, normalizedSeverity,
                            normalizedCheckType, ITEM_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账异常查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<ReconciliationItem> getItem(long itemId) {
        if (itemId <= 0) {
            return ServiceResult.failure("对账异常不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            ReconciliationItem item = reconciliationDao.findItemById(connection, itemId);
            if (item == null) {
                return ServiceResult.failure("对账异常不存在。");
            }
            return ServiceResult.success("对账异常查询成功。", item);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账异常查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<ReconciliationActionLog>> listActionLogs(long itemId) {
        if (itemId <= 0) {
            return ServiceResult.failure("对账异常不存在。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("对账处理日志查询成功。",
                    reconciliationDao.findActionLogsByItem(connection, itemId));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("对账处理日志查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> handleItem(long itemId, long adminUserId, String targetStatus, String note,
                                          String ipAddress) {
        String normalizedStatus;
        try {
            normalizedStatus = normalizeTargetStatus(targetStatus);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        String normalizedNote = normalizeHandleNote(normalizedStatus, note);
        if (normalizedNote == null) {
            return ServiceResult.failure("除接手处理外，其他处理动作必须填写至少 5 个字的处理说明。");
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
            String beforeStatus = item.getStatus();
            if (normalizedStatus.equals(beforeStatus)) {
                rollbackQuietly(connection);
                return ServiceResult.failure("处理状态没有变化。");
            }

            Timestamp handledAt = new Timestamp(System.currentTimeMillis());
            reconciliationDao.updateItemHandling(connection, itemId, adminUserId, normalizedStatus,
                    normalizedStatus, normalizedNote, handledAt);

            ReconciliationActionLog actionLog = new ReconciliationActionLog();
            actionLog.setItemId(itemId);
            actionLog.setAdminUserId(adminUserId);
            actionLog.setActionType("STATUS_CHANGE");
            actionLog.setBeforeStatus(beforeStatus);
            actionLog.setAfterStatus(normalizedStatus);
            actionLog.setNote(normalizedNote);
            reconciliationDao.insertActionLog(connection, actionLog);

            adminAuditLogDao.insert(connection, adminUserId, "HANDLE_RECONCILIATION_ITEM",
                    "RECONCILIATION_ITEM", String.valueOf(itemId),
                    "处理对账异常，状态：" + beforeStatus + " -> " + normalizedStatus
                            + "，业务标识：" + item.getBusinessId()
                            + "，说明：" + normalizedNote,
                    ipAddress);
            syncReconciliationBatchAlert(connection, item.getBatchId(), adminUserId);

            connection.commit();
            return ServiceResult.success("对账异常处理状态已更新。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("对账异常处理失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    private void checkTransactionLedger(Connection connection, long batchId, LocalDate reconDate,
                                        CheckCounter counter) throws SQLException {
        for (Txn txn : findSuccessTransactions(connection, reconDate)) {
            if ("DEPOSIT".equals(txn.txnType)) {
                assertLedger(connection, batchId, counter, txn, txn.toAccountId, "IN");
            } else if ("WITHDRAW".equals(txn.txnType) || "PAYMENT".equals(txn.txnType)
                    || "BUY_WEALTH".equals(txn.txnType)) {
                assertLedger(connection, batchId, counter, txn, txn.fromAccountId, "OUT");
            } else if ("REDEEM_WEALTH".equals(txn.txnType)) {
                assertLedger(connection, batchId, counter, txn, txn.toAccountId, "IN");
            } else if ("TRANSFER_INNER".equals(txn.txnType)) {
                assertLedger(connection, batchId, counter, txn, txn.fromAccountId, "OUT");
                assertLedger(connection, batchId, counter, txn, txn.toAccountId, "IN");
            } else if ("ACCOUNT_ADJUSTMENT".equals(txn.txnType)) {
                if (txn.toAccountId != null) {
                    assertLedger(connection, batchId, counter, txn, txn.toAccountId, "IN");
                } else {
                    assertLedger(connection, batchId, counter, txn, txn.fromAccountId, "OUT");
                }
            }
        }
    }

    private void syncReconciliationBatchAlert(Connection connection, long batchId, long adminUserId)
            throws SQLException {
        int unfinishedCount = reconciliationDao.countUnfinishedItemsByBatch(connection, batchId);
        if (unfinishedCount == 0) {
            adminAlertService.resolveByTarget(connection, "RECONCILIATION_EXCEPTION", "RECONCILIATION_BATCH",
                    String.valueOf(batchId), adminUserId, "该对账批次异常项已全部处理完成");
        } else {
            adminAlertService.ackByTarget(connection, "RECONCILIATION_EXCEPTION", "RECONCILIATION_BATCH",
                    String.valueOf(batchId), adminUserId, "对账批次处理中，剩余未完成异常 " + unfinishedCount + " 项");
        }
    }

    private void assertLedger(Connection connection, long batchId, CheckCounter counter, Txn txn,
                              Long accountId, String direction) throws SQLException {
        counter.totalChecks++;
        String expected = "accountId=" + accountId + ", direction=" + direction
                + ", amount=" + money(txn.amount);
        if (accountId == null) {
            addItem(connection, batchId, "TRANSACTION_LEDGER", "CRITICAL", txn.txnType,
                    txn.transactionNo, expected, "accountId=NULL",
                    "成功交易缺少应记账账户，无法形成完整流水。");
            counter.exceptionCount++;
            return;
        }
        int count = ledgerMatchCount(connection, txn.transactionId, accountId, direction, txn.amount);
        if (count <= 0) {
            addItem(connection, batchId, "TRANSACTION_LEDGER", "CRITICAL", txn.txnType,
                    txn.transactionNo, expected, "匹配流水笔数=0",
                    "成功交易未找到匹配的账户流水。");
            counter.exceptionCount++;
        }
    }

    private void checkCurrentAccountBalance(Connection connection, long batchId, CheckCounter counter)
            throws SQLException {
        String sql = "SELECT a.account_id, a.account_no, a.available_balance, l.ledger_id, l.balance_after "
                + "FROM t_account a "
                + "LEFT JOIN (SELECT account_id, MAX(ledger_id) AS ledger_id FROM t_ledger_entry "
                + "GROUP BY account_id) latest ON a.account_id = latest.account_id "
                + "LEFT JOIN t_ledger_entry l ON latest.ledger_id = l.ledger_id "
                + "ORDER BY a.account_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Long ledgerId = getNullableLong(resultSet, "ledger_id");
                if (ledgerId == null) {
                    continue;
                }
                counter.totalChecks++;
                BigDecimal availableBalance = resultSet.getBigDecimal("available_balance");
                BigDecimal balanceAfter = resultSet.getBigDecimal("balance_after");
                if (!sameMoney(availableBalance, balanceAfter)) {
                    addItem(connection, batchId, "ACCOUNT_BALANCE", "CRITICAL", "ACCOUNT",
                            resultSet.getString("account_no"),
                            "账户当前可用余额=" + money(balanceAfter),
                            "账户表余额=" + money(availableBalance),
                            "账户余额与最后一条流水余额不一致。");
                    counter.exceptionCount++;
                }
            }
        }
    }

    private void checkWealthBuy(Connection connection, long batchId, LocalDate reconDate,
                                CheckCounter counter) throws SQLException {
        for (Txn txn : findTransactionsByType(connection, reconDate, "BUY_WEALTH")) {
            counter.totalChecks++;
            int holdingCount = countByTransactionAndAmount(connection,
                    "SELECT COUNT(*) FROM t_wealth_holding WHERE buy_transaction_id = ? AND principal = ?",
                    txn.transactionId, txn.amount);
            if (holdingCount <= 0) {
                addItem(connection, batchId, "WEALTH_BUY", "CRITICAL", "BUY_WEALTH",
                        txn.transactionNo, "存在理财持仓且本金=" + money(txn.amount),
                        "匹配持仓笔数=0", "理财申购成功交易未形成对应持仓。");
                counter.exceptionCount++;
            }

            counter.totalChecks++;
            int confirmCount = countByTransactionAndAmount(connection,
                    "SELECT COUNT(*) FROM t_wealth_order_confirm WHERE transaction_id = ? AND amount = ?",
                    txn.transactionId, txn.amount);
            if (confirmCount <= 0) {
                addItem(connection, batchId, "WEALTH_BUY", "CRITICAL", "BUY_WEALTH",
                        txn.transactionNo, "存在申购确认记录且金额=" + money(txn.amount),
                        "匹配确认笔数=0", "理财申购成功交易缺少风险揭示和确认记录。");
                counter.exceptionCount++;
            }
        }
    }

    private void checkWealthRedeem(Connection connection, long batchId, LocalDate reconDate,
                                   CheckCounter counter) throws SQLException {
        for (Txn txn : findTransactionsByType(connection, reconDate, "REDEEM_WEALTH")) {
            counter.totalChecks++;
            int count = countByTransaction(connection,
                    "SELECT COUNT(*) FROM t_wealth_holding WHERE redeem_transaction_id = ? "
                            + "AND status = 'REDEEMED'",
                    txn.transactionId);
            if (count <= 0) {
                addItem(connection, batchId, "WEALTH_REDEEM", "CRITICAL", "REDEEM_WEALTH",
                        txn.transactionNo, "持仓已标记 REDEEMED 且关联赎回交易",
                        "匹配赎回持仓笔数=0", "理财赎回成功交易未回写对应持仓状态。");
                counter.exceptionCount++;
            }
        }
    }

    private void checkRiskBlock(Connection connection, long batchId, LocalDate reconDate,
                                CheckCounter counter) throws SQLException {
        String sql = "SELECT event_id, transaction_no, txn_type, amount FROM t_risk_event "
                + "WHERE decision = 'BLOCK' AND created_at >= ? AND created_at < ? "
                + "ORDER BY event_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDateRange(statement, reconDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counter.totalChecks++;
                    String transactionNo = resultSet.getString("transaction_no");
                    int successCount = countSuccessTransactionByNo(connection, transactionNo);
                    if (successCount > 0) {
                        addItem(connection, batchId, "RISK_BLOCK", "CRITICAL",
                                resultSet.getString("txn_type"), transactionNo,
                                "风控 BLOCK 不应产生 SUCCESS 交易",
                                "SUCCESS交易笔数=" + successCount,
                                "被风控拦截的交易号出现在成功交易表中。");
                        counter.exceptionCount++;
                    }
                }
            }
        }
    }

    private List<Txn> findSuccessTransactions(Connection connection, LocalDate reconDate) throws SQLException {
        String sql = "SELECT transaction_id, transaction_no, from_account_id, to_account_id, txn_type, amount "
                + "FROM t_transaction WHERE status = 'SUCCESS' AND created_at >= ? AND created_at < ? "
                + "ORDER BY transaction_id";
        return findTransactions(connection, sql, reconDate, null);
    }

    private List<Txn> findTransactionsByType(Connection connection, LocalDate reconDate, String txnType)
            throws SQLException {
        String sql = "SELECT transaction_id, transaction_no, from_account_id, to_account_id, txn_type, amount "
                + "FROM t_transaction WHERE status = 'SUCCESS' AND txn_type = ? "
                + "AND created_at >= ? AND created_at < ? ORDER BY transaction_id";
        return findTransactions(connection, sql, reconDate, txnType);
    }

    private List<Txn> findTransactions(Connection connection, String sql, LocalDate reconDate, String txnType)
            throws SQLException {
        List<Txn> transactions = new ArrayList<Txn>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (txnType != null) {
                statement.setString(index++, txnType);
            }
            bindDateRange(statement, reconDate, index);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Txn txn = new Txn();
                    txn.transactionId = resultSet.getLong("transaction_id");
                    txn.transactionNo = resultSet.getString("transaction_no");
                    txn.fromAccountId = getNullableLong(resultSet, "from_account_id");
                    txn.toAccountId = getNullableLong(resultSet, "to_account_id");
                    txn.txnType = resultSet.getString("txn_type");
                    txn.amount = resultSet.getBigDecimal("amount");
                    transactions.add(txn);
                }
            }
        }
        return transactions;
    }

    private int ledgerMatchCount(Connection connection, long transactionId, long accountId, String direction,
                                 BigDecimal amount) throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_ledger_entry WHERE transaction_id = ? "
                + "AND account_id = ? AND direction = ? AND amount = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, accountId);
            statement.setString(3, direction);
            statement.setBigDecimal(4, amount);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private int countByTransactionAndAmount(Connection connection, String sql, long transactionId,
                                            BigDecimal amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setBigDecimal(2, amount);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private int countByTransaction(Connection connection, String sql, long transactionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private int countSuccessTransactionByNo(Connection connection, String transactionNo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_transaction WHERE transaction_no = ? AND status = 'SUCCESS'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transactionNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private void addItem(Connection connection, long batchId, String checkType, String severity,
                         String businessType, String businessId, String expectedValue, String actualValue,
                         String description) throws SQLException {
        ReconciliationItem item = new ReconciliationItem();
        item.setBatchId(batchId);
        item.setCheckType(checkType);
        item.setSeverity(severity);
        item.setBusinessType(businessType);
        item.setBusinessId(businessId);
        item.setExpectedValue(trim(expectedValue, 255));
        item.setActualValue(trim(actualValue, 255));
        item.setDescription(trim(description, 500));
        item.setStatus("OPEN");
        reconciliationDao.insertItem(connection, item);
    }

    private void bindDateRange(PreparedStatement statement, LocalDate reconDate) throws SQLException {
        bindDateRange(statement, reconDate, 1);
    }

    private void bindDateRange(PreparedStatement statement, LocalDate reconDate, int startIndex)
            throws SQLException {
        statement.setTimestamp(startIndex, Timestamp.valueOf(reconDate.atStartOfDay()));
        statement.setTimestamp(startIndex + 1, Timestamp.valueOf(reconDate.plusDays(1).atStartOfDay()));
    }

    private Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private boolean sameMoney(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == null && right == null;
        }
        return left.setScale(2, RoundingMode.HALF_UP)
                .compareTo(right.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().length() == 0 || "ALL".equals(status)) {
            return null;
        }
        String value = status.trim();
        if (isValidItemStatus(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的处理状态。");
    }

    private String normalizeTargetStatus(String status) {
        if (status == null || status.trim().length() == 0) {
            throw new IllegalArgumentException("请选择处理动作。");
        }
        String value = status.trim();
        if ("OPEN".equals(value)) {
            throw new IllegalArgumentException("不能将异常重新设置为待处理。");
        }
        if (isValidItemStatus(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的处理动作。");
    }

    private boolean isValidItemStatus(String status) {
        return "OPEN".equals(status)
                || "INVESTIGATING".equals(status)
                || "CONFIRMED_EXCEPTION".equals(status)
                || "ACCEPTED".equals(status)
                || "FIXED".equals(status)
                || "CLOSED".equals(status);
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.trim().length() == 0 || "ALL".equals(severity)) {
            return null;
        }
        String value = severity.trim();
        if ("CRITICAL".equals(value) || "WARN".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的严重度。");
    }

    private String normalizeCheckType(String checkType) {
        if (checkType == null || checkType.trim().length() == 0 || "ALL".equals(checkType)) {
            return null;
        }
        String value = checkType.trim();
        if ("TRANSACTION_LEDGER".equals(value)
                || "ACCOUNT_BALANCE".equals(value)
                || "WEALTH_BUY".equals(value)
                || "WEALTH_REDEEM".equals(value)
                || "RISK_BLOCK".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("请选择正确的检查类型。");
    }

    private String normalizeHandleNote(String targetStatus, String note) {
        String value = note == null ? "" : note.trim();
        if (value.length() > 500) {
            value = value.substring(0, 500);
        }
        if ("INVESTIGATING".equals(targetStatus)) {
            return value.length() == 0 ? "接手处理" : value;
        }
        return value.length() >= 5 ? value : null;
    }

    private void markFailed(long batchId) {
        if (batchId <= 0) {
            return;
        }
        try (Connection connection = DBUtil.getConnection()) {
            reconciliationDao.completeBatch(connection, batchId, "FAILED", 0, 1,
                    new Timestamp(System.currentTimeMillis()));
        } catch (SQLException ignored) {
        }
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

    private static class CheckCounter {
        private int totalChecks;
        private int exceptionCount;
    }

    private static class Txn {
        private long transactionId;
        private String transactionNo;
        private Long fromAccountId;
        private Long toAccountId;
        private String txnType;
        private BigDecimal amount;
    }
}
