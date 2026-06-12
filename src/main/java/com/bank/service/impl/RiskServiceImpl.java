package com.bank.service.impl;

import com.bank.bean.CustomerLimit;
import com.bank.bean.RiskLimitRule;
import com.bank.bean.RiskLimitUsage;
import com.bank.dao.RiskDao;
import com.bank.dao.impl.RiskDaoImpl;
import com.bank.dto.RiskCheckRequest;
import com.bank.dto.RiskDecision;
import com.bank.dto.RiskEventView;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskService;
import com.bank.util.DBUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RiskServiceImpl implements RiskService {
    private static final int EVENT_QUERY_LIMIT = 100;

    private final RiskDao riskDao = new RiskDaoImpl();

    @Override
    public ServiceResult<RiskDecision> evaluateAndReserve(Connection connection, RiskCheckRequest request) {
        if (request == null || request.getCustomerId() == null || request.getTxnType() == null
                || request.getAmount() == null || request.getTransactionNo() == null) {
            return ServiceResult.failure("风控参数不完整。");
        }

        try {
            String customerRiskLevel = normalizeRiskLevel(request.getCustomerRiskLevel());
            RiskLimitRule rule = effectiveRule(connection, request.getCustomerId(), request.getTxnType(),
                    customerRiskLevel);
            if (rule == null) {
                return ServiceResult.failure("未找到当前交易类型的风控限额规则。");
            }

            RiskLimitUsage usage = riskDao.lockUsage(connection, request.getCustomerId(), LocalDate.now(),
                    request.getTxnType());
            RiskDecision decision = evaluate(connection, request, rule, usage);
            if (!"PASS".equals(decision.getDecision())) {
                riskDao.insertEvent(connection, request, decision);
            }
            if (!decision.isBlocked()) {
                BigDecimal nextUsedAmount = usage.getUsedAmount().add(request.getAmount())
                        .setScale(2, RoundingMode.HALF_UP);
                riskDao.updateUsage(connection, usage.getUsageId(), nextUsedAmount, usage.getUsedCount() + 1);
            }
            return ServiceResult.success("风控检查完成。", decision);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风控检查失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<List<RiskEventView>> listEvents(long customerId, String decision) {
        String normalizedDecision = normalizeDecision(decision);
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("风险事件查询成功。",
                    riskDao.findEventsByCustomer(connection, customerId, normalizedDecision, EVENT_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风险事件查询失败，请检查数据库状态或稍后重试。");
        }
    }

    private RiskLimitRule effectiveRule(Connection connection, long customerId, String txnType,
                                        String customerRiskLevel) throws SQLException {
        CustomerLimit customerLimit = riskDao.findActiveCustomerLimit(connection, customerId, txnType);
        if (customerLimit != null) {
            RiskLimitRule rule = new RiskLimitRule();
            rule.setRuleCode("CUSTOMER_LIMIT_" + customerId + "_" + txnType);
            rule.setTxnType(txnType);
            rule.setCustomerRiskLevel(customerRiskLevel);
            rule.setSingleLimit(customerLimit.getSingleLimit());
            rule.setDailyAmountLimit(customerLimit.getDailyAmountLimit());
            rule.setDailyCountLimit(customerLimit.getDailyCountLimit());
            rule.setStatus(customerLimit.getStatus());
            return rule;
        }
        RiskLimitRule rule = riskDao.findActiveRule(connection, txnType, customerRiskLevel);
        if (rule == null && !"C3".equals(customerRiskLevel)) {
            rule = riskDao.findActiveRule(connection, txnType, "C3");
        }
        return rule;
    }

    private RiskDecision evaluate(Connection connection, RiskCheckRequest request, RiskLimitRule rule,
                                  RiskLimitUsage usage) throws SQLException {
        int score = 0;
        boolean blocked = false;
        List<String> hitRules = new ArrayList<String>();
        List<String> reasons = new ArrayList<String>();
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(rule.getSingleLimit()) > 0) {
            blocked = true;
            score += 60;
            hitRules.add("SINGLE_LIMIT");
            reasons.add("单笔金额超过限额 ¥" + moneyText(rule.getSingleLimit()));
        }

        BigDecimal nextUsedAmount = usage.getUsedAmount().add(amount).setScale(2, RoundingMode.HALF_UP);
        if (nextUsedAmount.compareTo(rule.getDailyAmountLimit()) > 0) {
            blocked = true;
            score += 60;
            hitRules.add("DAILY_AMOUNT_LIMIT");
            reasons.add("当日累计金额将超过限额 ¥" + moneyText(rule.getDailyAmountLimit()));
        }

        if (usage.getUsedCount() + 1 > rule.getDailyCountLimit()) {
            blocked = true;
            score += 45;
            hitRules.add("DAILY_COUNT_LIMIT");
            reasons.add("当日交易笔数将超过 " + rule.getDailyCountLimit() + " 笔");
        }

        LocalTime now = LocalTime.now();
        if ((now.getHour() >= 23 || now.getHour() < 6) && amount.compareTo(new BigDecimal("1000.00")) >= 0) {
            score += 15;
            hitRules.add("NIGHT_OUTFLOW");
            reasons.add("夜间资金流出交易");
        }

        Timestamp recentSince = Timestamp.valueOf(LocalDateTime.now().minusMinutes(10));
        int recentOutflowCount = riskDao.countRecentOutflow(connection, request.getCustomerId(), recentSince);
        if (recentOutflowCount >= 5) {
            score += recentOutflowCount >= 8 ? 35 : 20;
            hitRules.add("FREQUENT_OUTFLOW");
            reasons.add("10 分钟内已有 " + recentOutflowCount + " 笔资金流出交易");
        }

        Timestamp avgSince = Timestamp.valueOf(LocalDateTime.now().minusDays(90));
        BigDecimal avgAmount = riskDao.averageSuccessfulAmount(connection, request.getCustomerId(),
                request.getTxnType(), avgSince);
        if (avgAmount != null && avgAmount.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(avgAmount.multiply(new BigDecimal("3")).setScale(2, RoundingMode.HALF_UP)) > 0
                && amount.compareTo(new BigDecimal("5000.00")) > 0) {
            score += 20;
            hitRules.add("HISTORY_AMOUNT_DEVIATION");
            reasons.add("交易金额明显高于近 90 天同类交易均值 ¥" + moneyText(avgAmount));
        }

        if ("TRANSFER_INNER".equals(request.getTxnType()) && request.getTargetAccountId() != null
                && amount.compareTo(new BigDecimal("10000.00")) > 0) {
            int previousTransfers = riskDao.countSuccessfulTransferToAccount(connection, request.getCustomerId(),
                    request.getTargetAccountId());
            if (previousTransfers == 0) {
                score += 20;
                hitRules.add("NEW_RECEIVER_LARGE_TRANSFER");
                reasons.add("首次向该账户发起大额转账");
            }
        }

        RiskDecision decision = new RiskDecision();
        decision.setRiskScore(Math.min(score, 100));
        decision.setDecision(blocked ? "BLOCK" : (score > 0 ? "WARN" : "PASS"));
        decision.setRiskLevel(riskLevel(decision.getRiskScore(), blocked));
        decision.setHitRules(join(hitRules));
        decision.setReason(reasons.isEmpty() ? "风控通过。" : join(reasons));
        return decision;
    }

    private String normalizeRiskLevel(String riskLevel) {
        if ("C1".equals(riskLevel) || "C2".equals(riskLevel) || "C3".equals(riskLevel)
                || "C4".equals(riskLevel) || "C5".equals(riskLevel)) {
            return riskLevel;
        }
        return "C3";
    }

    private String normalizeDecision(String decision) {
        if ("WARN".equals(decision) || "BLOCK".equals(decision)) {
            return decision;
        }
        return null;
    }

    private String riskLevel(int score, boolean blocked) {
        if (blocked || score >= 70) {
            return "HIGH";
        }
        if (score >= 30) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String moneyText(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append(value);
        }
        String joined = builder.toString();
        return joined.length() > 500 ? joined.substring(0, 500) : joined;
    }
}
