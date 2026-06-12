package com.bank.service;

import com.bank.dto.AdminRiskGraphScorePage;
import com.bank.dto.AdminRiskGraphNeighborhood;
import com.bank.dto.AdminRiskGraphModelComparePage;
import com.bank.dto.ServiceResult;

public interface RiskGraphScoreService {
    ServiceResult<AdminRiskGraphScorePage> queryScores(String modelVersion, String decision, String edgeType,
                                                       String minScore, String labelFraud);

    ServiceResult<AdminRiskGraphModelComparePage> queryModelComparison(String baselineModelVersion,
                                                                       String candidateModelVersion);

    ServiceResult<AdminRiskGraphModelComparePage> queryModelComparison(String baselineModelVersion,
                                                                       String candidateModelVersion,
                                                                       String reviewCapacity,
                                                                       String blockCapacity);

    ServiceResult<Void> updateModelGovernance(String modelVersion, String modelRole, String lifecycleStatus,
                                              String onlineMode, boolean operational, String governanceNote,
                                              long adminUserId, String ipAddress);

    ServiceResult<AdminRiskGraphNeighborhood> queryNeighborhood(String graphEdgeId, String modelVersion);
}
