package com.bank.service.impl;

import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.RiskGraphReviewCaseDao;
import com.bank.dao.RiskGraphScoreDao;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.RiskGraphReviewCaseDaoImpl;
import com.bank.dao.impl.RiskGraphScoreDaoImpl;
import com.bank.dto.AdminRiskGraphReviewCasePage;
import com.bank.dto.AdminRiskGraphReviewCaseView;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphReviewCaseService;
import com.bank.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RiskGraphReviewCaseServiceImpl implements RiskGraphReviewCaseService {
    private static final int CASE_QUERY_LIMIT = 200;
    private static final int MATERIALIZE_LIMIT = 5000;
    private static final int FEEDBACK_EXPORT_LIMIT = 50000;

    private final RiskGraphScoreDao riskGraphScoreDao = new RiskGraphScoreDaoImpl();
    private final RiskGraphReviewCaseDao reviewCaseDao = new RiskGraphReviewCaseDaoImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();

    @Override
    public ServiceResult<AdminRiskGraphReviewCasePage> queryCases(String modelVersion, String caseStatus,
                                                                  String caseType, String reviewResult) {
        String normalizedStatus = normalizeStatusFilter(caseStatus);
        String normalizedCaseType = normalizeCaseType(caseType);
        String normalizedReviewResult = normalizeReviewResultFilter(reviewResult);
        String normalizedModelVersion = normalize(modelVersion);

        try (Connection connection = DBUtil.getConnection()) {
            List<String> versions = riskGraphScoreDao.findModelVersions(connection);
            String operationalModelVersion = prepareGovernance(connection, versions);
            normalizedModelVersion = resolveModelVersion(normalizedModelVersion, versions, operationalModelVersion);

            AdminRiskGraphReviewCasePage page = new AdminRiskGraphReviewCasePage();
            page.setModelVersions(versions);
            page.setSelectedModelVersion(normalizedModelVersion);
            page.setOperationalModelVersion(operationalModelVersion);
            page.setCases(reviewCaseDao.findCases(connection, normalizedModelVersion, normalizedStatus,
                    normalizedCaseType, normalizedReviewResult, CASE_QUERY_LIMIT));

            Map<String, Integer> counts = reviewCaseDao.countByStatus(connection, normalizedModelVersion);
            page.setOpenCount(count(counts, "OPEN"));
            page.setReviewedCount(count(counts, "REVIEWED"));
            page.setIgnoredCount(count(counts, "IGNORED"));
            page.setConflictCount(reviewCaseDao.countConflicts(connection, normalizedModelVersion));
            Map<String, Integer> feedbackCounts =
                    reviewCaseDao.countByReviewResult(connection, normalizedModelVersion);
            page.setConfirmedRiskCount(count(feedbackCounts, "CONFIRMED_RISK"));
            page.setFalsePositiveCount(count(feedbackCounts, "FALSE_POSITIVE"));
            page.setNeedMoreDataCount(count(feedbackCounts, "NEED_MORE_DATA"));
            page.setIgnoredFeedbackCount(count(feedbackCounts, "IGNORE"));
            return ServiceResult.success("GNN review cases queried.", page);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("GNN复核队列查询失败，请检查数据库状态或稍后重试。");
        }
    }

    @Override
    public ServiceResult<Integer> materializeCases(String modelVersion, long adminUserId, String ipAddress) {
        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);
            List<String> versions = riskGraphScoreDao.findModelVersions(connection);
            String operationalModelVersion = prepareGovernance(connection, versions);
            String normalizedModelVersion = resolveModelVersion(normalize(modelVersion), versions,
                    operationalModelVersion);
            if (normalizedModelVersion == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("当前还没有可用的GNN模型评分，无法生成复核队列。");
            }
            int affected = reviewCaseDao.materializeCandidates(connection, normalizedModelVersion, MATERIALIZE_LIMIT);
            adminAuditLogDao.insert(connection, adminUserId, "MATERIALIZE_RISK_GRAPH_CASE",
                    "RISK_GRAPH_REVIEW_CASE", normalizedModelVersion,
                    "同步GNN复核候选样本，模型版本：" + normalizedModelVersion + "，影响行数：" + affected,
                    ipAddress);
            connection.commit();
            return ServiceResult.success("已同步GNN复核候选样本：" + affected + " 条。", Integer.valueOf(affected));
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("GNN复核候选样本生成失败，请检查数据库状态或稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<List<AdminRiskGraphReviewCaseView>> exportFeedbackCases(String modelVersion,
                                                                                 String reviewResult,
                                                                                 long adminUserId,
                                                                                 String ipAddress) {
        String normalizedModelVersion = normalize(modelVersion);
        String normalizedReviewResult = normalizeReviewResultFilter(reviewResult);
        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);
            List<String> versions = riskGraphScoreDao.findModelVersions(connection);
            String operationalModelVersion = prepareGovernance(connection, versions);
            normalizedModelVersion = resolveModelVersion(normalizedModelVersion, versions, operationalModelVersion);
            if (normalizedModelVersion == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("当前还没有可导出的GNN模型反馈样本。");
            }
            List<AdminRiskGraphReviewCaseView> cases = reviewCaseDao.findFeedbackCases(connection,
                    normalizedModelVersion, normalizedReviewResult, FEEDBACK_EXPORT_LIMIT);
            adminAuditLogDao.insert(connection, adminUserId, "EXPORT_RISK_GRAPH_FEEDBACK",
                    "RISK_GRAPH_FEEDBACK", normalizedModelVersion,
                    "导出GNN复核反馈样本，模型版本：" + normalizedModelVersion
                            + "，人工结论：" + (normalizedReviewResult == null ? "ALL" : normalizedReviewResult)
                            + "，样本数：" + cases.size(),
                    ipAddress);
            connection.commit();
            return ServiceResult.success("GNN复核反馈样本已导出。", cases);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("GNN复核反馈样本导出失败，请稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    @Override
    public ServiceResult<AdminRiskGraphReviewCaseView> getCase(String caseId) {
        Long parsedCaseId = parseId(caseId);
        if (parsedCaseId == null) {
            return ServiceResult.failure("请选择一条有效的复核样本。");
        }
        try (Connection connection = DBUtil.getConnection()) {
            AdminRiskGraphReviewCaseView view = reviewCaseDao.findById(connection, parsedCaseId.longValue());
            if (view == null) {
                return ServiceResult.failure("复核样本不存在。");
            }
            return ServiceResult.success("GNN review case queried.", view);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("复核样本详情查询失败，请稍后重试。");
        }
    }

    @Override
    public ServiceResult<Void> reviewCase(long adminUserId, String caseId, String reviewResult,
                                          String reviewNote, String ipAddress) {
        Long parsedCaseId = parseId(caseId);
        if (parsedCaseId == null) {
            return ServiceResult.failure("请选择一条有效的复核样本。");
        }
        String normalizedResult = normalizeReviewResult(reviewResult);
        if (normalizedResult == null) {
            return ServiceResult.failure("请选择有效的人工复核结论。");
        }
        String normalizedNote = trimToLength(reviewNote, 1000, null);
        String targetStatus = statusForReviewResult(normalizedResult);

        Connection connection = null;
        try {
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);
            AdminRiskGraphReviewCaseView view =
                    reviewCaseDao.findByIdForUpdate(connection, parsedCaseId.longValue());
            if (view == null) {
                rollbackQuietly(connection);
                return ServiceResult.failure("复核样本不存在或已被删除。");
            }

            reviewCaseDao.updateReview(connection, parsedCaseId.longValue(), targetStatus,
                    normalizedResult, normalizedNote, adminUserId);
            adminAuditLogDao.insert(connection, adminUserId, "REVIEW_RISK_GRAPH_CASE",
                    "RISK_GRAPH_REVIEW_CASE", String.valueOf(parsedCaseId),
                    "处理GNN复核样本，图边：" + view.getGraphEdgeId() + "，模型版本：" + view.getModelVersion()
                            + "，结论：" + normalizedResult + "，状态：" + targetStatus,
                    ipAddress);
            connection.commit();
            return ServiceResult.success("复核结论已记录，后续可作为模型阈值校准和再训练反馈。", null);
        } catch (SQLException e) {
            rollbackQuietly(connection);
            e.printStackTrace();
            return ServiceResult.failure("复核结论保存失败，请稍后重试。");
        } finally {
            closeQuietly(connection);
        }
    }

    private String prepareGovernance(Connection connection, List<String> versions) throws SQLException {
        riskGraphScoreDao.ensureGovernanceTable(connection);
        riskGraphScoreDao.insertMissingGovernance(connection, versions);
        String operationalModelVersion = riskGraphScoreDao.findOperationalModelVersion(connection);
        if (versions != null && !versions.isEmpty()
                && (operationalModelVersion == null || !versions.contains(operationalModelVersion))) {
            operationalModelVersion = versions.get(0);
            riskGraphScoreDao.promoteDefaultOperationalModel(connection, operationalModelVersion);
        }
        return operationalModelVersion;
    }

    private String resolveModelVersion(String modelVersion, List<String> versions, String operationalModelVersion) {
        if (modelVersion != null && versions != null && versions.contains(modelVersion)) {
            return modelVersion;
        }
        if (operationalModelVersion != null && versions != null && versions.contains(operationalModelVersion)) {
            return operationalModelVersion;
        }
        return versions == null || versions.isEmpty() ? null : versions.get(0);
    }

    private int count(Map<String, Integer> counts, String status) {
        Integer value = counts == null ? null : counts.get(status);
        return value == null ? 0 : value.intValue();
    }

    private String normalize(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStatusFilter(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return "OPEN";
        }
        if ("ALL".equals(normalized)) {
            return null;
        }
        if ("OPEN".equals(normalized) || "REVIEWED".equals(normalized) || "IGNORED".equals(normalized)) {
            return normalized;
        }
        return "OPEN";
    }

    private String normalizeCaseType(String caseType) {
        String normalized = normalize(caseType);
        if (normalized == null || "ALL".equals(normalized)) {
            return null;
        }
        if ("LABEL_CONFLICT".equals(normalized) || "TRUE_POSITIVE_BLOCK".equals(normalized)
                || "STRONG_REVIEW".equals(normalized) || "MODEL_REVIEW".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeReviewResultFilter(String reviewResult) {
        String normalized = normalize(reviewResult);
        if (normalized == null || "ALL".equals(normalized)) {
            return null;
        }
        return normalizeReviewResult(normalized);
    }

    private String normalizeReviewResult(String reviewResult) {
        String normalized = normalize(reviewResult);
        if ("CONFIRMED_RISK".equals(normalized) || "FALSE_POSITIVE".equals(normalized)
                || "NEED_MORE_DATA".equals(normalized) || "IGNORE".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String statusForReviewResult(String reviewResult) {
        if ("IGNORE".equals(reviewResult)) {
            return "IGNORED";
        }
        if ("NEED_MORE_DATA".equals(reviewResult)) {
            return "OPEN";
        }
        return "REVIEWED";
    }

    private String trimToLength(String value, int maxLength, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return defaultValue;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private Long parseId(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            long id = Long.parseLong(normalized);
            return id > 0 ? Long.valueOf(id) : null;
        } catch (NumberFormatException e) {
            return null;
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
}
