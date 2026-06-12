package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.bean.BillPayment;
import com.bank.bean.Customer;
import com.bank.bean.LedgerEntry;
import com.bank.bean.TransactionRecord;
import com.bank.bean.User;
import com.bank.dao.AccountDao;
import com.bank.dao.BillPaymentDao;
import com.bank.dao.CustomerDao;
import com.bank.dao.LedgerDao;
import com.bank.dao.OperationLogDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.UserDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dao.impl.BillPaymentDaoImpl;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dao.impl.LedgerDaoImpl;
import com.bank.dao.impl.OperationLogDaoImpl;
import com.bank.dao.impl.TransactionDaoImpl;
import com.bank.dao.impl.UserDaoImpl;
import com.bank.dto.BillPaymentView;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.RiskCheckRequest;
import com.bank.dto.RiskDecision;
import com.bank.dto.ServiceResult;
import com.bank.dto.TransactionResult;
import com.bank.service.AdminAlertService;
import com.bank.service.NotificationService;
import com.bank.service.RiskService;
import com.bank.service.TransactionService;
import com.bank.util.DBUtil;
import com.bank.util.MoneyUtil;
import com.bank.util.PasswordUtil;
import com.bank.util.TransactionNoGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class TransactionServiceImpl implements TransactionService {
    private static final BigDecimal MAX_DEPOSIT_AMOUNT = new BigDecimal("1000000.00");
    private static final BigDecimal MAX_WITHDRAW_AMOUNT = new BigDecimal("50000.00");
    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("200000.00");
    private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("20000.00");
    private static final int LEDGER_QUERY_LIMIT = 100;
    private static final int PAYMENT_QUERY_LIMIT = 100;

    private final UserDao userDao = new UserDaoImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final LedgerDao ledgerDao = new LedgerDaoImpl();
    private final OperationLogDao operationLogDao = new OperationLogDaoImpl();
    private final BillPaymentDao billPaymentDao = new BillPaymentDaoImpl();
    private final RiskService riskService = new RiskServiceImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();

    @Override
    public ServiceResult<TransactionResult> deposit(long customerId, long userId, long accountId,
                                                    String amountText, String remark, String ipAddress) {
        BigDecimal amount;
        try {
            amount = MoneyUtil.parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (!MoneyUtil.isPositive(amount)) {
            return ServiceResult.failure("存款金额必须大于 0。");
        }
        if (amount.compareTo(MAX_DEPOSIT_AMOUNT) > 0) {
            return ServiceResult.failure("单笔模拟存款不能超过 1,000,000.00。");
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            Account account = accountDao.findByIdForUpdate(connection, accountId);
            if (account == null || !Long.valueOf(customerId).equals(account.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("账户不存在或不属于当前客户。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("账户状态不可用，不能办理存款。");
            }

            BigDecimal balanceAfter = account.getAvailableBalance().add(amount).setScale(2);
            accountDao.updateAvailableBalance(connection, accountId, balanceAfter);

            String transactionNo = TransactionNoGenerator.generate();
            String safeRemark = trimToLength(remark, 255);
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setTransactionNo(transactionNo);
            transactionRecord.setCustomerId(customerId);
            transactionRecord.setToAccountId(accountId);
            transactionRecord.setTxnType("DEPOSIT");
            transactionRecord.setAmount(amount);
            transactionRecord.setStatus("SUCCESS");
            transactionRecord.setRiskScore(0);
            transactionRecord.setRemark(safeRemark);
            long transactionId = transactionDao.insert(connection, transactionRecord);

            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setTransactionId(transactionId);
            ledgerEntry.setAccountId(accountId);
            ledgerEntry.setDirection("IN");
            ledgerEntry.setAmount(amount);
            ledgerEntry.setBalanceAfter(balanceAfter);
            ledgerEntry.setSummary("模拟存款入账");
            ledgerDao.insert(connection, ledgerEntry);

            operationLogDao.insert(connection, userId, "DEPOSIT", transactionNo,
                    "模拟存款，账户：" + account.getAccountNo() + "，金额：" + amount.toPlainString(),
                    ipAddress);
            notificationService.create(connection, customerId, userId, "TRANSACTION", "存款成功",
                    "账户 " + account.getAccountNo() + " 已入账 ¥" + amount.toPlainString()
                            + "，交易后余额 ¥" + balanceAfter.toPlainString() + "。",
                    "DEPOSIT", transactionNo);

            connection.commit();
            return ServiceResult.success("存款成功。", buildDepositResult(transactionNo, account, amount,
                    balanceAfter, safeRemark));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("存款失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<TransactionResult> withdraw(long customerId, long userId, long accountId,
                                                     String amountText, String payPassword, String remark,
                                                     String ipAddress) {
        BigDecimal amount;
        try {
            amount = MoneyUtil.parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (!MoneyUtil.isPositive(amount)) {
            return ServiceResult.failure("取款金额必须大于 0。");
        }
        if (amount.compareTo(MAX_WITHDRAW_AMOUNT) > 0) {
            return ServiceResult.failure("单笔模拟取款不能超过 50,000.00。");
        }
        if (payPassword == null || !payPassword.matches("\\d{6}")) {
            return ServiceResult.failure("请输入 6 位支付密码。");
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            User user = userDao.findById(connection, userId);
            if (user == null) {
                connection.rollback();
                return ServiceResult.failure("用户不存在，请重新登录。");
            }
            if (user.getPayPasswordHash() == null || user.getPayPasswordHash().trim().length() == 0) {
                connection.rollback();
                return ServiceResult.failure("请先设置支付密码。");
            }
            if (!PasswordUtil.matches(payPassword, user.getPayPasswordHash())) {
                connection.rollback();
                return ServiceResult.failure("支付密码错误。");
            }

            Account account = accountDao.findByIdForUpdate(connection, accountId);
            if (account == null || !Long.valueOf(customerId).equals(account.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("账户不存在或不属于当前客户。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("账户状态不可用，不能办理取款。");
            }
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                connection.rollback();
                return ServiceResult.failure("账户可用余额不足。");
            }

            String transactionNo = TransactionNoGenerator.generate();
            ServiceResult<RiskDecision> riskResult = evaluateRisk(connection, customerId, accountId, null,
                    transactionNo, "WITHDRAW", amount, ipAddress);
            if (!riskResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(riskResult.getMessage());
            }
            RiskDecision riskDecision = riskResult.getData();
            if (riskDecision.isBlocked()) {
                notifyRiskBlocked(connection, customerId, userId, "取款被风控拦截", "WITHDRAW", transactionNo,
                        amount, riskDecision);
                connection.commit();
                return ServiceResult.failure("交易被风控拦截：" + riskDecision.getReason());
            }

            BigDecimal balanceAfter = account.getAvailableBalance().subtract(amount).setScale(2);
            accountDao.updateAvailableBalance(connection, accountId, balanceAfter);

            String safeRemark = trimToLength(remark, 255);
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setTransactionNo(transactionNo);
            transactionRecord.setCustomerId(customerId);
            transactionRecord.setFromAccountId(accountId);
            transactionRecord.setTxnType("WITHDRAW");
            transactionRecord.setAmount(amount);
            transactionRecord.setStatus("SUCCESS");
            transactionRecord.setRiskScore(riskDecision.getRiskScore());
            transactionRecord.setRemark(safeRemark);
            long transactionId = transactionDao.insert(connection, transactionRecord);

            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setTransactionId(transactionId);
            ledgerEntry.setAccountId(accountId);
            ledgerEntry.setDirection("OUT");
            ledgerEntry.setAmount(amount);
            ledgerEntry.setBalanceAfter(balanceAfter);
            ledgerEntry.setSummary("模拟现金取款");
            ledgerDao.insert(connection, ledgerEntry);

            operationLogDao.insert(connection, userId, "WITHDRAW", transactionNo,
                    "模拟取款，账户：" + account.getAccountNo() + "，金额：" + amount.toPlainString(),
                    ipAddress);
            notificationService.create(connection, customerId, userId, "TRANSACTION", "取款成功",
                    "账户 " + account.getAccountNo() + " 已支出 ¥" + amount.toPlainString()
                            + "，交易后余额 ¥" + balanceAfter.toPlainString() + "。",
                    "WITHDRAW", transactionNo);
            notifyRiskWarnIfNeeded(connection, customerId, userId, "取款触发风控预警", "WITHDRAW",
                    transactionNo, amount, riskDecision);

            connection.commit();
            return ServiceResult.success("取款成功。", buildWithdrawResult(transactionNo, account, amount,
                    balanceAfter, safeRemark));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("取款失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<TransactionResult> innerTransfer(long customerId, long userId, long fromAccountId,
                                                          String toAccountNo, String amountText,
                                                          String payPassword, String remark, String ipAddress) {
        String normalizedToAccountNo = toAccountNo == null ? "" : toAccountNo.trim();
        if (normalizedToAccountNo.length() == 0) {
            return ServiceResult.failure("请输入收款账号。");
        }

        BigDecimal amount;
        try {
            amount = MoneyUtil.parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (!MoneyUtil.isPositive(amount)) {
            return ServiceResult.failure("转账金额必须大于 0。");
        }
        if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            return ServiceResult.failure("单笔本行转账不能超过 200,000.00。");
        }
        if (payPassword == null || !payPassword.matches("\\d{6}")) {
            return ServiceResult.failure("请输入 6 位支付密码。");
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            User user = userDao.findById(connection, userId);
            if (user == null) {
                connection.rollback();
                return ServiceResult.failure("用户不存在，请重新登录。");
            }
            if (user.getPayPasswordHash() == null || user.getPayPasswordHash().trim().length() == 0) {
                connection.rollback();
                return ServiceResult.failure("请先设置支付密码。");
            }
            if (!PasswordUtil.matches(payPassword, user.getPayPasswordHash())) {
                connection.rollback();
                return ServiceResult.failure("支付密码错误。");
            }

            Account lookupFrom = accountDao.findById(connection, fromAccountId);
            if (lookupFrom == null || !Long.valueOf(customerId).equals(lookupFrom.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("付款账户不存在或不属于当前客户。");
            }

            Account lookupTo = accountDao.findByAccountNo(connection, normalizedToAccountNo);
            if (lookupTo == null) {
                connection.rollback();
                return ServiceResult.failure("收款账户不存在。");
            }
            if (Long.valueOf(fromAccountId).equals(lookupTo.getAccountId())) {
                connection.rollback();
                return ServiceResult.failure("不能向同一个账户转账。");
            }

            Account firstLocked;
            Account secondLocked;
            if (fromAccountId < lookupTo.getAccountId()) {
                firstLocked = accountDao.findByIdForUpdate(connection, fromAccountId);
                secondLocked = accountDao.findByIdForUpdate(connection, lookupTo.getAccountId());
            } else {
                firstLocked = accountDao.findByIdForUpdate(connection, lookupTo.getAccountId());
                secondLocked = accountDao.findByIdForUpdate(connection, fromAccountId);
            }
            if (firstLocked == null || secondLocked == null) {
                connection.rollback();
                return ServiceResult.failure("账户状态已变化，请刷新后重试。");
            }

            Account fromAccount = Long.valueOf(fromAccountId).equals(firstLocked.getAccountId())
                    ? firstLocked : secondLocked;
            Account toAccount = lookupTo.getAccountId().equals(firstLocked.getAccountId())
                    ? firstLocked : secondLocked;

            if (!Long.valueOf(customerId).equals(fromAccount.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("付款账户不属于当前客户。");
            }
            if (!"NORMAL".equals(fromAccount.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("付款账户状态不可用，不能转账。");
            }
            if (!"NORMAL".equals(toAccount.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("收款账户状态不可用，不能转账。");
            }
            if (fromAccount.getAvailableBalance().compareTo(amount) < 0) {
                connection.rollback();
                return ServiceResult.failure("付款账户可用余额不足。");
            }

            String transactionNo = TransactionNoGenerator.generate();
            ServiceResult<RiskDecision> riskResult = evaluateRisk(connection, customerId, fromAccount.getAccountId(),
                    toAccount.getAccountId(), transactionNo, "TRANSFER_INNER", amount, ipAddress);
            if (!riskResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(riskResult.getMessage());
            }
            RiskDecision riskDecision = riskResult.getData();
            if (riskDecision.isBlocked()) {
                notifyRiskBlocked(connection, customerId, userId, "转账被风控拦截", "TRANSFER_INNER", transactionNo,
                        amount, riskDecision);
                connection.commit();
                return ServiceResult.failure("交易被风控拦截：" + riskDecision.getReason());
            }

            BigDecimal fromBalanceAfter = fromAccount.getAvailableBalance().subtract(amount).setScale(2);
            BigDecimal toBalanceAfter = toAccount.getAvailableBalance().add(amount).setScale(2);
            accountDao.updateAvailableBalance(connection, fromAccount.getAccountId(), fromBalanceAfter);
            accountDao.updateAvailableBalance(connection, toAccount.getAccountId(), toBalanceAfter);

            String safeRemark = trimToLength(remark, 255);
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setTransactionNo(transactionNo);
            transactionRecord.setCustomerId(customerId);
            transactionRecord.setFromAccountId(fromAccount.getAccountId());
            transactionRecord.setToAccountId(toAccount.getAccountId());
            transactionRecord.setTxnType("TRANSFER_INNER");
            transactionRecord.setAmount(amount);
            transactionRecord.setStatus("SUCCESS");
            transactionRecord.setRiskScore(riskDecision.getRiskScore());
            transactionRecord.setRemark(safeRemark);
            long transactionId = transactionDao.insert(connection, transactionRecord);

            LedgerEntry outEntry = new LedgerEntry();
            outEntry.setTransactionId(transactionId);
            outEntry.setAccountId(fromAccount.getAccountId());
            outEntry.setDirection("OUT");
            outEntry.setAmount(amount);
            outEntry.setBalanceAfter(fromBalanceAfter);
            outEntry.setSummary("本行转账转出至 " + toAccount.getAccountNo());
            ledgerDao.insert(connection, outEntry);

            LedgerEntry inEntry = new LedgerEntry();
            inEntry.setTransactionId(transactionId);
            inEntry.setAccountId(toAccount.getAccountId());
            inEntry.setDirection("IN");
            inEntry.setAmount(amount);
            inEntry.setBalanceAfter(toBalanceAfter);
            inEntry.setSummary("本行转账入账自 " + fromAccount.getAccountNo());
            ledgerDao.insert(connection, inEntry);

            operationLogDao.insert(connection, userId, "TRANSFER_INNER", transactionNo,
                    "本行转账，付款账户：" + fromAccount.getAccountNo()
                            + "，收款账户：" + toAccount.getAccountNo()
                            + "，金额：" + amount.toPlainString(),
                    ipAddress);
            notificationService.create(connection, customerId, userId, "TRANSACTION", "转账成功",
                    "账户 " + fromAccount.getAccountNo() + " 已向 " + toAccount.getAccountNo()
                            + " 转出 ¥" + amount.toPlainString() + "。",
                    "TRANSFER_INNER", transactionNo);
            notifyRiskWarnIfNeeded(connection, customerId, userId, "转账触发风控预警", "TRANSFER_INNER",
                    transactionNo, amount, riskDecision);

            connection.commit();
            return ServiceResult.success("转账成功。", buildTransferResult(transactionNo, fromAccount, toAccount,
                    amount, fromBalanceAfter, safeRemark));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("转账失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<TransactionResult> payBill(long customerId, long userId, long accountId,
                                                    String paymentType, String payerNo, String billingMonth,
                                                    String amountText, String payPassword, String remark,
                                                    String ipAddress) {
        String normalizedPaymentType = normalizePaymentType(paymentType);
        if (normalizedPaymentType == null) {
            return ServiceResult.failure("请选择正确的缴费类型。");
        }
        String normalizedPayerNo = payerNo == null ? "" : payerNo.trim();
        if (normalizedPayerNo.length() == 0 || normalizedPayerNo.length() > 40) {
            return ServiceResult.failure("请输入有效的户号或手机号。");
        }
        if ("MOBILE".equals(normalizedPaymentType) && !normalizedPayerNo.matches("1\\d{10}")) {
            return ServiceResult.failure("话费充值请输入 11 位手机号。");
        }
        String normalizedBillingMonth = billingMonth == null ? "" : billingMonth.trim();
        if (!normalizedBillingMonth.matches("\\d{4}-\\d{2}")) {
            return ServiceResult.failure("账期格式应为 YYYY-MM。");
        }

        BigDecimal amount;
        try {
            amount = MoneyUtil.parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (!MoneyUtil.isPositive(amount)) {
            return ServiceResult.failure("缴费金额必须大于 0。");
        }
        if (amount.compareTo(MAX_PAYMENT_AMOUNT) > 0) {
            return ServiceResult.failure("单笔缴费不能超过 20,000.00。");
        }
        if (payPassword == null || !payPassword.matches("\\d{6}")) {
            return ServiceResult.failure("请输入 6 位支付密码。");
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            User user = userDao.findById(connection, userId);
            if (user == null) {
                connection.rollback();
                return ServiceResult.failure("用户不存在，请重新登录。");
            }
            if (user.getPayPasswordHash() == null || user.getPayPasswordHash().trim().length() == 0) {
                connection.rollback();
                return ServiceResult.failure("请先设置支付密码。");
            }
            if (!PasswordUtil.matches(payPassword, user.getPayPasswordHash())) {
                connection.rollback();
                return ServiceResult.failure("支付密码错误。");
            }

            Account account = accountDao.findByIdForUpdate(connection, accountId);
            if (account == null || !Long.valueOf(customerId).equals(account.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("付款账户不存在或不属于当前客户。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("付款账户状态不可用，不能缴费。");
            }
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                connection.rollback();
                return ServiceResult.failure("付款账户可用余额不足。");
            }

            String transactionNo = TransactionNoGenerator.generate();
            ServiceResult<RiskDecision> riskResult = evaluateRisk(connection, customerId, account.getAccountId(), null,
                    transactionNo, "PAYMENT", amount, ipAddress);
            if (!riskResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(riskResult.getMessage());
            }
            RiskDecision riskDecision = riskResult.getData();
            if (riskDecision.isBlocked()) {
                notifyRiskBlocked(connection, customerId, userId, "缴费被风控拦截", "PAYMENT", transactionNo,
                        amount, riskDecision);
                connection.commit();
                return ServiceResult.failure("交易被风控拦截：" + riskDecision.getReason());
            }

            BigDecimal balanceAfter = account.getAvailableBalance().subtract(amount).setScale(2);
            accountDao.updateAvailableBalance(connection, account.getAccountId(), balanceAfter);

            String safeRemark = trimToLength(remark, 255);
            String institutionName = institutionNameFor(normalizedPaymentType);
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setTransactionNo(transactionNo);
            transactionRecord.setCustomerId(customerId);
            transactionRecord.setFromAccountId(account.getAccountId());
            transactionRecord.setTxnType("PAYMENT");
            transactionRecord.setAmount(amount);
            transactionRecord.setStatus("SUCCESS");
            transactionRecord.setRiskScore(riskDecision.getRiskScore());
            transactionRecord.setRemark(safeRemark);
            long transactionId = transactionDao.insert(connection, transactionRecord);

            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setTransactionId(transactionId);
            ledgerEntry.setAccountId(account.getAccountId());
            ledgerEntry.setDirection("OUT");
            ledgerEntry.setAmount(amount);
            ledgerEntry.setBalanceAfter(balanceAfter);
            ledgerEntry.setSummary(paymentTypeName(normalizedPaymentType) + "缴费");
            ledgerDao.insert(connection, ledgerEntry);

            BillPayment billPayment = new BillPayment();
            billPayment.setTransactionId(transactionId);
            billPayment.setCustomerId(customerId);
            billPayment.setAccountId(account.getAccountId());
            billPayment.setPaymentType(normalizedPaymentType);
            billPayment.setInstitutionName(institutionName);
            billPayment.setPayerNo(normalizedPayerNo);
            billPayment.setBillingMonth(normalizedBillingMonth);
            billPayment.setAmount(amount);
            billPayment.setStatus("SUCCESS");
            billPaymentDao.insert(connection, billPayment);

            operationLogDao.insert(connection, userId, "PAYMENT", transactionNo,
                    paymentTypeName(normalizedPaymentType) + "缴费，账户：" + account.getAccountNo()
                            + "，户号：" + normalizedPayerNo
                            + "，账期：" + normalizedBillingMonth
                            + "，金额：" + amount.toPlainString(),
                    ipAddress);
            notificationService.create(connection, customerId, userId, "TRANSACTION", "缴费成功",
                    paymentTypeName(normalizedPaymentType) + "缴费已完成，账户 " + account.getAccountNo()
                            + " 支出 ¥" + amount.toPlainString() + "。",
                    "PAYMENT", transactionNo);
            notifyRiskWarnIfNeeded(connection, customerId, userId, "缴费触发风控预警", "PAYMENT",
                    transactionNo, amount, riskDecision);

            connection.commit();
            return ServiceResult.success("缴费成功。", buildPaymentResult(transactionNo, account, amount,
                    balanceAfter, normalizedPaymentType, normalizedPayerNo, safeRemark));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("缴费失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<LedgerEntryView>> listLedgerEntries(long customerId, Long accountId,
                                                                  String direction, String txnType) {
        String normalizedDirection = normalizeDirection(direction);
        String normalizedTxnType = normalizeTxnType(txnType);
        try (Connection connection = DBUtil.getConnection()) {
            List<LedgerEntryView> entries = ledgerDao.findByCustomer(connection, customerId, accountId,
                    normalizedDirection, normalizedTxnType, LEDGER_QUERY_LIMIT);
            return ServiceResult.success("流水查询成功。", entries);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("流水查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<BillPaymentView>> listBillPayments(long customerId, String paymentType) {
        String normalizedPaymentType = normalizePaymentType(paymentType);
        try (Connection connection = DBUtil.getConnection()) {
            List<BillPaymentView> records = billPaymentDao.findByCustomer(connection, customerId,
                    normalizedPaymentType, PAYMENT_QUERY_LIMIT);
            return ServiceResult.success("缴费记录查询成功。", records);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("缴费记录查询失败，请检查数据库状态或稍后重试。");
        }
    }

    private TransactionResult buildDepositResult(String transactionNo, Account account, BigDecimal amount,
                                                 BigDecimal balanceAfter, String remark) {
        TransactionResult result = new TransactionResult();
        result.setTransactionNo(transactionNo);
        result.setTxnType("DEPOSIT");
        result.setAccountNo(account.getAccountNo());
        result.setDirection("IN");
        result.setAmount(amount);
        result.setBalanceAfter(balanceAfter);
        result.setStatus("SUCCESS");
        result.setSummary(remark == null || remark.length() == 0 ? "模拟存款入账" : remark);
        result.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return result;
    }

    private TransactionResult buildWithdrawResult(String transactionNo, Account account, BigDecimal amount,
                                                  BigDecimal balanceAfter, String remark) {
        TransactionResult result = new TransactionResult();
        result.setTransactionNo(transactionNo);
        result.setTxnType("WITHDRAW");
        result.setAccountNo(account.getAccountNo());
        result.setDirection("OUT");
        result.setAmount(amount);
        result.setBalanceAfter(balanceAfter);
        result.setStatus("SUCCESS");
        result.setSummary(remark == null || remark.length() == 0 ? "模拟现金取款" : remark);
        result.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return result;
    }

    private TransactionResult buildTransferResult(String transactionNo, Account fromAccount, Account toAccount,
                                                  BigDecimal amount, BigDecimal fromBalanceAfter, String remark) {
        TransactionResult result = new TransactionResult();
        result.setTransactionNo(transactionNo);
        result.setTxnType("TRANSFER_INNER");
        result.setAccountNo(fromAccount.getAccountNo());
        result.setDirection("OUT");
        result.setAmount(amount);
        result.setBalanceAfter(fromBalanceAfter);
        result.setStatus("SUCCESS");
        String defaultSummary = "本行转账至 " + toAccount.getAccountNo();
        result.setSummary(remark == null || remark.length() == 0 ? defaultSummary : remark + "；" + defaultSummary);
        result.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return result;
    }

    private TransactionResult buildPaymentResult(String transactionNo, Account account, BigDecimal amount,
                                                 BigDecimal balanceAfter, String paymentType, String payerNo,
                                                 String remark) {
        TransactionResult result = new TransactionResult();
        result.setTransactionNo(transactionNo);
        result.setTxnType("PAYMENT");
        result.setAccountNo(account.getAccountNo());
        result.setDirection("OUT");
        result.setAmount(amount);
        result.setBalanceAfter(balanceAfter);
        result.setStatus("SUCCESS");
        String defaultSummary = paymentTypeName(paymentType) + "缴费，户号：" + payerNo;
        result.setSummary(remark == null || remark.length() == 0 ? defaultSummary : remark + "；" + defaultSummary);
        result.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return result;
    }

    private String normalizeDirection(String direction) {
        if ("IN".equals(direction) || "OUT".equals(direction)) {
            return direction;
        }
        return null;
    }

    private String normalizeTxnType(String txnType) {
        if ("DEPOSIT".equals(txnType) || "WITHDRAW".equals(txnType) || "TRANSFER_INNER".equals(txnType)
                || "PAYMENT".equals(txnType) || "BUY_WEALTH".equals(txnType)
                || "REDEEM_WEALTH".equals(txnType) || "ACCOUNT_ADJUSTMENT".equals(txnType)) {
            return txnType;
        }
        return null;
    }

    private String normalizePaymentType(String paymentType) {
        if ("WATER".equals(paymentType) || "ELECTRICITY".equals(paymentType)
                || "GAS".equals(paymentType) || "MOBILE".equals(paymentType)) {
            return paymentType;
        }
        return null;
    }

    private String paymentTypeName(String paymentType) {
        if ("WATER".equals(paymentType)) {
            return "水费";
        }
        if ("ELECTRICITY".equals(paymentType)) {
            return "电费";
        }
        if ("GAS".equals(paymentType)) {
            return "燃气费";
        }
        if ("MOBILE".equals(paymentType)) {
            return "话费";
        }
        return "生活";
    }

    private String institutionNameFor(String paymentType) {
        if ("WATER".equals(paymentType)) {
            return "iBank 城市供水模拟机构";
        }
        if ("ELECTRICITY".equals(paymentType)) {
            return "iBank 电力缴费模拟机构";
        }
        if ("GAS".equals(paymentType)) {
            return "iBank 燃气服务模拟机构";
        }
        if ("MOBILE".equals(paymentType)) {
            return "iBank 通信充值模拟机构";
        }
        return "iBank 生活缴费模拟机构";
    }

    private ServiceResult<RiskDecision> evaluateRisk(Connection connection, long customerId, long accountId,
                                                     Long targetAccountId, String transactionNo, String txnType,
                                                     BigDecimal amount, String ipAddress) {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setCustomerId(customerId);
        request.setAccountId(accountId);
        request.setTargetAccountId(targetAccountId);
        request.setTransactionNo(transactionNo);
        request.setTxnType(txnType);
        request.setAmount(amount);
        request.setCustomerRiskLevel(customerRiskLevel(connection, customerId));
        request.setIpAddress(ipAddress);
        return riskService.evaluateAndReserve(connection, request);
    }

    private String customerRiskLevel(Connection connection, long customerId) {
        try {
            Customer customer = customerDao.findById(connection, customerId);
            if (customer != null && customer.getRiskLevel() != null) {
                return customer.getRiskLevel();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "C3";
    }

    private void notifyRiskBlocked(Connection connection, long customerId, long userId, String title,
                                   String businessType, String transactionNo, BigDecimal amount,
                                   RiskDecision riskDecision) throws SQLException {
        notificationService.create(connection, customerId, userId, "RISK", title,
                "交易金额 ¥" + amount.toPlainString() + "，已被风控拦截。原因：" + riskDecision.getReason(),
                businessType, transactionNo);
        adminAlertService.create(connection, "RISK_BLOCK", "HIGH", title,
                "客户 " + customerId + " 的 " + businessType + " 交易 " + transactionNo
                        + " 已被风控拦截，金额 ¥" + amount.toPlainString()
                        + "，原因：" + riskDecision.getReason(),
                businessType, transactionNo, "RISK_OPERATOR");
    }

    private void notifyRiskWarnIfNeeded(Connection connection, long customerId, long userId, String title,
                                        String businessType, String transactionNo, BigDecimal amount,
                                        RiskDecision riskDecision) throws SQLException {
        if (riskDecision != null && "WARN".equals(riskDecision.getDecision())) {
            notificationService.create(connection, customerId, userId, "RISK", title,
                    "交易金额 ¥" + amount.toPlainString() + "，已通过但命中风控预警。原因："
                            + riskDecision.getReason(),
                    businessType, transactionNo);
            adminAlertService.create(connection, "RISK_WARN", "WARNING", title,
                    "客户 " + customerId + " 的 " + businessType + " 交易 " + transactionNo
                            + " 已通过但命中风控预警，金额 ¥" + amount.toPlainString()
                            + "，原因：" + riskDecision.getReason(),
                    businessType, transactionNo, "RISK_OPERATOR");
        }
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return null;
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

    private void restoreAutoCommit(Connection connection, boolean oldAutoCommit) {
        if (connection != null) {
            try {
                connection.setAutoCommit(oldAutoCommit);
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
