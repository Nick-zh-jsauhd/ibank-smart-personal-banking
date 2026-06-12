package com.bank.service.impl;

import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.RiskGraphScoreDao;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.RiskGraphScoreDaoImpl;
import com.bank.dto.AdminRiskGraphEdgeView;
import com.bank.dto.AdminRiskGraphCapacityMetric;
import com.bank.dto.AdminRiskGraphModelComparePage;
import com.bank.dto.AdminRiskGraphNeighborhood;
import com.bank.dto.AdminRiskGraphScorePage;
import com.bank.dto.AdminRiskGraphScoreSummary;
import com.bank.dto.AdminRiskGraphScoreView;
import com.bank.dto.AdminRiskGraphThresholdPlan;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphScoreService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RiskGraphScoreServiceImpl implements RiskGraphScoreService {
    private static final int SCORE_QUERY_LIMIT = 200;
    private static final int NEIGHBORHOOD_EDGE_LIMIT = 100;
    private static final int[] CAPACITY_BUCKETS = new int[]{100, 500, 1000, 2000, 5000, 10000, 20000};
    private static final int DEFAULT_REVIEW_CAPACITY = 5000;
    private static final int DEFAULT_BLOCK_CAPACITY = 100;
    private static final int MAX_THRESHOLD_CAPACITY = 200000;

    private final RiskGraphScoreDao riskGraphScoreDao = new RiskGraphScoreDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();

    @Override
    public ServiceResult<AdminRiskGraphScorePage> queryScores(String modelVersion, String decision, String edgeType,
                                                              String minScore, String labelFraud) {
        String normalizedModelVersion = normalize(modelVersion);
        String normalizedDecision = normalizeDecision(decision);
        String normalizedEdgeType = normalize(edgeType);
        Integer normalizedMinScore;
        Integer normalizedLabelFraud;
        try {
            normalizedMinScore = normalizeMinScore(minScore);
            normalizedLabelFraud = normalizeLabelFraud(labelFraud);
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }

        try (Connection connection = DBUtil.getConnection()) {
            AdminRiskGraphScorePage page = new AdminRiskGraphScorePage();
            List<String> modelVersions = riskGraphScoreDao.findModelVersions(connection);
            String operationalModelVersion = prepareGovernance(connection, modelVersions);
            normalizedModelVersion = resolveModelVersion(normalizedModelVersion, modelVersions,
                    operationalModelVersion);
            AdminRiskGraphScoreSummary summary =
                    riskGraphScoreDao.loadSummary(connection, normalizedModelVersion);
            page.setModelVersions(modelVersions);
            page.setEdgeTypes(riskGraphScoreDao.findEdgeTypes(connection, normalizedModelVersion));
            page.setSelectedModelVersion(normalizedModelVersion);
            page.setOperationalModelVersion(operationalModelVersion);
            page.setSummary(summary);
            List<AdminRiskGraphScoreView> scores = riskGraphScoreDao.findScores(connection, normalizedModelVersion,
                    normalizedDecision, normalizedEdgeType, normalizedMinScore, normalizedLabelFraud, SCORE_QUERY_LIMIT);
            applyBusinessDecisions(scores);
            page.setScores(scores);
            return ServiceResult.success("GNN risk graph scores queried.", page);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("GNN评分查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<AdminRiskGraphModelComparePage> queryModelComparison(String baselineModelVersion,
                                                                              String candidateModelVersion) {
        return queryModelComparison(baselineModelVersion, candidateModelVersion, null, null);
    }

    @Override
    public ServiceResult<AdminRiskGraphModelComparePage> queryModelComparison(String baselineModelVersion,
                                                                              String candidateModelVersion,
                                                                              String reviewCapacity,
                                                                              String blockCapacity) {
        String normalizedBaseline = normalize(baselineModelVersion);
        String normalizedCandidate = normalize(candidateModelVersion);
        int normalizedReviewCapacity;
        int normalizedBlockCapacity;
        try {
            normalizedReviewCapacity = normalizeCapacity(reviewCapacity, DEFAULT_REVIEW_CAPACITY, "复核容量");
            normalizedBlockCapacity = normalizeCapacity(blockCapacity, DEFAULT_BLOCK_CAPACITY, "拦截容量");
            if (normalizedBlockCapacity > normalizedReviewCapacity) {
                throw new IllegalArgumentException("拦截容量不能大于复核容量，否则 BLOCK 队列无法作为 REVIEW 队列的高危子集。");
            }
        } catch (IllegalArgumentException e) {
            return ServiceResult.failure(e.getMessage());
        }
        try (Connection connection = DBUtil.getConnection()) {
            List<String> modelVersions = riskGraphScoreDao.findModelVersions(connection);
            String operationalModelVersion = prepareGovernance(connection, modelVersions);
            if (modelVersions.isEmpty()) {
                AdminRiskGraphModelComparePage emptyPage = new AdminRiskGraphModelComparePage();
                emptyPage.setModelVersions(modelVersions);
                emptyPage.setOperationalModelVersion(operationalModelVersion);
                emptyPage.setReviewCapacity(normalizedReviewCapacity);
                emptyPage.setBlockCapacity(normalizedBlockCapacity);
                return ServiceResult.success("GNN model comparison queried.", emptyPage);
            }

            normalizedCandidate = resolveModelVersion(normalizedCandidate, modelVersions, operationalModelVersion);
            normalizedBaseline = resolveBaselineModelVersion(normalizedBaseline, modelVersions, normalizedCandidate,
                    operationalModelVersion);

            List<String> selectedVersions = selectedVersions(normalizedBaseline, normalizedCandidate);
            AdminRiskGraphModelComparePage page = new AdminRiskGraphModelComparePage();
            page.setModelVersions(modelVersions);
            page.setModelGovernance(riskGraphScoreDao.loadModelGovernance(connection, modelVersions));
            page.setOperationalModelVersion(operationalModelVersion);
            page.setBaselineModelVersion(normalizedBaseline);
            page.setCandidateModelVersion(normalizedCandidate);
            page.setReviewCapacity(normalizedReviewCapacity);
            page.setBlockCapacity(normalizedBlockCapacity);
            page.setModelMetrics(riskGraphScoreDao.loadModelMetrics(connection, selectedVersions));
            page.setCapacityMetrics(riskGraphScoreDao.loadCapacityMetrics(connection, selectedVersions,
                    CAPACITY_BUCKETS));
            page.setThresholdPlan(loadThresholdPlan(connection, normalizedCandidate,
                    normalizedReviewCapacity, normalizedBlockCapacity));
            return ServiceResult.success("GNN model comparison queried.", page);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("GNN模型对比查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> updateModelGovernance(String modelVersion, String modelRole, String lifecycleStatus,
                                                     String onlineMode, boolean operational, String governanceNote,
                                                     long adminUserId, String ipAddress) {
        String normalizedModelVersion = normalize(modelVersion);
        if (normalizedModelVersion == null) {
            return ServiceResult.failure("Please select a valid graph model version.");
        }
        String normalizedRole = normalizeModelRole(modelRole, operational);
        String normalizedLifecycle = normalizeLifecycleStatus(lifecycleStatus, operational);
        String normalizedOnlineMode = normalizeOnlineMode(onlineMode);
        String normalizedNote = trimToLength(governanceNote, 500);
        if (operational && "ARCHIVED".equals(normalizedRole)) {
            return ServiceResult.failure("Archived model cannot be set as the operational model.");
        }
        if (operational && "RETIRED".equals(normalizedLifecycle)) {
            return ServiceResult.failure("Retired model cannot be set as the operational model.");
        }

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);
            List<String> modelVersions = riskGraphScoreDao.findModelVersions(connection);
            prepareGovernance(connection, modelVersions);
            if (!modelVersions.contains(normalizedModelVersion)) {
                rollbackQuietly(connection);
                return ServiceResult.failure("The selected graph model version has no imported score data.");
            }
            riskGraphScoreDao.updateModelGovernance(connection, normalizedModelVersion, normalizedRole,
                    normalizedLifecycle, normalizedOnlineMode, operational, normalizedNote, adminUserId);
            adminAuditLogDao.insert(connection, adminUserId, "UPDATE_RISK_GRAPH_MODEL_GOVERNANCE",
                    "RISK_GRAPH_MODEL", normalizedModelVersion,
                    "role=" + normalizedRole + ", lifecycle=" + normalizedLifecycle
                            + ", onlineMode=" + normalizedOnlineMode + ", operational=" + operational,
                    ipAddress);
            connection.commit();
            return ServiceResult.success("Risk graph model governance updated.", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("Risk graph model governance update failed.");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<AdminRiskGraphNeighborhood> queryNeighborhood(String graphEdgeId, String modelVersion) {
        long parsedEdgeId;
        try {
            parsedEdgeId = Long.parseLong(normalize(graphEdgeId));
        } catch (RuntimeException e) {
            return ServiceResult.failure("请选择一条有效的图边记录。");
        }

        String normalizedModelVersion = normalize(modelVersion);
        try (Connection connection = DBUtil.getConnection()) {
            List<String> modelVersions = riskGraphScoreDao.findModelVersions(connection);
            String operationalModelVersion = prepareGovernance(connection, modelVersions);
            normalizedModelVersion = resolveModelVersion(normalizedModelVersion, modelVersions,
                    operationalModelVersion);
            if (normalizedModelVersion == null) {
                return ServiceResult.failure("当前还没有可用的 GNN 模型评分。");
            }
            AdminRiskGraphNeighborhood graph = riskGraphScoreDao.loadNeighborhood(connection, parsedEdgeId,
                    normalizedModelVersion, NEIGHBORHOOD_EDGE_LIMIT);
            if (graph.getEdges().isEmpty()) {
                return ServiceResult.failure("没有找到这条图边的邻域证据。");
            }
            applyBusinessDecisions(graph);
            return ServiceResult.success("Risk graph neighborhood queried.", graph);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("图谱证据查询失败，请稍后重试。");
        }
    }

    private String normalize(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    private String prepareGovernance(Connection connection, List<String> modelVersions) throws SQLException {
        riskGraphScoreDao.ensureGovernanceTable(connection);
        riskGraphScoreDao.insertMissingGovernance(connection, modelVersions);
        String operationalModelVersion = riskGraphScoreDao.findOperationalModelVersion(connection);
        if (modelVersions != null && !modelVersions.isEmpty()
                && (operationalModelVersion == null || !modelVersions.contains(operationalModelVersion))) {
            operationalModelVersion = modelVersions.get(0);
            riskGraphScoreDao.promoteDefaultOperationalModel(connection, operationalModelVersion);
        }
        return operationalModelVersion;
    }

    private String resolveModelVersion(String requestedModelVersion, List<String> modelVersions,
                                       String operationalModelVersion) {
        if (requestedModelVersion != null && modelVersions != null && modelVersions.contains(requestedModelVersion)) {
            return requestedModelVersion;
        }
        if (operationalModelVersion != null && modelVersions != null
                && modelVersions.contains(operationalModelVersion)) {
            return operationalModelVersion;
        }
        return modelVersions == null || modelVersions.isEmpty() ? null : modelVersions.get(0);
    }

    private String resolveBaselineModelVersion(String requestedBaseline, List<String> modelVersions,
                                               String candidateModelVersion, String operationalModelVersion) {
        if (requestedBaseline != null && modelVersions != null && modelVersions.contains(requestedBaseline)) {
            return requestedBaseline;
        }
        if (operationalModelVersion != null && !operationalModelVersion.equals(candidateModelVersion)
                && modelVersions != null && modelVersions.contains(operationalModelVersion)) {
            return operationalModelVersion;
        }
        if (modelVersions != null) {
            for (String modelVersion : modelVersions) {
                if (!modelVersion.equals(candidateModelVersion)) {
                    return modelVersion;
                }
            }
        }
        return candidateModelVersion;
    }

    private String normalizeDecision(String decision) {
        String normalized = normalize(decision);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase();
        if ("PASS".equals(normalized) || "REVIEW".equals(normalized) || "BLOCK".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeModelRole(String role, boolean operational) {
        if (operational) {
            return "OPERATING";
        }
        String normalized = normalize(role);
        if ("OPERATING".equals(normalized)) {
            return "CANDIDATE";
        }
        if ("CANDIDATE".equals(normalized) || "EXPERIMENT".equals(normalized)
                || "ARCHIVED".equals(normalized)) {
            return normalized;
        }
        return "EXPERIMENT";
    }

    private String normalizeLifecycleStatus(String status, boolean operational) {
        if (operational) {
            return "APPROVED";
        }
        String normalized = normalize(status);
        if ("EVALUATING".equals(normalized) || "APPROVED".equals(normalized)
                || "SHADOW".equals(normalized) || "RETIRED".equals(normalized)) {
            return normalized;
        }
        return "EVALUATING";
    }

    private String normalizeOnlineMode(String mode) {
        String normalized = normalize(mode);
        if ("OFFLINE_REVIEW".equals(normalized) || "SHADOW".equals(normalized)
                || "ONLINE_ASSIST".equals(normalized)) {
            return normalized;
        }
        return "OFFLINE_REVIEW";
    }

    private Integer normalizeMinScore(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        int score = Integer.parseInt(normalized);
        if (score < 0 || score > 1000) {
            throw new IllegalArgumentException("最低评分必须在 0 到 1000 之间。");
        }
        return score;
    }

    private Integer normalizeLabelFraud(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if ("1".equals(normalized) || "true".equalsIgnoreCase(normalized)) {
            return 1;
        }
        if ("0".equals(normalized) || "false".equalsIgnoreCase(normalized)) {
            return 0;
        }
        return null;
    }

    private int normalizeCapacity(String value, int defaultValue, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            return defaultValue;
        }
        int capacity;
        try {
            capacity = Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是整数。");
        }
        if (capacity < 1 || capacity > MAX_THRESHOLD_CAPACITY) {
            throw new IllegalArgumentException(fieldName + "必须在 1 到 "
                    + MAX_THRESHOLD_CAPACITY + " 之间。");
        }
        return capacity;
    }

    private AdminRiskGraphThresholdPlan loadThresholdPlan(Connection connection, String modelVersion,
                                                         int reviewCapacity, int blockCapacity)
            throws SQLException {
        AdminRiskGraphThresholdPlan plan = new AdminRiskGraphThresholdPlan();
        plan.setModelVersion(modelVersion);
        plan.setReviewCapacity(reviewCapacity);
        plan.setBlockCapacity(blockCapacity);

        int[] customCapacities;
        if (reviewCapacity == blockCapacity) {
            customCapacities = new int[]{reviewCapacity};
        } else {
            customCapacities = new int[]{blockCapacity, reviewCapacity};
        }
        List<String> versions = new ArrayList<String>();
        versions.add(modelVersion);
        List<AdminRiskGraphCapacityMetric> metrics =
                riskGraphScoreDao.loadCapacityMetrics(connection, versions, customCapacities);
        plan.setBlockMetric(findCapacityMetric(metrics, modelVersion, blockCapacity));
        plan.setReviewMetric(findCapacityMetric(metrics, modelVersion, reviewCapacity));
        return plan;
    }

    private AdminRiskGraphCapacityMetric findCapacityMetric(List<AdminRiskGraphCapacityMetric> metrics,
                                                            String modelVersion, int capacity) {
        for (AdminRiskGraphCapacityMetric metric : metrics) {
            if (capacity == metric.getCapacity() && modelVersion.equals(metric.getModelVersion())) {
                return metric;
            }
        }
        return null;
    }

    private List<String> selectedVersions(String baselineModelVersion, String candidateModelVersion) {
        Set<String> selected = new LinkedHashSet<String>();
        if (baselineModelVersion != null) {
            selected.add(baselineModelVersion);
        }
        if (candidateModelVersion != null) {
            selected.add(candidateModelVersion);
        }
        return new ArrayList<String>(selected);
    }

    private String trimToLength(String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
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

    private void applyBusinessDecisions(List<AdminRiskGraphScoreView> scores) {
        for (AdminRiskGraphScoreView score : scores) {
            BusinessDecision decision = decide(score.isLabelFraud(), score.getDecision(), score.getRiskScore(),
                    score.getNeighborhoodBlockCount(), score.getNeighborhoodReviewCount(),
                    score.getNeighborhoodFraudCount());
            score.setBusinessDecision(decision.code);
            score.setBusinessDecisionLabel(decision.label);
            score.setBusinessDecisionReason(decision.reason);
            score.setBusinessDecisionSeverity(decision.severity);
            score.setLabelConflict(decision.labelConflict);
        }
    }

    private void applyBusinessDecisions(AdminRiskGraphNeighborhood graph) {
        int blockCount = graph.getBlockEdgeCount();
        int reviewCount = graph.getReviewEdgeCount();
        int fraudCount = graph.getFraudEdgeCount();
        for (AdminRiskGraphEdgeView edge : graph.getEdges()) {
            BusinessDecision decision = decide(edge.isLabelFraud(), edge.getDecision(), edge.getRiskScore(),
                    blockCount, reviewCount, fraudCount);
            edge.setBusinessDecision(decision.code);
            edge.setBusinessDecisionLabel(decision.label);
            edge.setBusinessDecisionReason(decision.reason);
            edge.setBusinessDecisionSeverity(decision.severity);
            edge.setLabelConflict(decision.labelConflict);
        }
    }

    private BusinessDecision decide(boolean labelFraud, String modelDecision, int riskScore,
                                    int neighborhoodBlockCount, int neighborhoodReviewCount,
                                    int neighborhoodFraudCount) {
        boolean modelBlock = "BLOCK".equals(modelDecision) || riskScore >= 900;
        boolean modelReview = "REVIEW".equals(modelDecision) || riskScore >= 770;
        boolean strongNeighborhood = neighborhoodBlockCount >= 3 || neighborhoodFraudCount >= 1;
        boolean mediumNeighborhood = neighborhoodReviewCount >= 3 || neighborhoodBlockCount >= 1;

        if (!labelFraud && modelBlock) {
            return new BusinessDecision("LABEL_CONFLICT", "标签冲突复核",
                    "模型给出高风险，但训练数据标签为正常；不能直接定性为洗钱，应优先人工复核图谱邻域和交易上下文。",
                    "severity-conflict", true);
        }
        if (labelFraud && modelBlock && strongNeighborhood) {
            return new BusinessDecision("INVESTIGATION_REQUIRED", "重点调查",
                    "模型高分、真实标签高危，且邻域中存在高风险或洗钱标签边；建议进入重点调查队列。",
                    "severity-critical", false);
        }
        if (modelBlock) {
            return new BusinessDecision("BLOCK", "建议阻断",
                    "模型分数达到阻断区间，业务上应结合限额规则、客户风险等级和人工复核后再处置。",
                    "severity-danger", false);
        }
        if (modelReview && (riskScore >= 850 || strongNeighborhood || mediumNeighborhood)) {
            return new BusinessDecision("STRONG_REVIEW", "强化复核",
                    "模型进入复核区间，且分数或邻域证据较强；建议提高复核优先级。",
                    "severity-high", false);
        }
        if (modelReview) {
            return new BusinessDecision("REVIEW", "人工复核",
                    "模型进入复核区间；建议保留证据并由风控人员判断是否升级。",
                    "severity-warning", false);
        }
        return new BusinessDecision("PASS", "低风险通过",
                "模型和邻域证据未达到复核阈值；保留评分记录并进入常规监控。",
                "severity-success", false);
    }

    private static class BusinessDecision {
        private final String code;
        private final String label;
        private final String reason;
        private final String severity;
        private final boolean labelConflict;

        private BusinessDecision(String code, String label, String reason, String severity, boolean labelConflict) {
            this.code = code;
            this.label = label;
            this.reason = reason;
            this.severity = severity;
            this.labelConflict = labelConflict;
        }
    }
}
