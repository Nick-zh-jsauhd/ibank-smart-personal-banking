package com.bank.dao.impl;

import com.bank.dao.RiskGraphScoreDao;
import com.bank.dto.AdminRiskGraphEdgeView;
import com.bank.dto.AdminRiskGraphCapacityMetric;
import com.bank.dto.AdminRiskGraphModelGovernanceView;
import com.bank.dto.AdminRiskGraphModelVersionMetric;
import com.bank.dto.AdminRiskGraphNeighborhood;
import com.bank.dto.AdminRiskGraphNodeView;
import com.bank.dto.AdminRiskGraphScoreSummary;
import com.bank.dto.AdminRiskGraphScoreView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RiskGraphScoreDaoImpl implements RiskGraphScoreDao {
    @Override
    public void ensureGovernanceTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS t_risk_graph_model_governance ("
                + "model_version VARCHAR(80) PRIMARY KEY, "
                + "model_role VARCHAR(30) NOT NULL DEFAULT 'EXPERIMENT', "
                + "lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'EVALUATING', "
                + "online_mode VARCHAR(30) NOT NULL DEFAULT 'OFFLINE_REVIEW', "
                + "is_operational TINYINT(1) NOT NULL DEFAULT 0, "
                + "governance_note VARCHAR(500) NULL, "
                + "promoted_by_admin_user_id BIGINT NULL, "
                + "promoted_at DATETIME NULL, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "KEY idx_risk_graph_model_governance_role (model_role), "
                + "KEY idx_risk_graph_model_governance_operational (is_operational), "
                + "CONSTRAINT fk_risk_graph_model_governance_admin "
                + "FOREIGN KEY (promoted_by_admin_user_id) REFERENCES t_user (user_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    @Override
    public List<String> findModelVersions(Connection connection) throws SQLException {
        String sql = "SELECT model_version FROM t_risk_graph_model_score "
                + "GROUP BY model_version ORDER BY model_version DESC";
        List<String> versions = new ArrayList<String>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                versions.add(resultSet.getString("model_version"));
            }
        }
        return versions;
    }

    @Override
    public String findOperationalModelVersion(Connection connection) throws SQLException {
        String sql = "SELECT model_version FROM t_risk_graph_model_governance "
                + "WHERE is_operational = 1 ORDER BY promoted_at DESC, updated_at DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString("model_version");
            }
        }
        return null;
    }

    @Override
    public void insertMissingGovernance(Connection connection, List<String> modelVersions) throws SQLException {
        if (modelVersions == null || modelVersions.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO t_risk_graph_model_governance (model_version) VALUES (?) "
                + "ON DUPLICATE KEY UPDATE model_version = VALUES(model_version)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String modelVersion : modelVersions) {
                statement.setString(1, modelVersion);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public void promoteDefaultOperationalModel(Connection connection, String modelVersion) throws SQLException {
        if (modelVersion == null) {
            return;
        }
        try (PreparedStatement clear = connection.prepareStatement(
                "UPDATE t_risk_graph_model_governance SET is_operational = 0 WHERE is_operational = 1")) {
            clear.executeUpdate();
        }
        String sql = "UPDATE t_risk_graph_model_governance "
                + "SET model_role = 'OPERATING', lifecycle_status = 'APPROVED', "
                + "online_mode = 'OFFLINE_REVIEW', is_operational = 1, "
                + "governance_note = CASE WHEN governance_note IS NULL OR governance_note = '' "
                + "THEN 'Auto selected as the first operational offline-review model.' ELSE governance_note END, "
                + "promoted_at = COALESCE(promoted_at, NOW()) "
                + "WHERE model_version = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.executeUpdate();
        }
    }

    @Override
    public List<AdminRiskGraphModelGovernanceView> loadModelGovernance(Connection connection,
                                                                        List<String> modelVersions)
            throws SQLException {
        List<AdminRiskGraphModelGovernanceView> governance =
                new ArrayList<AdminRiskGraphModelGovernanceView>();
        if (modelVersions == null || modelVersions.isEmpty()) {
            return governance;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT model_version, model_role, lifecycle_status, online_mode, is_operational, ");
        sql.append("governance_note, promoted_by_admin_user_id, promoted_at, created_at, updated_at ");
        sql.append("FROM t_risk_graph_model_governance WHERE model_version IN (");
        appendPlaceholders(sql, modelVersions.size());
        sql.append(")");

        Map<String, AdminRiskGraphModelGovernanceView> viewMap =
                new LinkedHashMap<String, AdminRiskGraphModelGovernanceView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < modelVersions.size(); i++) {
                statement.setString(i + 1, modelVersions.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AdminRiskGraphModelGovernanceView view = mapGovernance(resultSet);
                    viewMap.put(view.getModelVersion(), view);
                }
            }
        }

        for (String modelVersion : modelVersions) {
            AdminRiskGraphModelGovernanceView view = viewMap.get(modelVersion);
            governance.add(view == null ? defaultGovernance(modelVersion) : view);
        }
        return governance;
    }

    @Override
    public void updateModelGovernance(Connection connection, String modelVersion, String modelRole,
                                      String lifecycleStatus, String onlineMode, boolean operational,
                                      String governanceNote, long adminUserId) throws SQLException {
        List<String> versions = new ArrayList<String>();
        versions.add(modelVersion);
        insertMissingGovernance(connection, versions);
        if (operational) {
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE t_risk_graph_model_governance SET is_operational = 0 "
                            + "WHERE is_operational = 1 AND model_version <> ?")) {
                clear.setString(1, modelVersion);
                clear.executeUpdate();
            }
        }

        String sql = "UPDATE t_risk_graph_model_governance "
                + "SET model_role = ?, lifecycle_status = ?, online_mode = ?, is_operational = ?, "
                + "governance_note = ?, "
                + "promoted_by_admin_user_id = CASE WHEN ? = 1 THEN ? ELSE promoted_by_admin_user_id END, "
                + "promoted_at = CASE WHEN ? = 1 THEN NOW() ELSE promoted_at END "
                + "WHERE model_version = ?";
        int operationalValue = operational ? 1 : 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelRole);
            statement.setString(2, lifecycleStatus);
            statement.setString(3, onlineMode);
            statement.setInt(4, operationalValue);
            statement.setString(5, governanceNote);
            statement.setInt(6, operationalValue);
            statement.setLong(7, adminUserId);
            statement.setInt(8, operationalValue);
            statement.setString(9, modelVersion);
            statement.executeUpdate();
        }
    }

    @Override
    public List<String> findEdgeTypes(Connection connection, String modelVersion) throws SQLException {
        String sql = "SELECT edge_type FROM t_risk_graph_edge GROUP BY edge_type ORDER BY edge_type";
        List<String> types = new ArrayList<String>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    types.add(resultSet.getString("edge_type"));
                }
            }
        }
        return types;
    }

    @Override
    public AdminRiskGraphScoreSummary loadSummary(Connection connection, String modelVersion) throws SQLException {
        String sql = "SELECT s.model_version, COUNT(*) AS total_scores, "
                + "SUM(CASE WHEN s.decision = 'PASS' THEN 1 ELSE 0 END) AS pass_count, "
                + "SUM(CASE WHEN s.decision = 'REVIEW' THEN 1 ELSE 0 END) AS review_count, "
                + "SUM(CASE WHEN s.decision = 'BLOCK' THEN 1 ELSE 0 END) AS block_count, "
                + "SUM(CASE WHEN e.label_fraud = 1 THEN 1 ELSE 0 END) AS fraud_label_count, "
                + "SUM(CASE WHEN s.decision = 'PASS' AND e.label_fraud = 1 THEN 1 ELSE 0 END) AS pass_fraud_count, "
                + "SUM(CASE WHEN s.decision = 'REVIEW' AND e.label_fraud = 1 THEN 1 ELSE 0 END) AS review_fraud_count, "
                + "SUM(CASE WHEN s.decision = 'BLOCK' AND e.label_fraud = 1 THEN 1 ELSE 0 END) AS block_fraud_count, "
                + "AVG(s.risk_score) AS avg_score, AVG(s.risk_probability) AS avg_probability, "
                + "MAX(s.review_threshold) AS review_threshold, MAX(s.block_threshold) AS block_threshold "
                + "FROM t_risk_graph_model_score s "
                + "JOIN t_risk_graph_edge e ON s.graph_edge_id = e.graph_edge_id "
                + "WHERE (? IS NULL OR s.model_version = ?) "
                + "GROUP BY s.model_version ORDER BY MAX(s.scored_at) DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setString(2, modelVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    AdminRiskGraphScoreSummary summary = new AdminRiskGraphScoreSummary();
                    summary.setModelVersion(resultSet.getString("model_version"));
                    summary.setTotalScores(resultSet.getLong("total_scores"));
                    summary.setPassCount(resultSet.getLong("pass_count"));
                    summary.setReviewCount(resultSet.getLong("review_count"));
                    summary.setBlockCount(resultSet.getLong("block_count"));
                    summary.setFraudLabelCount(resultSet.getLong("fraud_label_count"));
                    summary.setPassFraudCount(resultSet.getLong("pass_fraud_count"));
                    summary.setReviewFraudCount(resultSet.getLong("review_fraud_count"));
                    summary.setBlockFraudCount(resultSet.getLong("block_fraud_count"));
                    summary.setAverageScore(resultSet.getBigDecimal("avg_score"));
                    summary.setAverageProbability(resultSet.getBigDecimal("avg_probability"));
                    summary.setReviewThreshold(resultSet.getBigDecimal("review_threshold"));
                    summary.setBlockThreshold(resultSet.getBigDecimal("block_threshold"));
                    return summary;
                }
            }
        }
        return new AdminRiskGraphScoreSummary();
    }

    @Override
    public List<AdminRiskGraphModelVersionMetric> loadModelMetrics(Connection connection, List<String> modelVersions)
            throws SQLException {
        List<AdminRiskGraphModelVersionMetric> metrics =
                new ArrayList<AdminRiskGraphModelVersionMetric>();
        if (modelVersions == null || modelVersions.isEmpty()) {
            return metrics;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.model_version, MAX(s.feature_version) AS feature_version, ");
        sql.append("COUNT(*) AS total_scores, ");
        sql.append("SUM(CASE WHEN e.label_fraud = 1 THEN 1 ELSE 0 END) AS positive_labels, ");
        sql.append("SUM(CASE WHEN s.decision = 'PASS' THEN 1 ELSE 0 END) AS pass_count, ");
        sql.append("SUM(CASE WHEN s.decision = 'REVIEW' THEN 1 ELSE 0 END) AS review_count, ");
        sql.append("SUM(CASE WHEN s.decision = 'BLOCK' THEN 1 ELSE 0 END) AS block_count, ");
        sql.append("SUM(CASE WHEN s.decision = 'PASS' AND e.label_fraud = 1 THEN 1 ELSE 0 END) AS pass_fraud_count, ");
        sql.append("SUM(CASE WHEN s.decision = 'REVIEW' AND e.label_fraud = 1 THEN 1 ELSE 0 END) AS review_fraud_count, ");
        sql.append("SUM(CASE WHEN s.decision = 'BLOCK' AND e.label_fraud = 1 THEN 1 ELSE 0 END) AS block_fraud_count, ");
        sql.append("AVG(s.risk_score) AS avg_score, AVG(s.risk_probability) AS avg_probability, ");
        sql.append("MAX(s.review_threshold) AS review_threshold, MAX(s.block_threshold) AS block_threshold ");
        sql.append("FROM t_risk_graph_model_score s ");
        sql.append("JOIN t_risk_graph_edge e ON s.graph_edge_id = e.graph_edge_id ");
        sql.append("WHERE s.model_version IN (");
        appendPlaceholders(sql, modelVersions.size());
        sql.append(") GROUP BY s.model_version");

        Map<String, AdminRiskGraphModelVersionMetric> metricMap =
                new LinkedHashMap<String, AdminRiskGraphModelVersionMetric>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < modelVersions.size(); i++) {
                statement.setString(i + 1, modelVersions.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AdminRiskGraphModelVersionMetric metric = new AdminRiskGraphModelVersionMetric();
                    metric.setModelVersion(resultSet.getString("model_version"));
                    metric.setFeatureVersion(resultSet.getString("feature_version"));
                    metric.setTotalScores(resultSet.getLong("total_scores"));
                    metric.setPositiveLabels(resultSet.getLong("positive_labels"));
                    metric.setPassCount(resultSet.getLong("pass_count"));
                    metric.setReviewCount(resultSet.getLong("review_count"));
                    metric.setBlockCount(resultSet.getLong("block_count"));
                    metric.setPassFraudCount(resultSet.getLong("pass_fraud_count"));
                    metric.setReviewFraudCount(resultSet.getLong("review_fraud_count"));
                    metric.setBlockFraudCount(resultSet.getLong("block_fraud_count"));
                    metric.setAverageScore(resultSet.getBigDecimal("avg_score"));
                    metric.setAverageProbability(resultSet.getBigDecimal("avg_probability"));
                    metric.setReviewThreshold(resultSet.getBigDecimal("review_threshold"));
                    metric.setBlockThreshold(resultSet.getBigDecimal("block_threshold"));
                    metricMap.put(metric.getModelVersion(), metric);
                }
            }
        }

        for (String version : modelVersions) {
            AdminRiskGraphModelVersionMetric metric = metricMap.get(version);
            if (metric != null) {
                metrics.add(metric);
            }
        }
        return metrics;
    }

    @Override
    public List<AdminRiskGraphCapacityMetric> loadCapacityMetrics(Connection connection, List<String> modelVersions,
                                                                  int[] capacities) throws SQLException {
        List<AdminRiskGraphCapacityMetric> metrics = new ArrayList<AdminRiskGraphCapacityMetric>();
        if (modelVersions == null || modelVersions.isEmpty() || capacities == null || capacities.length == 0) {
            return metrics;
        }

        int[] sortedCapacities = Arrays.copyOf(capacities, capacities.length);
        Arrays.sort(sortedCapacities);
        for (String modelVersion : modelVersions) {
            long totalPositiveCount = countPositiveLabels(connection, modelVersion);
            metrics.addAll(loadCapacityMetricsForModel(connection, modelVersion, sortedCapacities,
                    totalPositiveCount));
        }
        return metrics;
    }

    private List<AdminRiskGraphCapacityMetric> loadCapacityMetricsForModel(Connection connection, String modelVersion,
                                                                           int[] capacities,
                                                                           long totalPositiveCount)
            throws SQLException {
        List<AdminRiskGraphCapacityMetric> metrics = new ArrayList<AdminRiskGraphCapacityMetric>();
        String sql = "SELECT s.risk_score, e.label_fraud "
                + "FROM t_risk_graph_model_score s "
                + "JOIN t_risk_graph_edge e ON s.graph_edge_id = e.graph_edge_id "
                + "WHERE s.model_version = ? "
                + "ORDER BY s.risk_score DESC, s.risk_probability DESC, e.amount DESC";
        int capacityIndex = 0;
        int rowNo = 0;
        int thresholdScore = 0;
        long truePositiveCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next() && capacityIndex < capacities.length) {
                    rowNo++;
                    thresholdScore = resultSet.getInt("risk_score");
                    if (resultSet.getInt("label_fraud") == 1) {
                        truePositiveCount++;
                    }
                    while (capacityIndex < capacities.length && rowNo == capacities[capacityIndex]) {
                        metrics.add(buildCapacityMetric(modelVersion, capacities[capacityIndex], rowNo,
                                thresholdScore, truePositiveCount, totalPositiveCount));
                        capacityIndex++;
                    }
                }
            }
        }
        while (capacityIndex < capacities.length) {
            metrics.add(buildCapacityMetric(modelVersion, capacities[capacityIndex], rowNo, thresholdScore,
                    truePositiveCount, totalPositiveCount));
            capacityIndex++;
        }
        return metrics;
    }

    private long countPositiveLabels(Connection connection, String modelVersion) throws SQLException {
        String sql = "SELECT COUNT(*) AS positive_count "
                + "FROM t_risk_graph_model_score s "
                + "JOIN t_risk_graph_edge e ON s.graph_edge_id = e.graph_edge_id "
                + "WHERE s.model_version = ? AND e.label_fraud = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("positive_count");
                }
            }
        }
        return 0L;
    }

    private AdminRiskGraphCapacityMetric buildCapacityMetric(String modelVersion, int capacity, int selectedCount,
                                                             int thresholdScore, long truePositiveCount,
                                                             long totalPositiveCount) {
        AdminRiskGraphCapacityMetric metric = new AdminRiskGraphCapacityMetric();
        metric.setModelVersion(modelVersion);
        metric.setCapacity(capacity);
        metric.setSelectedCount(selectedCount);
        metric.setThresholdScore(thresholdScore);
        metric.setTruePositiveCount(truePositiveCount);
        metric.setFalsePositiveCount(Math.max(0, selectedCount - truePositiveCount));
        metric.setTotalPositiveCount(totalPositiveCount);
        return metric;
    }

    private void appendPlaceholders(StringBuilder sql, int count) {
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
    }

    @Override
    public List<AdminRiskGraphScoreView> findScores(Connection connection, String modelVersion, String decision,
                                                    String edgeType, Integer minScore, Integer labelFraud,
                                                    int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.graph_edge_id, b.batch_code, e.dataset_name, e.source_row_no, ");
        sql.append("e.from_node_id, e.to_node_id, e.from_external_id, e.to_external_id, ");
        sql.append("e.edge_type, e.amount, e.currency, e.event_time, e.label_fraud, ");
        sql.append("s.model_version, s.feature_version, s.risk_score, s.risk_probability, ");
        sql.append("s.decision, s.review_threshold, s.block_threshold, s.reason_json, s.scored_at, ");
        sql.append("0 AS neighborhood_block_count, 0 AS neighborhood_review_count, 0 AS neighborhood_fraud_count ");
        sql.append("FROM t_risk_graph_model_score s ");
        sql.append("JOIN t_risk_graph_edge e ON s.graph_edge_id = e.graph_edge_id ");
        sql.append("JOIN t_risk_graph_dataset_batch b ON e.batch_id = b.graph_batch_id ");
        sql.append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<Object>();
        if (modelVersion != null) {
            sql.append("AND s.model_version = ? ");
            params.add(modelVersion);
        }
        if (decision != null) {
            sql.append("AND s.decision = ? ");
            params.add(decision);
        }
        if (edgeType != null) {
            sql.append("AND e.edge_type = ? ");
            params.add(edgeType);
        }
        if (minScore != null) {
            sql.append("AND s.risk_score >= ? ");
            params.add(minScore);
        }
        if (labelFraud != null) {
            sql.append("AND e.label_fraud = ? ");
            params.add(labelFraud);
        }
        sql.append("ORDER BY s.risk_score DESC, s.risk_probability DESC, e.amount DESC LIMIT ?");
        params.add(limit);

        List<AdminRiskGraphScoreView> scores = new ArrayList<AdminRiskGraphScoreView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    statement.setInt(i + 1, (Integer) param);
                } else {
                    statement.setString(i + 1, (String) param);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(mapScore(resultSet));
                }
            }
        }
        return scores;
    }

    @Override
    public AdminRiskGraphNeighborhood loadNeighborhood(Connection connection, long graphEdgeId, String modelVersion,
                                                       int edgeLimit) throws SQLException {
        AdminRiskGraphNeighborhood graph = new AdminRiskGraphNeighborhood();
        graph.setCenterEdgeId(graphEdgeId);
        graph.setModelVersion(modelVersion);

        AdminRiskGraphEdgeView centerEdge = loadCenterEdge(connection, graphEdgeId, modelVersion);
        if (centerEdge == null) {
            return graph;
        }

        Map<Long, AdminRiskGraphEdgeView> edgeMap = new LinkedHashMap<Long, AdminRiskGraphEdgeView>();
        centerEdge.setCenterEdge(true);
        edgeMap.put(centerEdge.getGraphEdgeId(), centerEdge);

        String sql = "SELECT e.graph_edge_id, b.batch_code, e.dataset_name, e.source_row_no, "
                + "e.from_node_id, e.to_node_id, e.from_external_id, e.to_external_id, "
                + "e.edge_type, e.amount, e.currency, e.event_time, e.label_fraud, "
                + "s.model_version, s.risk_score, s.risk_probability, s.decision, s.reason_json "
                + "FROM t_risk_graph_edge e "
                + "JOIN t_risk_graph_dataset_batch b ON e.batch_id = b.graph_batch_id "
                + "LEFT JOIN t_risk_graph_model_score s ON s.graph_edge_id = e.graph_edge_id AND s.model_version = ? "
                + "WHERE e.dataset_name = ? "
                + "AND (e.graph_edge_id = ? OR e.from_node_id IN (?, ?) OR e.to_node_id IN (?, ?)) "
                + "ORDER BY CASE WHEN e.graph_edge_id = ? THEN 0 ELSE 1 END, "
                + "COALESCE(s.risk_score, 0) DESC, e.amount DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setString(2, centerEdge.getDatasetName());
            statement.setLong(3, graphEdgeId);
            statement.setLong(4, centerEdge.getFromNodeId());
            statement.setLong(5, centerEdge.getToNodeId());
            statement.setLong(6, centerEdge.getFromNodeId());
            statement.setLong(7, centerEdge.getToNodeId());
            statement.setLong(8, graphEdgeId);
            statement.setInt(9, edgeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AdminRiskGraphEdgeView edge = mapGraphEdge(resultSet, graphEdgeId);
                    edgeMap.put(edge.getGraphEdgeId(), edge);
                }
            }
        }

        Set<Long> nodeIds = new LinkedHashSet<Long>();
        for (AdminRiskGraphEdgeView edge : edgeMap.values()) {
            nodeIds.add(edge.getFromNodeId());
            nodeIds.add(edge.getToNodeId());
            if (edge.isLabelFraud()) {
                graph.setFraudEdgeCount(graph.getFraudEdgeCount() + 1);
            }
            if ("BLOCK".equals(edge.getDecision())) {
                graph.setBlockEdgeCount(graph.getBlockEdgeCount() + 1);
            }
            if ("REVIEW".equals(edge.getDecision())) {
                graph.setReviewEdgeCount(graph.getReviewEdgeCount() + 1);
            }
        }

        Map<Long, AdminRiskGraphNodeView> nodeMap = loadNodes(connection, nodeIds);
        for (AdminRiskGraphEdgeView edge : edgeMap.values()) {
            incrementNeighborhoodStats(nodeMap.get(edge.getFromNodeId()), edge);
            incrementNeighborhoodStats(nodeMap.get(edge.getToNodeId()), edge);
        }
        AdminRiskGraphNodeView sourceNode = nodeMap.get(centerEdge.getFromNodeId());
        AdminRiskGraphNodeView targetNode = nodeMap.get(centerEdge.getToNodeId());
        if (sourceNode != null) {
            sourceNode.setRole(centerEdge.getFromNodeId() == centerEdge.getToNodeId() ? "source-target" : "source");
        }
        if (targetNode != null) {
            targetNode.setRole(centerEdge.getFromNodeId() == centerEdge.getToNodeId() ? "source-target" : "target");
        }

        graph.setEdges(new ArrayList<AdminRiskGraphEdgeView>(edgeMap.values()));
        graph.setNodes(new ArrayList<AdminRiskGraphNodeView>(nodeMap.values()));
        return graph;
    }

    private AdminRiskGraphEdgeView loadCenterEdge(Connection connection, long graphEdgeId, String modelVersion)
            throws SQLException {
        String sql = "SELECT e.graph_edge_id, b.batch_code, e.dataset_name, e.source_row_no, "
                + "e.from_node_id, e.to_node_id, e.from_external_id, e.to_external_id, "
                + "e.edge_type, e.amount, e.currency, e.event_time, e.label_fraud, "
                + "s.model_version, s.risk_score, s.risk_probability, s.decision, s.reason_json "
                + "FROM t_risk_graph_edge e "
                + "JOIN t_risk_graph_dataset_batch b ON e.batch_id = b.graph_batch_id "
                + "JOIN t_risk_graph_model_score s ON s.graph_edge_id = e.graph_edge_id AND s.model_version = ? "
                + "WHERE e.graph_edge_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, modelVersion);
            statement.setLong(2, graphEdgeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapGraphEdge(resultSet, graphEdgeId);
                }
            }
        }
        return null;
    }

    private Map<Long, AdminRiskGraphNodeView> loadNodes(Connection connection, Set<Long> nodeIds)
            throws SQLException {
        Map<Long, AdminRiskGraphNodeView> nodes = new LinkedHashMap<Long, AdminRiskGraphNodeView>();
        if (nodeIds.isEmpty()) {
            return nodes;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT graph_node_id, external_node_id, node_type, display_name, in_degree, out_degree, ");
        sql.append("fraud_in_degree, fraud_out_degree, total_in_amount, total_out_amount ");
        sql.append("FROM t_risk_graph_node WHERE graph_node_id IN (");
        int index = 0;
        for (Long ignored : nodeIds) {
            if (index++ > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            for (Long nodeId : nodeIds) {
                statement.setLong(parameterIndex++, nodeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AdminRiskGraphNodeView node = new AdminRiskGraphNodeView();
                    node.setGraphNodeId(resultSet.getLong("graph_node_id"));
                    node.setExternalNodeId(resultSet.getString("external_node_id"));
                    node.setNodeType(resultSet.getString("node_type"));
                    node.setDisplayName(resultSet.getString("display_name"));
                    node.setInDegree(resultSet.getInt("in_degree"));
                    node.setOutDegree(resultSet.getInt("out_degree"));
                    node.setFraudInDegree(resultSet.getInt("fraud_in_degree"));
                    node.setFraudOutDegree(resultSet.getInt("fraud_out_degree"));
                    node.setTotalInAmount(resultSet.getBigDecimal("total_in_amount"));
                    node.setTotalOutAmount(resultSet.getBigDecimal("total_out_amount"));
                    nodes.put(node.getGraphNodeId(), node);
                }
            }
        }
        return nodes;
    }

    private void incrementNeighborhoodStats(AdminRiskGraphNodeView node, AdminRiskGraphEdgeView edge) {
        if (node == null) {
            return;
        }
        node.setNeighborhoodEdgeCount(node.getNeighborhoodEdgeCount() + 1);
        if (edge.isLabelFraud() || "BLOCK".equals(edge.getDecision()) || "REVIEW".equals(edge.getDecision())) {
            node.setNeighborhoodRiskEdgeCount(node.getNeighborhoodRiskEdgeCount() + 1);
        }
    }

    private AdminRiskGraphEdgeView mapGraphEdge(ResultSet resultSet, long centerEdgeId) throws SQLException {
        AdminRiskGraphEdgeView edge = new AdminRiskGraphEdgeView();
        edge.setGraphEdgeId(resultSet.getLong("graph_edge_id"));
        edge.setBatchCode(resultSet.getString("batch_code"));
        edge.setDatasetName(resultSet.getString("dataset_name"));
        edge.setSourceRowNo(resultSet.getInt("source_row_no"));
        edge.setFromNodeId(resultSet.getLong("from_node_id"));
        edge.setToNodeId(resultSet.getLong("to_node_id"));
        edge.setFromExternalId(resultSet.getString("from_external_id"));
        edge.setToExternalId(resultSet.getString("to_external_id"));
        edge.setEdgeType(resultSet.getString("edge_type"));
        edge.setAmount(resultSet.getBigDecimal("amount"));
        edge.setCurrency(resultSet.getString("currency"));
        edge.setEventTime(resultSet.getTimestamp("event_time"));
        edge.setLabelFraud(resultSet.getInt("label_fraud") == 1);
        edge.setModelVersion(resultSet.getString("model_version"));
        edge.setRiskScore(resultSet.getInt("risk_score"));
        edge.setRiskProbability(resultSet.getBigDecimal("risk_probability"));
        edge.setDecision(resultSet.getString("decision"));
        edge.setReasonJson(resultSet.getString("reason_json"));
        edge.setCenterEdge(edge.getGraphEdgeId() == centerEdgeId);
        return edge;
    }

    private AdminRiskGraphScoreView mapScore(ResultSet resultSet) throws SQLException {
        AdminRiskGraphScoreView score = new AdminRiskGraphScoreView();
        score.setGraphEdgeId(resultSet.getLong("graph_edge_id"));
        score.setBatchCode(resultSet.getString("batch_code"));
        score.setDatasetName(resultSet.getString("dataset_name"));
        score.setSourceRowNo(resultSet.getInt("source_row_no"));
        score.setFromNodeId(resultSet.getLong("from_node_id"));
        score.setToNodeId(resultSet.getLong("to_node_id"));
        score.setFromExternalId(resultSet.getString("from_external_id"));
        score.setToExternalId(resultSet.getString("to_external_id"));
        score.setEdgeType(resultSet.getString("edge_type"));
        score.setAmount(resultSet.getBigDecimal("amount"));
        score.setCurrency(resultSet.getString("currency"));
        score.setEventTime(resultSet.getTimestamp("event_time"));
        score.setLabelFraud(resultSet.getInt("label_fraud") == 1);
        score.setModelVersion(resultSet.getString("model_version"));
        score.setFeatureVersion(resultSet.getString("feature_version"));
        score.setRiskScore(resultSet.getInt("risk_score"));
        score.setRiskProbability(resultSet.getBigDecimal("risk_probability"));
        score.setDecision(resultSet.getString("decision"));
        score.setReviewThreshold(resultSet.getBigDecimal("review_threshold"));
        score.setBlockThreshold(resultSet.getBigDecimal("block_threshold"));
        score.setReasonJson(resultSet.getString("reason_json"));
        score.setScoredAt(resultSet.getTimestamp("scored_at"));
        score.setNeighborhoodBlockCount(resultSet.getInt("neighborhood_block_count"));
        score.setNeighborhoodReviewCount(resultSet.getInt("neighborhood_review_count"));
        score.setNeighborhoodFraudCount(resultSet.getInt("neighborhood_fraud_count"));
        return score;
    }

    private AdminRiskGraphModelGovernanceView mapGovernance(ResultSet resultSet) throws SQLException {
        AdminRiskGraphModelGovernanceView view = new AdminRiskGraphModelGovernanceView();
        view.setModelVersion(resultSet.getString("model_version"));
        view.setModelRole(resultSet.getString("model_role"));
        view.setLifecycleStatus(resultSet.getString("lifecycle_status"));
        view.setOnlineMode(resultSet.getString("online_mode"));
        view.setOperational(resultSet.getInt("is_operational") == 1);
        view.setGovernanceNote(resultSet.getString("governance_note"));
        long adminUserId = resultSet.getLong("promoted_by_admin_user_id");
        view.setPromotedByAdminUserId(resultSet.wasNull() ? null : Long.valueOf(adminUserId));
        view.setPromotedAt(resultSet.getTimestamp("promoted_at"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        view.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return view;
    }

    private AdminRiskGraphModelGovernanceView defaultGovernance(String modelVersion) {
        AdminRiskGraphModelGovernanceView view = new AdminRiskGraphModelGovernanceView();
        view.setModelVersion(modelVersion);
        view.setModelRole("EXPERIMENT");
        view.setLifecycleStatus("EVALUATING");
        view.setOnlineMode("OFFLINE_REVIEW");
        view.setOperational(false);
        return view;
    }
}
