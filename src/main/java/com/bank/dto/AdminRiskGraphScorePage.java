package com.bank.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminRiskGraphScorePage {
    private AdminRiskGraphScoreSummary summary;
    private List<AdminRiskGraphScoreView> scores = new ArrayList<AdminRiskGraphScoreView>();
    private List<String> modelVersions = new ArrayList<String>();
    private List<String> edgeTypes = new ArrayList<String>();
    private String selectedModelVersion;
    private String operationalModelVersion;

    public AdminRiskGraphScoreSummary getSummary() {
        return summary;
    }

    public void setSummary(AdminRiskGraphScoreSummary summary) {
        this.summary = summary;
    }

    public List<AdminRiskGraphScoreView> getScores() {
        return scores;
    }

    public void setScores(List<AdminRiskGraphScoreView> scores) {
        this.scores = scores;
    }

    public List<String> getModelVersions() {
        return modelVersions;
    }

    public void setModelVersions(List<String> modelVersions) {
        this.modelVersions = modelVersions;
    }

    public List<String> getEdgeTypes() {
        return edgeTypes;
    }

    public void setEdgeTypes(List<String> edgeTypes) {
        this.edgeTypes = edgeTypes;
    }

    public String getSelectedModelVersion() {
        return selectedModelVersion;
    }

    public void setSelectedModelVersion(String selectedModelVersion) {
        this.selectedModelVersion = selectedModelVersion;
    }

    public String getOperationalModelVersion() {
        return operationalModelVersion;
    }

    public void setOperationalModelVersion(String operationalModelVersion) {
        this.operationalModelVersion = operationalModelVersion;
    }
}
