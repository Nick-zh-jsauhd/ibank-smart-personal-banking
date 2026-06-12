package com.bank.dao;

import com.bank.dto.AdminRiskGraphReviewCaseView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface RiskGraphReviewCaseDao {
    int materializeCandidates(Connection connection, String modelVersion, int limit) throws SQLException;

    List<AdminRiskGraphReviewCaseView> findCases(Connection connection, String modelVersion, String caseStatus,
                                                 String caseType, String reviewResult, int limit)
            throws SQLException;

    List<AdminRiskGraphReviewCaseView> findFeedbackCases(Connection connection, String modelVersion,
                                                         String reviewResult, int limit)
            throws SQLException;

    AdminRiskGraphReviewCaseView findById(Connection connection, long caseId) throws SQLException;

    AdminRiskGraphReviewCaseView findByIdForUpdate(Connection connection, long caseId) throws SQLException;

    Map<String, Integer> countByStatus(Connection connection, String modelVersion) throws SQLException;

    Map<String, Integer> countByReviewResult(Connection connection, String modelVersion) throws SQLException;

    int countConflicts(Connection connection, String modelVersion) throws SQLException;

    void updateReview(Connection connection, long caseId, String caseStatus, String reviewResult,
                      String reviewNote, long adminUserId) throws SQLException;
}
