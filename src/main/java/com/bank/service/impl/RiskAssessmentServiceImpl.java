package com.bank.service.impl;

import com.bank.bean.Customer;
import com.bank.bean.RiskAssessment;
import com.bank.dao.CustomerDao;
import com.bank.dao.RiskAssessmentDao;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dao.impl.RiskAssessmentDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.service.NotificationService;
import com.bank.service.RiskAssessmentService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class RiskAssessmentServiceImpl implements RiskAssessmentService {
    private static final long VALID_DAYS = 365L;
    private static final int ASSESSMENT_QUERY_LIMIT = 20;

    private final RiskAssessmentDao riskAssessmentDao = new RiskAssessmentDaoImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();

    @Override
    public ServiceResult<RiskAssessment> getLatestAssessment(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("风险测评查询成功。",
                    riskAssessmentDao.findLatestByCustomer(connection, customerId));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风险测评查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<RiskAssessment> submitAssessment(long customerId, long userId, String age,
                                                          String experience, String lossTolerance, String goal,
                                                          String horizon, String liquidity, String knowledge) {
        AssessmentScore score;
        try {
            score = score(age, experience, lossTolerance, goal, horizon, liquidity, knowledge);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);

            Customer customer = customerDao.findById(connection, customerId);
            if (customer == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("客户档案不存在，请重新登录。");
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp until = new Timestamp(now.getTime() + VALID_DAYS * 24L * 60L * 60L * 1000L);
            RiskAssessment assessment = new RiskAssessment();
            assessment.setCustomerId(customerId);
            assessment.setTotalScore(score.getTotalScore());
            assessment.setRiskLevel(score.getRiskLevel());
            assessment.setAnswersJson(score.getAnswersJson());
            assessment.setStatus("VALID");
            assessment.setEffectiveFrom(now);
            assessment.setEffectiveUntil(until);
            long assessmentId = riskAssessmentDao.insert(connection, assessment);
            assessment.setAssessmentId(assessmentId);
            assessment.setCreatedAt(now);

            customerDao.updateRiskProfile(connection, customerId, score.getRiskLevel(), "ASSESSMENT", now);
            notificationService.create(connection, customerId, userId, "RISK", "风险测评已完成",
                    "您的风险承受能力等级已更新为 " + score.getRiskLevel()
                            + "，测评有效期至 " + until.toString().substring(0, 10) + "。",
                    "RISK_ASSESSMENT", String.valueOf(assessmentId));

            connection.commit();
            return ServiceResult.success("风险测评提交成功。", assessment);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("风险测评提交失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<RiskAssessment>> listAssessments(long customerId) {
        try (Connection connection = DBUtil.getConnection()) {
            return ServiceResult.success("风险测评记录查询成功。",
                    riskAssessmentDao.findByCustomer(connection, customerId, ASSESSMENT_QUERY_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("风险测评记录查询失败，请检查数据库状态或稍后重试。");
        }
    }

    private AssessmentScore score(String age, String experience, String lossTolerance, String goal,
                                  String horizon, String liquidity, String knowledge) {
        int total = 0;
        total += optionScore("年龄区间", age, "AGE_OVER_60", 4, "AGE_50_60", 8,
                "AGE_36_49", 12, "AGE_25_35", 16, "AGE_18_24", 12);
        total += optionScore("投资经验", experience, "EXP_NONE", 2, "EXP_LT1", 6,
                "EXP_1_3", 10, "EXP_3_5", 14, "EXP_GT5", 18);
        total += optionScore("可承受亏损比例", lossTolerance, "LOSS_NONE", 2, "LOSS_5", 8,
                "LOSS_10", 14, "LOSS_20", 20, "LOSS_GT20", 26);
        total += optionScore("投资目标", goal, "GOAL_PRINCIPAL", 2, "GOAL_STABLE", 8,
                "GOAL_BALANCE", 14, "GOAL_GROWTH", 20, "GOAL_AGGRESSIVE", 24);
        total += optionScore("投资期限偏好", horizon, "HORIZON_1M", 4, "HORIZON_3M", 8,
                "HORIZON_6M", 12, "HORIZON_1Y", 16, "HORIZON_GT1Y", 18);
        total += optionScore("流动性要求", liquidity, "LIQUIDITY_ANYTIME", 4, "LIQUIDITY_WEEK", 8,
                "LIQUIDITY_MONTH", 12, "LIQUIDITY_QUARTER", 16, "LIQUIDITY_LONG", 18);
        total += optionScore("金融知识水平", knowledge, "KNOWLEDGE_LOW", 2, "KNOWLEDGE_BASIC", 6,
                "KNOWLEDGE_COMMON", 10, "KNOWLEDGE_MULTI", 14, "KNOWLEDGE_PRO", 18);

        return new AssessmentScore(total, riskLevel(total),
                "{\"age\":\"" + json(age) + "\","
                        + "\"experience\":\"" + json(experience) + "\","
                        + "\"lossTolerance\":\"" + json(lossTolerance) + "\","
                        + "\"goal\":\"" + json(goal) + "\","
                        + "\"horizon\":\"" + json(horizon) + "\","
                        + "\"liquidity\":\"" + json(liquidity) + "\","
                        + "\"knowledge\":\"" + json(knowledge) + "\"}");
    }

    private int optionScore(String fieldName, String value,
                            String option1, int score1, String option2, int score2, String option3, int score3,
                            String option4, int score4, String option5, int score5) {
        if (option1.equals(value)) return score1;
        if (option2.equals(value)) return score2;
        if (option3.equals(value)) return score3;
        if (option4.equals(value)) return score4;
        if (option5.equals(value)) return score5;
        throw new IllegalArgumentException("请选择有效的" + fieldName + "。");
    }

    private String riskLevel(int score) {
        if (score <= 25) {
            return "C1";
        }
        if (score <= 45) {
            return "C2";
        }
        if (score <= 65) {
            return "C3";
        }
        if (score <= 85) {
            return "C4";
        }
        return "C5";
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // ignore rollback failure
            }
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignore close failure
            }
        }
    }

    private static class AssessmentScore {
        private final int totalScore;
        private final String riskLevel;
        private final String answersJson;

        AssessmentScore(int totalScore, String riskLevel, String answersJson) {
            this.totalScore = totalScore;
            this.riskLevel = riskLevel;
            this.answersJson = answersJson;
        }

        int getTotalScore() {
            return totalScore;
        }

        String getRiskLevel() {
            return riskLevel;
        }

        String getAnswersJson() {
            return answersJson;
        }
    }
}
