package com.bank.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminRiskGraphModelComparePage {
    private List<String> modelVersions = new ArrayList<String>();
    private List<AdminRiskGraphModelGovernanceView> modelGovernance =
            new ArrayList<AdminRiskGraphModelGovernanceView>();
    private List<AdminRiskGraphModelVersionMetric> modelMetrics =
            new ArrayList<AdminRiskGraphModelVersionMetric>();
    private List<AdminRiskGraphCapacityMetric> capacityMetrics =
            new ArrayList<AdminRiskGraphCapacityMetric>();
    private String baselineModelVersion;
    private String candidateModelVersion;
    private String operationalModelVersion;
    private int reviewCapacity;
    private int blockCapacity;
    private AdminRiskGraphThresholdPlan thresholdPlan;

    public List<String> getModelVersions() {
        return modelVersions;
    }

    public void setModelVersions(List<String> modelVersions) {
        this.modelVersions = modelVersions == null ? new ArrayList<String>() : modelVersions;
    }

    public List<AdminRiskGraphModelGovernanceView> getModelGovernance() {
        return modelGovernance;
    }

    public void setModelGovernance(List<AdminRiskGraphModelGovernanceView> modelGovernance) {
        this.modelGovernance = modelGovernance == null
                ? new ArrayList<AdminRiskGraphModelGovernanceView>() : modelGovernance;
    }

    public List<AdminRiskGraphModelVersionMetric> getModelMetrics() {
        return modelMetrics;
    }

    public void setModelMetrics(List<AdminRiskGraphModelVersionMetric> modelMetrics) {
        this.modelMetrics = modelMetrics == null
                ? new ArrayList<AdminRiskGraphModelVersionMetric>() : modelMetrics;
    }

    public List<AdminRiskGraphCapacityMetric> getCapacityMetrics() {
        return capacityMetrics;
    }

    public void setCapacityMetrics(List<AdminRiskGraphCapacityMetric> capacityMetrics) {
        this.capacityMetrics = capacityMetrics == null
                ? new ArrayList<AdminRiskGraphCapacityMetric>() : capacityMetrics;
    }

    public String getBaselineModelVersion() {
        return baselineModelVersion;
    }

    public void setBaselineModelVersion(String baselineModelVersion) {
        this.baselineModelVersion = baselineModelVersion;
    }

    public String getCandidateModelVersion() {
        return candidateModelVersion;
    }

    public void setCandidateModelVersion(String candidateModelVersion) {
        this.candidateModelVersion = candidateModelVersion;
    }

    public String getOperationalModelVersion() {
        return operationalModelVersion;
    }

    public void setOperationalModelVersion(String operationalModelVersion) {
        this.operationalModelVersion = operationalModelVersion;
    }

    public int getReviewCapacity() {
        return reviewCapacity;
    }

    public void setReviewCapacity(int reviewCapacity) {
        this.reviewCapacity = reviewCapacity;
    }

    public int getBlockCapacity() {
        return blockCapacity;
    }

    public void setBlockCapacity(int blockCapacity) {
        this.blockCapacity = blockCapacity;
    }

    public AdminRiskGraphThresholdPlan getThresholdPlan() {
        return thresholdPlan;
    }

    public void setThresholdPlan(AdminRiskGraphThresholdPlan thresholdPlan) {
        this.thresholdPlan = thresholdPlan;
    }
}
