package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.bean.Customer;
import com.bank.bean.LedgerEntry;
import com.bank.bean.RiskAssessment;
import com.bank.bean.TransactionRecord;
import com.bank.bean.User;
import com.bank.bean.WealthHolding;
import com.bank.bean.WealthOrder;
import com.bank.bean.WealthOrderConfirm;
import com.bank.bean.WealthProduct;
import com.bank.dao.AccountDao;
import com.bank.dao.CustomerDao;
import com.bank.dao.LedgerDao;
import com.bank.dao.OperationLogDao;
import com.bank.dao.RiskAssessmentDao;
import com.bank.dao.TransactionDao;
import com.bank.dao.UserDao;
import com.bank.dao.WealthDao;
import com.bank.dao.impl.AccountDaoImpl;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dao.impl.LedgerDaoImpl;
import com.bank.dao.impl.OperationLogDaoImpl;
import com.bank.dao.impl.RiskAssessmentDaoImpl;
import com.bank.dao.impl.TransactionDaoImpl;
import com.bank.dao.impl.UserDaoImpl;
import com.bank.dao.impl.WealthDaoImpl;
import com.bank.dto.RiskCheckRequest;
import com.bank.dto.RiskDecision;
import com.bank.dto.ServiceResult;
import com.bank.dto.TransactionResult;
import com.bank.dto.WealthHoldingView;
import com.bank.dto.WealthOrderView;
import com.bank.dto.WealthPurchasePreview;
import com.bank.dto.WealthSettlementSummary;
import com.bank.service.NotificationService;
import com.bank.service.RiskService;
import com.bank.service.WealthService;
import com.bank.util.DBUtil;
import com.bank.util.MoneyUtil;
import com.bank.util.PasswordUtil;
import com.bank.util.TransactionNoGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class WealthServiceImpl implements WealthService {
    private static final long RISK_PROFILE_VALID_DAYS = 365L;
    private static final String DISCLOSURE_VERSION = "WEALTH_DISCLOSURE_2026_V1";
    private static final String ORDER_TYPE_BUY = "BUY";
    private static final String ORDER_TYPE_REDEEM = "REDEEM";
    private static final String STATUS_BUY_PENDING_CONFIRM = "BUY_PENDING_CONFIRM";
    private static final String STATUS_BUY_CONFIRMED = "BUY_CONFIRMED";
    private static final String STATUS_REDEEM_PENDING_ARRIVAL = "REDEEM_PENDING_ARRIVAL";
    private static final String STATUS_REDEEM_COMPLETED = "REDEEM_COMPLETED";
    private static final int SETTLEMENT_BATCH_LIMIT = 200;

    private final WealthDao wealthDao = new WealthDaoImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();
    private final UserDao userDao = new UserDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final TransactionDao transactionDao = new TransactionDaoImpl();
    private final LedgerDao ledgerDao = new LedgerDaoImpl();
    private final OperationLogDao operationLogDao = new OperationLogDaoImpl();
    private final RiskAssessmentDao riskAssessmentDao = new RiskAssessmentDaoImpl();
    private final RiskService riskService = new RiskServiceImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();

    @Override
    public ServiceResult<List<WealthProduct>> listProducts() {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("理财产品查询成功。", wealthDao.findProducts(connection, "ON_SALE"));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("理财产品查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<WealthProduct> getProduct(long productId) {
        try (Connection connection = DBUtil.getConnection()) {
            WealthProduct product = wealthDao.findProductById(connection, productId);
            if (product == null) {
                return ServiceResult.failure("理财产品不存在。");
            }
            return ServiceResult.success("理财产品查询成功。", product);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("理财产品查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<WealthPurchasePreview> previewPurchase(long customerId, long productId,
                                                                long accountId, String amountText) {
        BigDecimal amount;
        try {
            amount = MoneyUtil.parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (!MoneyUtil.isPositive(amount)) {
            return ServiceResult.failure("申购金额必须大于 0。");
        }

        try (Connection connection = DBUtil.getConnection()) {
            Customer customer = customerDao.findById(connection, customerId);
            if (customer == null) {
                return ServiceResult.failure("客户档案不存在，请重新登录。");
            }
            ServiceResult<Void> riskProfileResult = validateRiskProfile(connection, customer);
            if (!riskProfileResult.isSuccess()) {
                return ServiceResult.failure(riskProfileResult.getMessage());
            }

            WealthProduct product = wealthDao.findProductById(connection, productId);
            if (product == null || !"ON_SALE".equals(product.getStatus())) {
                return ServiceResult.failure("理财产品不存在或已下架。");
            }
            if (!riskMatched(customer.getRiskLevel(), product.getRiskLevel())) {
                return ServiceResult.failure("您的风险等级为 " + customer.getRiskLevel()
                        + "，不匹配该产品风险等级 " + product.getRiskLevel() + "。");
            }
            if (amount.compareTo(product.getMinAmount()) < 0) {
                return ServiceResult.failure("申购金额低于产品起购金额。");
            }
            if (amount.compareTo(product.getMaxAmount()) > 0) {
                return ServiceResult.failure("申购金额超过产品购买上限。");
            }

            Account account = accountDao.findById(connection, accountId);
            if (account == null || !Long.valueOf(customerId).equals(account.getCustomerId())) {
                return ServiceResult.failure("付款账户不存在或不属于当前客户。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                return ServiceResult.failure("付款账户状态不可用，不能申购理财。");
            }
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                return ServiceResult.failure("付款账户可用余额不足。");
            }

            WealthPurchasePreview preview = new WealthPurchasePreview();
            preview.setProduct(product);
            preview.setAccount(account);
            preview.setAmount(amount);
            preview.setCustomerRiskLevel(customer.getRiskLevel());
            preview.setProductRiskLevel(product.getRiskLevel());
            preview.setMatchResult(matchResult(customer.getRiskLevel(), product.getRiskLevel()));
            preview.setDisclosureVersion(DISCLOSURE_VERSION);
            return ServiceResult.success("申购确认信息生成成功。", preview);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("申购确认信息生成失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<TransactionResult> buyProduct(long customerId, long userId, long productId,
                                                       long accountId, String amountText, String payPassword,
                                                       String ipAddress) {
        return confirmBuyProduct(customerId, userId, productId, accountId, amountText, payPassword,
                true, true, true, true, ipAddress);
    }

    @Override
    public ServiceResult<TransactionResult> confirmBuyProduct(long customerId, long userId, long productId,
                                                              long accountId, String amountText, String payPassword,
                                                              boolean productDisclosureChecked,
                                                              boolean nonDepositChecked,
                                                              boolean yieldNotGuaranteedChecked,
                                                              boolean accountConfirmed,
                                                              String ipAddress) {
        if (!productDisclosureChecked || !nonDepositChecked || !yieldNotGuaranteedChecked || !accountConfirmed) {
            return ServiceResult.failure("请完整勾选风险揭示和申购确认后再提交。");
        }
        BigDecimal amount;
        try {
            amount = MoneyUtil.parseAmount(amountText);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        if (!MoneyUtil.isPositive(amount)) {
            return ServiceResult.failure("申购金额必须大于 0。");
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
            ServiceResult<Void> passwordResult = verifyPayPassword(user, payPassword);
            if (!passwordResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(passwordResult.getMessage());
            }

            Customer customer = customerDao.findById(connection, customerId);
            if (customer == null) {
                connection.rollback();
                return ServiceResult.failure("客户档案不存在，请重新登录。");
            }
            ServiceResult<Void> riskProfileResult = validateRiskProfile(connection, customer);
            if (!riskProfileResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(riskProfileResult.getMessage());
            }

            WealthProduct product = wealthDao.findProductById(connection, productId);
            if (product == null || !"ON_SALE".equals(product.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("理财产品不存在或已下架。");
            }
            if (!riskMatched(customer.getRiskLevel(), product.getRiskLevel())) {
                connection.rollback();
                return ServiceResult.failure("您的风险等级为 " + customer.getRiskLevel()
                        + "，不匹配该产品风险等级 " + product.getRiskLevel() + "。");
            }
            if (amount.compareTo(product.getMinAmount()) < 0) {
                connection.rollback();
                return ServiceResult.failure("申购金额低于产品起购金额。");
            }
            if (amount.compareTo(product.getMaxAmount()) > 0) {
                connection.rollback();
                return ServiceResult.failure("申购金额超过产品购买上限。");
            }

            Account account = accountDao.findByIdForUpdate(connection, accountId);
            if (account == null || !Long.valueOf(customerId).equals(account.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("付款账户不存在或不属于当前客户。");
            }
            if (!"NORMAL".equals(account.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("付款账户状态不可用，不能申购理财。");
            }
            if (account.getAvailableBalance().compareTo(amount) < 0) {
                connection.rollback();
                return ServiceResult.failure("付款账户可用余额不足。");
            }

            String transactionNo = TransactionNoGenerator.generate();
            ServiceResult<RiskDecision> riskResult = evaluateRisk(connection, customer, accountId, transactionNo,
                    amount, ipAddress);
            if (!riskResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(riskResult.getMessage());
            }
            RiskDecision riskDecision = riskResult.getData();
            if (riskDecision.isBlocked()) {
                notifyRiskBlocked(connection, customerId, userId, "理财申购被风控拦截", transactionNo, amount,
                        riskDecision);
                connection.commit();
                return ServiceResult.failure("交易被风控拦截：" + riskDecision.getReason());
            }

            BigDecimal frozenBefore = account.getFrozenBalance() == null
                    ? BigDecimal.ZERO.setScale(2)
                    : account.getFrozenBalance();
            BigDecimal balanceAfter = account.getAvailableBalance().subtract(amount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal frozenAfter = frozenBefore.add(amount).setScale(2, RoundingMode.HALF_UP);
            accountDao.updateBalances(connection, accountId, balanceAfter, frozenAfter);

            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setTransactionNo(transactionNo);
            transactionRecord.setCustomerId(customerId);
            transactionRecord.setFromAccountId(accountId);
            transactionRecord.setTxnType("BUY_WEALTH");
            transactionRecord.setAmount(amount);
            transactionRecord.setStatus("PENDING_CONFIRM");
            transactionRecord.setRiskScore(riskDecision.getRiskScore());
            transactionRecord.setRemark("理财申购已提交，资金已冻结，等待清算确认：" + product.getProductName());
            long transactionId = transactionDao.insert(connection, transactionRecord);

            WealthOrder order = new WealthOrder();
            order.setOrderNo(transactionNo);
            order.setCustomerId(customerId);
            order.setAccountId(accountId);
            order.setProductId(productId);
            order.setTransactionId(transactionId);
            order.setOrderType(ORDER_TYPE_BUY);
            order.setAmount(amount);
            order.setConfirmedAmount(null);
            order.setIncomeAmount(BigDecimal.ZERO.setScale(2));
            order.setStatus(STATUS_BUY_PENDING_CONFIRM);
            order.setSubmitTime(new Timestamp(System.currentTimeMillis()));
            wealthDao.insertOrder(connection, order);

            WealthOrderConfirm confirm = new WealthOrderConfirm();
            confirm.setTransactionId(transactionId);
            confirm.setCustomerId(customerId);
            confirm.setAccountId(accountId);
            confirm.setProductId(productId);
            confirm.setAmount(amount);
            confirm.setCustomerRiskLevel(customer.getRiskLevel());
            confirm.setProductRiskLevel(product.getRiskLevel());
            confirm.setMatchResult(matchResult(customer.getRiskLevel(), product.getRiskLevel()));
            confirm.setDisclosureVersion(DISCLOSURE_VERSION);
            confirm.setProductDisclosureChecked(productDisclosureChecked);
            confirm.setNonDepositChecked(nonDepositChecked);
            confirm.setYieldNotGuaranteedChecked(yieldNotGuaranteedChecked);
            confirm.setAccountConfirmed(accountConfirmed);
            confirm.setIpAddress(ipAddress);
            wealthDao.insertOrderConfirm(connection, confirm);

            operationLogDao.insert(connection, userId, "BUY_WEALTH", transactionNo,
                    "提交理财申购，产品：" + product.getProductName() + "，金额：" + amount.toPlainString()
                            + "，资金已冻结等待清算确认",
                    ipAddress);
            notificationService.create(connection, customerId, userId, "WEALTH", "理财申购已提交",
                    "您已提交 " + product.getProductName() + " 申购，金额 ¥" + amount.toPlainString()
                            + "，付款账户 " + account.getAccountNo() + " 的资金已冻结，后台清算确认后生成持仓。",
                    "BUY_WEALTH", transactionNo);
            notifyRiskWarnIfNeeded(connection, customerId, userId, "理财申购触发风控预警", transactionNo,
                    amount, riskDecision);

            connection.commit();
            return ServiceResult.success("理财申购已提交，资金已冻结，等待清算确认。", buildResult(transactionNo, "BUY_WEALTH",
                    account.getAccountNo(), "FREEZE", amount, balanceAfter,
                    "申购已提交：" + product.getProductName() + "，后台确认后才生成理财持仓。"));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("理财申购失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<WealthHoldingView>> listHoldings(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            List<WealthHoldingView> holdings = wealthDao.findHoldingsByCustomer(connection, customerId);
            for (WealthHoldingView holding : holdings) {
                enrichIncome(holding);
            }
            return ServiceResult.success("理财持仓查询成功。", holdings);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("理财持仓查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<TransactionResult> redeemHolding(long customerId, long userId, long holdingId,
                                                          String payPassword, String ipAddress) {
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
            ServiceResult<Void> passwordResult = verifyPayPassword(user, payPassword);
            if (!passwordResult.isSuccess()) {
                connection.rollback();
                return ServiceResult.failure(passwordResult.getMessage());
            }

            WealthHolding holding = wealthDao.findHoldingByIdForUpdate(connection, holdingId);
            if (holding == null || !Long.valueOf(customerId).equals(holding.getCustomerId())) {
                connection.rollback();
                return ServiceResult.failure("持仓不存在或不属于当前客户。");
            }
            if (!"HOLDING".equals(holding.getStatus())) {
                connection.rollback();
                return ServiceResult.failure("该持仓当前不可赎回。");
            }

            WealthProduct product = wealthDao.findProductById(connection, holding.getProductId());
            Account account = accountDao.findByIdForUpdate(connection, holding.getAccountId());
            if (product == null || account == null) {
                connection.rollback();
                return ServiceResult.failure("持仓关联的产品或账户不存在。");
            }

            LocalDate today = LocalDate.now();
            LocalDate maturityDate = holding.getMaturityDate() == null
                    ? holding.getBuyTime().toLocalDateTime().toLocalDate().plusDays(product.getPeriodDays())
                    : holding.getMaturityDate().toLocalDate();
            if (!product.isAllowEarlyRedeem() && today.isBefore(maturityDate)) {
                connection.rollback();
                return ServiceResult.failure("该产品为封闭式理财，未到期前不可赎回。预计到期日：" + maturityDate + "。");
            }

            BigDecimal income = calculateIncome(holding.getPrincipal(), holding.getExpectedRate(), holding.getBuyTime());
            BigDecimal redeemAmount = holding.getPrincipal().add(income).setScale(2, RoundingMode.HALF_UP);
            LocalDate expectedArrival = today.plusDays(Math.max(0, product.getArrivalDays()));
            String orderNo = TransactionNoGenerator.generate();

            WealthOrder order = new WealthOrder();
            order.setOrderNo(orderNo);
            order.setCustomerId(customerId);
            order.setAccountId(account.getAccountId());
            order.setProductId(product.getProductId());
            order.setHoldingId(holdingId);
            order.setOrderType(ORDER_TYPE_REDEEM);
            order.setAmount(holding.getPrincipal());
            order.setConfirmedAmount(redeemAmount);
            order.setIncomeAmount(income);
            order.setStatus(STATUS_REDEEM_PENDING_ARRIVAL);
            order.setSubmitTime(new Timestamp(System.currentTimeMillis()));
            order.setExpectedArrivalDate(Date.valueOf(expectedArrival));
            long orderId = wealthDao.insertOrder(connection, order);
            wealthDao.markRedeeming(connection, holdingId, orderId);

            operationLogDao.insert(connection, userId, "REDEEM_WEALTH", orderNo,
                    "提交理财赎回，产品：" + product.getProductName()
                            + "，本金：" + holding.getPrincipal().toPlainString()
                            + "，估算收益：" + income.toPlainString()
                            + "，预计到账日：" + expectedArrival,
                    ipAddress);
            notificationService.create(connection, customerId, userId, "WEALTH", "理财赎回已提交",
                    "您已提交 " + product.getProductName() + " 赎回申请，预计到账金额 ¥" + redeemAmount.toPlainString()
                            + "，其中估算收益 ¥" + income.toPlainString()
                            + "。到账日为 " + expectedArrival + "，清算完成后资金才进入账户余额。",
                    "REDEEM_WEALTH", orderNo);

            connection.commit();
            return ServiceResult.success("理财赎回已提交，等待清算到账。", buildResult(orderNo, "REDEEM_WEALTH",
                    account.getAccountNo(), "PENDING_IN", redeemAmount, account.getAvailableBalance(),
                    "赎回已提交：" + product.getProductName() + "，预计到账日 " + expectedArrival + "。"));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("理财赎回失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<WealthOrderView>> listSettlementOrders(String status) {
        String normalizedStatus = isBlank(status) ? null : status.trim();
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("理财订单查询成功。",
                    wealthDao.findRecentOrderViews(connection, normalizedStatus, 100));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("理财订单查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<WealthSettlementSummary> settlementSummary() {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("理财清算指标查询成功。", buildSettlementSummary(connection));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("理财清算指标查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<WealthSettlementSummary> runSettlement(long adminUserId, String action, String ipAddress) {
        String normalizedAction = isBlank(action) ? "all" : action.trim();
        boolean runBuy = "all".equals(normalizedAction) || "confirmBuy".equals(normalizedAction);
        boolean runRedeem = "all".equals(normalizedAction) || "settleRedeem".equals(normalizedAction);
        if (!runBuy && !runRedeem) {
            return ServiceResult.failure("请选择正确的理财清算动作。");
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            WealthSettlementSummary summary = new WealthSettlementSummary();
            if (runBuy) {
                List<WealthOrder> buyOrders = wealthDao.findOrdersByStatusForUpdate(connection, ORDER_TYPE_BUY,
                        STATUS_BUY_PENDING_CONFIRM, SETTLEMENT_BATCH_LIMIT);
                for (WealthOrder order : buyOrders) {
                    confirmBuyOrder(connection, order, adminUserId, ipAddress);
                }
                summary.setConfirmedBuyOrders(buyOrders.size());
            }
            if (runRedeem) {
                List<WealthOrder> redeemOrders = wealthDao.findOrdersByStatusForUpdate(connection, ORDER_TYPE_REDEEM,
                        STATUS_REDEEM_PENDING_ARRIVAL, SETTLEMENT_BATCH_LIMIT);
                for (WealthOrder order : redeemOrders) {
                    settleRedeemOrder(connection, order, adminUserId, ipAddress);
                }
                summary.setSettledRedeemOrders(redeemOrders.size());
            }

            WealthSettlementSummary latest = buildSettlementSummary(connection);
            summary.setPendingBuyOrders(latest.getPendingBuyOrders());
            summary.setRedeemingOrders(latest.getRedeemingOrders());
            connection.commit();
            return ServiceResult.success("理财清算完成。", summary);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("理财清算失败，请检查数据库状态或稍后重试。");
        } finally {
            restoreAutoCommit(connection, oldAutoCommit);
            closeQuietly(connection);
        }
    }

    private void confirmBuyOrder(Connection connection, WealthOrder order, long adminUserId, String ipAddress)
            throws SQLException {
        Account account = accountDao.findByIdForUpdate(connection, order.getAccountId());
        WealthProduct product = wealthDao.findProductById(connection, order.getProductId());
        if (account == null || product == null || order.getTransactionId() == null) {
            throw new SQLException("Invalid wealth buy order: " + order.getOrderNo());
        }
        BigDecimal frozenBefore = account.getFrozenBalance() == null
                ? BigDecimal.ZERO.setScale(2)
                : account.getFrozenBalance();
        if (frozenBefore.compareTo(order.getAmount()) < 0) {
            throw new SQLException("Insufficient frozen balance for wealth order: " + order.getOrderNo());
        }

        BigDecimal frozenAfter = frozenBefore.subtract(order.getAmount()).setScale(2, RoundingMode.HALF_UP);
        accountDao.updateBalances(connection, account.getAccountId(), account.getAvailableBalance(), frozenAfter);
        transactionDao.updateStatus(connection, order.getTransactionId(), "SUCCESS");

        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setTransactionId(order.getTransactionId());
        ledgerEntry.setAccountId(account.getAccountId());
        ledgerEntry.setDirection("OUT");
        ledgerEntry.setAmount(order.getAmount());
        ledgerEntry.setBalanceAfter(account.getAvailableBalance());
        ledgerEntry.setSummary("理财申购确认：" + product.getProductName());
        ledgerDao.insert(connection, ledgerEntry);

        LocalDate valueDate = LocalDate.now().plusDays(Math.max(0, product.getConfirmDays()));
        LocalDate maturityDate = valueDate.plusDays(product.getPeriodDays());
        WealthHolding holding = new WealthHolding();
        holding.setCustomerId(order.getCustomerId());
        holding.setAccountId(account.getAccountId());
        holding.setProductId(product.getProductId());
        holding.setBuyTransactionId(order.getTransactionId());
        holding.setBuyOrderId(order.getOrderId());
        holding.setPrincipal(order.getAmount());
        holding.setExpectedRate(product.getExpectedRate());
        holding.setBuyTime(new Timestamp(System.currentTimeMillis()));
        holding.setValueDate(Date.valueOf(valueDate));
        holding.setMaturityDate(Date.valueOf(maturityDate));
        holding.setCurrentIncome(BigDecimal.ZERO.setScale(2));
        holding.setEstimatedValue(order.getAmount());
        holding.setStatus("HOLDING");
        long holdingId = wealthDao.insertHolding(connection, holding);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        wealthDao.markBuyOrderConfirmed(connection, order.getOrderId(), holdingId, now,
                Date.valueOf(valueDate), Date.valueOf(maturityDate));

        Customer customer = customerDao.findById(connection, order.getCustomerId());
        Long userId = customer == null ? null : customer.getUserId();
        notificationService.create(connection, order.getCustomerId(), userId, "WEALTH", "理财申购已确认",
                "您申购的 " + product.getProductName() + " 已确认，起息日 " + valueDate
                        + "，到期日 " + maturityDate + "。资金已从冻结转为理财持仓。",
                "BUY_WEALTH", order.getOrderNo());
        operationLogDao.insert(connection, adminUserId, "WEALTH_SETTLEMENT_BUY", order.getOrderNo(),
                "确认理财申购，生成持仓：" + product.getProductName() + "，金额：" + order.getAmount().toPlainString(),
                ipAddress);
    }

    private void settleRedeemOrder(Connection connection, WealthOrder order, long adminUserId, String ipAddress)
            throws SQLException {
        if (order.getHoldingId() == null || order.getConfirmedAmount() == null) {
            throw new SQLException("Invalid wealth redeem order: " + order.getOrderNo());
        }
        WealthHolding holding = wealthDao.findHoldingByIdForUpdate(connection, order.getHoldingId());
        Account account = accountDao.findByIdForUpdate(connection, order.getAccountId());
        WealthProduct product = wealthDao.findProductById(connection, order.getProductId());
        if (holding == null || account == null || product == null) {
            throw new SQLException("Invalid wealth redeem order relation: " + order.getOrderNo());
        }

        BigDecimal redeemAmount = order.getConfirmedAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal income = order.getIncomeAmount() == null
                ? redeemAmount.subtract(holding.getPrincipal()).setScale(2, RoundingMode.HALF_UP)
                : order.getIncomeAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = account.getAvailableBalance().add(redeemAmount).setScale(2, RoundingMode.HALF_UP);
        accountDao.updateAvailableBalance(connection, account.getAccountId(), balanceAfter);

        TransactionRecord transactionRecord = new TransactionRecord();
        transactionRecord.setTransactionNo(order.getOrderNo());
        transactionRecord.setCustomerId(order.getCustomerId());
        transactionRecord.setToAccountId(account.getAccountId());
        transactionRecord.setTxnType("REDEEM_WEALTH");
        transactionRecord.setAmount(redeemAmount);
        transactionRecord.setStatus("SUCCESS");
        transactionRecord.setRiskScore(0);
        transactionRecord.setRemark("理财赎回到账：" + product.getProductName());
        long transactionId = transactionDao.insert(connection, transactionRecord);

        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setTransactionId(transactionId);
        ledgerEntry.setAccountId(account.getAccountId());
        ledgerEntry.setDirection("IN");
        ledgerEntry.setAmount(redeemAmount);
        ledgerEntry.setBalanceAfter(balanceAfter);
        ledgerEntry.setSummary("理财赎回到账：" + product.getProductName());
        ledgerDao.insert(connection, ledgerEntry);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        wealthDao.markRedeemed(connection, holding.getHoldingId(), transactionId, income, now);
        wealthDao.markRedeemOrderCompleted(connection, order.getOrderId(), transactionId, now);

        Customer customer = customerDao.findById(connection, order.getCustomerId());
        Long userId = customer == null ? null : customer.getUserId();
        notificationService.create(connection, order.getCustomerId(), userId, "WEALTH", "理财赎回已到账",
                "您赎回的 " + product.getProductName() + " 已到账，到账金额 ¥" + redeemAmount.toPlainString()
                        + "，其中收益 ¥" + income.toPlainString() + "。",
                "REDEEM_WEALTH", order.getOrderNo());
        operationLogDao.insert(connection, adminUserId, "WEALTH_SETTLEMENT_REDEEM", order.getOrderNo(),
                "处理理财赎回到账：" + product.getProductName() + "，到账金额：" + redeemAmount.toPlainString(),
                ipAddress);
    }

    private WealthSettlementSummary buildSettlementSummary(Connection connection) throws SQLException {
        WealthSettlementSummary summary = new WealthSettlementSummary();
        summary.setPendingBuyOrders(wealthDao.countOrdersByStatus(connection, ORDER_TYPE_BUY,
                STATUS_BUY_PENDING_CONFIRM));
        summary.setRedeemingOrders(wealthDao.countOrdersByStatus(connection, ORDER_TYPE_REDEEM,
                STATUS_REDEEM_PENDING_ARRIVAL));
        return summary;
    }

    private ServiceResult<Void> verifyPayPassword(User user, String payPassword) {
        if (user == null) {
            return ServiceResult.failure("用户不存在，请重新登录。");
        }
        if (user.getPayPasswordHash() == null || user.getPayPasswordHash().trim().length() == 0) {
            return ServiceResult.failure("请先设置支付密码。");
        }
        if (!PasswordUtil.matches(payPassword, user.getPayPasswordHash())) {
            return ServiceResult.failure("支付密码错误。");
        }
        return ServiceResult.success("支付密码校验成功。", null);
    }

    private ServiceResult<Void> validateRiskProfile(Connection connection, Customer customer) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        RiskAssessment validAssessment = riskAssessmentDao.findLatestValidByCustomer(connection,
                customer.getCustomerId(), now);
        if (validAssessment != null) {
            return ServiceResult.success("风险测评有效。", null);
        }
        if ("ADMIN".equals(customer.getRiskLevelSource()) && customer.getRiskLevelUpdatedAt() != null) {
            long validUntil = customer.getRiskLevelUpdatedAt().getTime()
                    + RISK_PROFILE_VALID_DAYS * 24L * 60L * 60L * 1000L;
            if (validUntil >= now.getTime()) {
                return ServiceResult.success("管理员确认的风险等级有效。", null);
            }
        }
        return ServiceResult.failure("请先完成风险测评，或联系管理员更新风险等级后再申购理财产品。");
    }

    private boolean riskMatched(String customerRisk, String productRisk) {
        return riskRank(customerRisk) >= riskRank(productRisk);
    }

    private String matchResult(String customerRisk, String productRisk) {
        int customerRank = riskRank(customerRisk);
        int productRank = riskRank(productRisk);
        if (customerRank == productRank) {
            return "EDGE_MATCH";
        }
        if (customerRank > productRank) {
            return "MATCH";
        }
        return "MISMATCH";
    }

    private int riskRank(String risk) {
        if (risk == null || risk.length() < 2) {
            return 0;
        }
        try {
            return Integer.parseInt(risk.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void enrichIncome(WealthHoldingView holding) {
        if ("HOLDING".equals(holding.getStatus()) || "REDEEMING".equals(holding.getStatus())) {
            BigDecimal income = calculateIncome(holding.getPrincipal(), holding.getExpectedRate(), holding.getBuyTime());
            BigDecimal estimatedValue = holding.getPrincipal().add(income).setScale(2, RoundingMode.HALF_UP);
            holding.setCurrentIncome(income);
            holding.setEstimatedValue(estimatedValue);
            holding.setRedeemAmount(estimatedValue);
        } else {
            BigDecimal income = holding.getCurrentIncome() == null
                    ? BigDecimal.ZERO.setScale(2)
                    : holding.getCurrentIncome().setScale(2, RoundingMode.HALF_UP);
            holding.setCurrentIncome(income);
            BigDecimal finalValue = holding.getPrincipal().add(income).setScale(2, RoundingMode.HALF_UP);
            holding.setEstimatedValue(finalValue);
            holding.setRedeemAmount(finalValue);
        }
        holding.setHoldingDays(holdingDays(holding.getBuyTime()));
        holding.setRemainingDays(remainingDays(holding.getMaturityDate()));
    }

    private BigDecimal calculateIncome(BigDecimal principal, BigDecimal expectedRate, Timestamp buyTime) {
        int days = holdingDays(buyTime);
        return principal.multiply(expectedRate)
                .multiply(new BigDecimal(days))
                .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
    }

    private int holdingDays(Timestamp buyTime) {
        if (buyTime == null) {
            return 1;
        }
        LocalDate buyDate = buyTime.toLocalDateTime().toLocalDate();
        long days = ChronoUnit.DAYS.between(buyDate, LocalDate.now());
        return (int) Math.max(1L, days);
    }

    private int remainingDays(Date maturityDate) {
        if (maturityDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), maturityDate.toLocalDate());
        return (int) Math.max(0L, days);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private ServiceResult<RiskDecision> evaluateRisk(Connection connection, Customer customer, long accountId,
                                                     String transactionNo, BigDecimal amount, String ipAddress) {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setCustomerId(customer.getCustomerId());
        request.setAccountId(accountId);
        request.setTransactionNo(transactionNo);
        request.setTxnType("BUY_WEALTH");
        request.setAmount(amount);
        request.setCustomerRiskLevel(customer.getRiskLevel());
        request.setIpAddress(ipAddress);
        return riskService.evaluateAndReserve(connection, request);
    }

    private void notifyRiskBlocked(Connection connection, long customerId, long userId, String title,
                                   String transactionNo, BigDecimal amount, RiskDecision riskDecision)
            throws SQLException {
        notificationService.create(connection, customerId, userId, "RISK", title,
                "交易金额 ¥" + amount.toPlainString() + "，已被风控拦截。原因：" + riskDecision.getReason(),
                "BUY_WEALTH", transactionNo);
    }

    private void notifyRiskWarnIfNeeded(Connection connection, long customerId, long userId, String title,
                                        String transactionNo, BigDecimal amount, RiskDecision riskDecision)
            throws SQLException {
        if (riskDecision != null && "WARN".equals(riskDecision.getDecision())) {
            notificationService.create(connection, customerId, userId, "RISK", title,
                    "交易金额 ¥" + amount.toPlainString() + "，已通过但命中风控预警。原因："
                            + riskDecision.getReason(),
                    "BUY_WEALTH", transactionNo);
        }
    }

    private TransactionResult buildResult(String transactionNo, String txnType, String accountNo, String direction,
                                          BigDecimal amount, BigDecimal balanceAfter, String summary) {
        TransactionResult result = new TransactionResult();
        result.setTransactionNo(transactionNo);
        result.setTxnType(txnType);
        result.setAccountNo(accountNo);
        result.setDirection(direction);
        result.setAmount(amount);
        result.setBalanceAfter(balanceAfter);
        if ("FREEZE".equals(direction)) {
            result.setStatus(STATUS_BUY_PENDING_CONFIRM);
        } else if ("PENDING_IN".equals(direction)) {
            result.setStatus(STATUS_REDEEM_PENDING_ARRIVAL);
        } else {
            result.setStatus("SUCCESS");
        }
        result.setSummary(summary);
        result.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return result;
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
