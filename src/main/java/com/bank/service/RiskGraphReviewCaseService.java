package com.bank.service;

import com.bank.dto.AdminRiskGraphReviewCasePage;
import com.bank.dto.AdminRiskGraphReviewCaseView;
import com.bank.dto.ServiceResult;

public interface RiskGraphReviewCaseService {
    ServiceResult<AdminRiskGraphReviewCasePage> queryCases(String modelVersion, String caseStatus,
                                                           String caseType, String reviewResult);

    ServiceResult<Integer> materializeCases(String modelVersion, long adminUserId, String ipAddress);

    ServiceResult<java.util.List<AdminRiskGraphReviewCaseView>> exportFeedbackCases(String modelVersion,
                                                                                    String reviewResult,
                                                                                    long adminUserId,
                                                                                    String ipAddress);

    ServiceResult<AdminRiskGraphReviewCaseView> getCase(String caseId);

    ServiceResult<Void> reviewCase(long adminUserId, String caseId, String reviewResult,
                                   String reviewNote, String ipAddress);
}
