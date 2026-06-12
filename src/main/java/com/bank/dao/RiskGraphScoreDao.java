package com.bank.dao;

import com.bank.dto.AdminRiskGraphScoreSummary;
import com.bank.dto.AdminRiskGraphScoreView;
import com.bank.dto.AdminRiskGraphNeighborhood;
import com.bank.dto.AdminRiskGraphCapacityMetric;
import com.bank.dto.AdminRiskGraphModelGovernanceView;
import com.bank.dto.AdminRiskGraphModelVersionMetric;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface RiskGraphScoreDao {
    void ensureGovernanceTable(Connection connection) throws SQLException;

    List<String> findModelVersions(Connection connection) throws SQLException;

    String findOperationalModelVersion(Connection connection) throws SQLException;

    void insertMissingGovernance(Connection connection, List<String> modelVersions) throws SQLException;

    void promoteDefaultOperationalModel(Connection connection, String modelVersion) throws SQLException;

    List<AdminRiskGraphModelGovernanceView> loadModelGovernance(Connection connection, List<String> modelVersions)
            throws SQLException;

    void updateModelGovernance(Connection connection, String modelVersion, String modelRole, String lifecycleStatus,
                               String onlineMode, boolean operational, String governanceNote, long adminUserId)
            throws SQLException;

    List<String> findEdgeTypes(Connection connection, String modelVersion) throws SQLException;

    AdminRiskGraphScoreSummary loadSummary(Connection connection, String modelVersion) throws SQLException;

    List<AdminRiskGraphModelVersionMetric> loadModelMetrics(Connection connection, List<String> modelVersions)
            throws SQLException;

    List<AdminRiskGraphCapacityMetric> loadCapacityMetrics(Connection connection, List<String> modelVersions,
                                                           int[] capacities) throws SQLException;

    List<AdminRiskGraphScoreView> findScores(Connection connection, String modelVersion, String decision,
                                             String edgeType, Integer minScore, Integer labelFraud,
                                             int limit) throws SQLException;

    AdminRiskGraphNeighborhood loadNeighborhood(Connection connection, long graphEdgeId, String modelVersion,
                                                int edgeLimit) throws SQLException;
}
