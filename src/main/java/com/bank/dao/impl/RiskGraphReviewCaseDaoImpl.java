package com.bank.dao.impl;

import com.bank.dao.RiskGraphReviewCaseDao;
import com.bank.dto.AdminRiskGraphReviewCaseView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiskGraphReviewCaseDaoImpl implements RiskGraphReviewCaseDao {
    @Override
    public int materializeCandidates(Connection connection, String modelVersion, int limit) throws SQLException {
        String sql = "INSERT INTO t_risk_graph_review_case "
                + "(graph_edge_id, model_version, case_type, case_status, model_decision, business_decision, "
                + "risk_score, risk_probability, label_fraud, priority, reason) "
                + "SELECT * FROM ("
                + "SELECT e.graph_edge_id, s.model_version, "
                + "CASE "
                + "WHEN e.label_fraud = 0 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN 'LABEL_CONFLICT' "
                + "WHEN e.label_fraud = 1 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN 'TRUE_POSITIVE_BLOCK' "
                + "WHEN s.decision = 'REVIEW' AND s.risk_score >= 850 THEN 'STRONG_REVIEW' "
                + "ELSE 'MODEL_REVIEW' END AS case_type, "
                + "'OPEN' AS case_status, s.decision AS model_decision, "
                + "CASE "
                + "WHEN e.label_fraud = 0 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN 'LABEL_CONFLICT' "
                + "WHEN e.label_fraud = 1 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN 'INVESTIGATION_REQUIRED' "
                + "WHEN s.decision = 'REVIEW' AND s.risk_score >= 850 THEN 'STRONG_REVIEW' "
                + "ELSE 'REVIEW' END AS business_decision, "
                + "s.risk_score, s.risk_probability, e.label_fraud, "
                + "CASE "
                + "WHEN e.label_fraud = 0 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN 100 "
                + "WHEN e.label_fraud = 1 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN 95 "
                + "WHEN s.decision = 'REVIEW' AND s.risk_score >= 850 THEN 80 "
                + "ELSE 60 END AS priority, "
                + "CASE "
                + "WHEN e.label_fraud = 0 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN '模型给出高风险，但源数据标签为正常；需要人工检查图谱邻域、金额、交易类型和上下文。' "
                + "WHEN e.label_fraud = 1 AND (s.decision = 'BLOCK' OR s.risk_score >= 900) "
                + "THEN '模型高风险且源数据标签为洗钱；建议作为重点调查样本沉淀。' "
                + "WHEN s.decision = 'REVIEW' AND s.risk_score >= 850 "
                + "THEN '模型进入强复核区间；建议检查是否存在拆分、中转或聚集模式。' "
                + "ELSE '模型进入复核区间；建议记录人工判断，作为后续阈值校准样本。' END AS reason "
                + "FROM t_risk_graph_model_score s "
                + "JOIN t_risk_graph_edge e ON s.graph_edge_id = e.graph_edge_id "
                + "WHERE s.model_version = ? AND ("
                + "(e.label_fraud = 0 AND (s.decision = 'BLOCK' OR s.risk_score >= 900)) "
                + "OR (e.label_fraud = 1 AND (s.decision IN ('BLOCK', 'REVIEW') OR s.risk_score >= 850)) "
                + "OR (s.decision = 'REVIEW' AND s.risk_score >= 850)) "
                + "ORDER BY priority DESC, s.risk_score DESC, s.risk_probability DESC LIMIT ?"
                + ") candidates "
                + "ON DUPLICATE KEY UPDATE "
                + "case_type = VALUES(case_type), "
                + "model_decision = VALUES(model_decision), "
                + "business_decision = VALUES(business_decision), "
                + "risk_score = VALUES(risk_score), "
                + "risk_probability = VALUES(risk_probability), "
                + "label_fraud = VALUES(label_fraud), "
                + "priority = VALUES(priority), "
                + "reason = VALUES(reason), "
                + "updated_at = NOW()";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setInt(2, limit);
            return statement.executeUpdate();
        }
    }

    @Override
    public List<AdminRiskGraphReviewCaseView> findCases(Connection connection, String modelVersion, String caseStatus,
                                                        String caseType, String reviewResult, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(selectCaseSql());
        List<Object> params = new ArrayList<Object>();
        sql.append(" WHERE 1 = 1 ");
        appendFilters(sql, params, modelVersion, caseStatus, caseType, reviewResult);
        sql.append("ORDER BY FIELD(c.case_status, 'OPEN', 'REVIEWED', 'IGNORED'), ")
                .append("c.priority DESC, c.risk_score DESC, c.updated_at DESC LIMIT ?");
        params.add(Integer.valueOf(limit));

        List<AdminRiskGraphReviewCaseView> cases = new ArrayList<AdminRiskGraphReviewCaseView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cases.add(mapCase(resultSet));
                }
            }
        }
        return cases;
    }

    @Override
    public List<AdminRiskGraphReviewCaseView> findFeedbackCases(Connection connection, String modelVersion,
                                                                String reviewResult, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(selectCaseSql());
        List<Object> params = new ArrayList<Object>();
        sql.append(" WHERE c.review_result IS NOT NULL ");
        if (modelVersion != null) {
            sql.append("AND c.model_version = ? ");
            params.add(modelVersion);
        }
        if (reviewResult != null) {
            sql.append("AND c.review_result = ? ");
            params.add(reviewResult);
        }
        sql.append("ORDER BY c.reviewed_at DESC, c.case_id DESC LIMIT ?");
        params.add(Integer.valueOf(limit));

        List<AdminRiskGraphReviewCaseView> cases = new ArrayList<AdminRiskGraphReviewCaseView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cases.add(mapCase(resultSet));
                }
            }
        }
        return cases;
    }

    @Override
    public AdminRiskGraphReviewCaseView findById(Connection connection, long caseId) throws SQLException {
        return findById(connection, caseId, false);
    }

    @Override
    public AdminRiskGraphReviewCaseView findByIdForUpdate(Connection connection, long caseId) throws SQLException {
        return findById(connection, caseId, true);
    }

    @Override
    public Map<String, Integer> countByStatus(Connection connection, String modelVersion) throws SQLException {
        String sql = "SELECT case_status, COUNT(*) AS total FROM t_risk_graph_review_case "
                + "WHERE (? IS NULL OR model_version = ?) GROUP BY case_status";
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setString(2, modelVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getString("case_status"), Integer.valueOf(resultSet.getInt("total")));
                }
            }
        }
        return counts;
    }

    @Override
    public Map<String, Integer> countByReviewResult(Connection connection, String modelVersion) throws SQLException {
        String sql = "SELECT review_result, COUNT(*) AS total FROM t_risk_graph_review_case "
                + "WHERE review_result IS NOT NULL AND (? IS NULL OR model_version = ?) "
                + "GROUP BY review_result";
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setString(2, modelVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getString("review_result"), Integer.valueOf(resultSet.getInt("total")));
                }
            }
        }
        return counts;
    }

    @Override
    public int countConflicts(Connection connection, String modelVersion) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM t_risk_graph_review_case "
                + "WHERE case_type = 'LABEL_CONFLICT' AND (? IS NULL OR model_version = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setString(2, modelVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("total") : 0;
            }
        }
    }

    @Override
    public void updateReview(Connection connection, long caseId, String caseStatus, String reviewResult,
                             String reviewNote, long adminUserId) throws SQLException {
        String sql = "UPDATE t_risk_graph_review_case SET case_status = ?, review_result = ?, "
                + "review_note = ?, reviewed_by_admin_id = ?, reviewed_at = NOW(), updated_at = NOW() "
                + "WHERE case_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, caseStatus);
            statement.setString(2, reviewResult);
            if (reviewNote == null) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, reviewNote);
            }
            statement.setLong(4, adminUserId);
            statement.setLong(5, caseId);
            statement.executeUpdate();
        }
    }

    private AdminRiskGraphReviewCaseView findById(Connection connection, long caseId, boolean forUpdate)
            throws SQLException {
        String sql = selectCaseSql() + " WHERE c.case_id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, caseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCase(resultSet);
                }
            }
        }
        return null;
    }

    private String selectCaseSql() {
        return "SELECT c.case_id, c.graph_edge_id, c.model_version, c.case_type, c.case_status, "
                + "c.model_decision, c.business_decision, c.risk_score, c.risk_probability, c.label_fraud, "
                + "c.priority, c.reason, c.review_result, c.review_note, c.reviewed_by_admin_id, "
                + "u.username AS reviewer_username, c.reviewed_at, c.created_at, c.updated_at, "
                + "b.batch_code, e.dataset_name, e.source_row_no, e.from_external_id, e.to_external_id, "
                + "e.edge_type, e.amount, e.currency, e.event_time, s.feature_version, s.reason_json "
                + "FROM t_risk_graph_review_case c "
                + "JOIN t_risk_graph_edge e ON c.graph_edge_id = e.graph_edge_id "
                + "JOIN t_risk_graph_dataset_batch b ON e.batch_id = b.graph_batch_id "
                + "LEFT JOIN t_risk_graph_model_score s ON s.graph_edge_id = c.graph_edge_id "
                + "AND s.model_version = c.model_version "
                + "LEFT JOIN t_user u ON c.reviewed_by_admin_id = u.user_id";
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String modelVersion, String caseStatus,
                               String caseType, String reviewResult) {
        if (modelVersion != null) {
            sql.append("AND c.model_version = ? ");
            params.add(modelVersion);
        }
        if (caseStatus != null) {
            sql.append("AND c.case_status = ? ");
            params.add(caseStatus);
        }
        if (caseType != null) {
            sql.append("AND c.case_type = ? ");
            params.add(caseType);
        }
        if (reviewResult != null) {
            sql.append("AND c.review_result = ? ");
            params.add(reviewResult);
        }
    }

    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Integer) {
                statement.setInt(i + 1, ((Integer) param).intValue());
            } else {
                statement.setString(i + 1, String.valueOf(param));
            }
        }
    }

    private AdminRiskGraphReviewCaseView mapCase(ResultSet resultSet) throws SQLException {
        AdminRiskGraphReviewCaseView view = new AdminRiskGraphReviewCaseView();
        view.setCaseId(resultSet.getLong("case_id"));
        view.setGraphEdgeId(resultSet.getLong("graph_edge_id"));
        view.setModelVersion(resultSet.getString("model_version"));
        view.setFeatureVersion(resultSet.getString("feature_version"));
        view.setCaseType(resultSet.getString("case_type"));
        view.setCaseStatus(resultSet.getString("case_status"));
        view.setModelDecision(resultSet.getString("model_decision"));
        view.setBusinessDecision(resultSet.getString("business_decision"));
        view.setRiskScore(resultSet.getInt("risk_score"));
        view.setRiskProbability(resultSet.getBigDecimal("risk_probability"));
        view.setLabelFraud(resultSet.getInt("label_fraud") == 1);
        view.setPriority(resultSet.getInt("priority"));
        view.setReason(resultSet.getString("reason"));
        view.setReviewResult(resultSet.getString("review_result"));
        view.setReviewNote(resultSet.getString("review_note"));
        long reviewerId = resultSet.getLong("reviewed_by_admin_id");
        view.setReviewedByAdminId(resultSet.wasNull() ? null : Long.valueOf(reviewerId));
        view.setReviewerUsername(resultSet.getString("reviewer_username"));
        view.setReviewedAt(resultSet.getTimestamp("reviewed_at"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        view.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        view.setBatchCode(resultSet.getString("batch_code"));
        view.setDatasetName(resultSet.getString("dataset_name"));
        view.setSourceRowNo(resultSet.getInt("source_row_no"));
        view.setFromExternalId(resultSet.getString("from_external_id"));
        view.setToExternalId(resultSet.getString("to_external_id"));
        view.setEdgeType(resultSet.getString("edge_type"));
        view.setAmount(resultSet.getBigDecimal("amount"));
        view.setCurrency(resultSet.getString("currency"));
        view.setEventTime(resultSet.getTimestamp("event_time"));
        view.setReasonJson(resultSet.getString("reason_json"));
        return view;
    }
}
