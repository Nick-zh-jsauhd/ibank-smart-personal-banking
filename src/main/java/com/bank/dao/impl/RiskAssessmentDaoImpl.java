package com.bank.dao.impl;

import com.bank.bean.RiskAssessment;
import com.bank.dao.RiskAssessmentDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RiskAssessmentDaoImpl implements RiskAssessmentDao {
    @Override
    public long insert(Connection connection, RiskAssessment assessment) throws SQLException {
        String sql = "INSERT INTO t_risk_assessment (customer_id, total_score, risk_level, answers_json, "
                + "status, effective_from, effective_until) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, assessment.getCustomerId());
            statement.setInt(2, assessment.getTotalScore());
            statement.setString(3, assessment.getRiskLevel());
            statement.setString(4, assessment.getAnswersJson());
            statement.setString(5, assessment.getStatus());
            statement.setTimestamp(6, assessment.getEffectiveFrom());
            statement.setTimestamp(7, assessment.getEffectiveUntil());
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
    public RiskAssessment findLatestByCustomer(Connection connection, long customerId) throws SQLException {
        String sql = baseSelectSql() + " WHERE customer_id = ? ORDER BY created_at DESC, assessment_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAssessment(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public RiskAssessment findLatestValidByCustomer(Connection connection, long customerId, Timestamp now)
            throws SQLException {
        String sql = baseSelectSql() + " WHERE customer_id = ? AND status = 'VALID' "
                + "AND effective_from <= ? AND effective_until >= ? "
                + "ORDER BY effective_until DESC, assessment_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setTimestamp(2, now);
            statement.setTimestamp(3, now);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAssessment(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<RiskAssessment> findByCustomer(Connection connection, long customerId, int limit)
            throws SQLException {
        String sql = baseSelectSql() + " WHERE customer_id = ? ORDER BY created_at DESC, assessment_id DESC LIMIT ?";
        List<RiskAssessment> assessments = new ArrayList<RiskAssessment>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessments.add(mapAssessment(resultSet));
                }
            }
        }
        return assessments;
    }

    private String baseSelectSql() {
        return "SELECT assessment_id, customer_id, total_score, risk_level, answers_json, status, "
                + "effective_from, effective_until, created_at FROM t_risk_assessment";
    }

    private RiskAssessment mapAssessment(ResultSet resultSet) throws SQLException {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setAssessmentId(resultSet.getLong("assessment_id"));
        assessment.setCustomerId(resultSet.getLong("customer_id"));
        assessment.setTotalScore(resultSet.getInt("total_score"));
        assessment.setRiskLevel(resultSet.getString("risk_level"));
        assessment.setAnswersJson(resultSet.getString("answers_json"));
        assessment.setStatus(resultSet.getString("status"));
        assessment.setEffectiveFrom(resultSet.getTimestamp("effective_from"));
        assessment.setEffectiveUntil(resultSet.getTimestamp("effective_until"));
        assessment.setCreatedAt(resultSet.getTimestamp("created_at"));
        return assessment;
    }
}
