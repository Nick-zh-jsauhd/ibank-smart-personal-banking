package com.bank.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminRiskGraphReviewCasePage {
    private List<String> modelVersions = new ArrayList<String>();
    private List<AdminRiskGraphReviewCaseView> cases = new ArrayList<AdminRiskGraphReviewCaseView>();
    private String selectedModelVersion;
    private String operationalModelVersion;
    private int openCount;
    private int reviewedCount;
    private int ignoredCount;
    private int conflictCount;
    private int confirmedRiskCount;
    private int falsePositiveCount;
    private int needMoreDataCount;
    private int ignoredFeedbackCount;

    public List<String> getModelVersions() {
        return modelVersions;
    }

    public void setModelVersions(List<String> modelVersions) {
        this.modelVersions = modelVersions == null ? new ArrayList<String>() : modelVersions;
    }

    public List<AdminRiskGraphReviewCaseView> getCases() {
        return cases;
    }

    public void setCases(List<AdminRiskGraphReviewCaseView> cases) {
        this.cases = cases == null ? new ArrayList<AdminRiskGraphReviewCaseView>() : cases;
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

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }

    public int getReviewedCount() {
        return reviewedCount;
    }

    public void setReviewedCount(int reviewedCount) {
        this.reviewedCount = reviewedCount;
    }

    public int getIgnoredCount() {
        return ignoredCount;
    }

    public void setIgnoredCount(int ignoredCount) {
        this.ignoredCount = ignoredCount;
    }

    public int getConflictCount() {
        return conflictCount;
    }

    public void setConflictCount(int conflictCount) {
        this.conflictCount = conflictCount;
    }

    public int getConfirmedRiskCount() {
        return confirmedRiskCount;
    }

    public void setConfirmedRiskCount(int confirmedRiskCount) {
        this.confirmedRiskCount = confirmedRiskCount;
    }

    public int getFalsePositiveCount() {
        return falsePositiveCount;
    }

    public void setFalsePositiveCount(int falsePositiveCount) {
        this.falsePositiveCount = falsePositiveCount;
    }

    public int getNeedMoreDataCount() {
        return needMoreDataCount;
    }

    public void setNeedMoreDataCount(int needMoreDataCount) {
        this.needMoreDataCount = needMoreDataCount;
    }

    public int getIgnoredFeedbackCount() {
        return ignoredFeedbackCount;
    }

    public void setIgnoredFeedbackCount(int ignoredFeedbackCount) {
        this.ignoredFeedbackCount = ignoredFeedbackCount;
    }

    public int getTrainableFeedbackCount() {
        return confirmedRiskCount + falsePositiveCount;
    }
}
