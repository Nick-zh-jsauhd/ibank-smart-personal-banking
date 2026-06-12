package com.bank.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AdminRiskGraphReviewCaseView {
    private long caseId;
    private long graphEdgeId;
    private String modelVersion;
    private String featureVersion;
    private String caseType;
    private String caseStatus;
    private String modelDecision;
    private String businessDecision;
    private int riskScore;
    private BigDecimal riskProbability;
    private boolean labelFraud;
    private int priority;
    private String reason;
    private String reviewResult;
    private String reviewNote;
    private Long reviewedByAdminId;
    private String reviewerUsername;
    private Timestamp reviewedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String batchCode;
    private String datasetName;
    private int sourceRowNo;
    private String fromExternalId;
    private String toExternalId;
    private String edgeType;
    private BigDecimal amount;
    private String currency;
    private Timestamp eventTime;
    private String reasonJson;

    public long getCaseId() {
        return caseId;
    }

    public void setCaseId(long caseId) {
        this.caseId = caseId;
    }

    public long getGraphEdgeId() {
        return graphEdgeId;
    }

    public void setGraphEdgeId(long graphEdgeId) {
        this.graphEdgeId = graphEdgeId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getFeatureVersion() {
        return featureVersion;
    }

    public void setFeatureVersion(String featureVersion) {
        this.featureVersion = featureVersion;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getModelDecision() {
        return modelDecision;
    }

    public void setModelDecision(String modelDecision) {
        this.modelDecision = modelDecision;
    }

    public String getBusinessDecision() {
        return businessDecision;
    }

    public void setBusinessDecision(String businessDecision) {
        this.businessDecision = businessDecision;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public BigDecimal getRiskProbability() {
        return riskProbability;
    }

    public void setRiskProbability(BigDecimal riskProbability) {
        this.riskProbability = riskProbability;
    }

    public boolean isLabelFraud() {
        return labelFraud;
    }

    public void setLabelFraud(boolean labelFraud) {
        this.labelFraud = labelFraud;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Long getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    public void setReviewedByAdminId(Long reviewedByAdminId) {
        this.reviewedByAdminId = reviewedByAdminId;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public int getSourceRowNo() {
        return sourceRowNo;
    }

    public void setSourceRowNo(int sourceRowNo) {
        this.sourceRowNo = sourceRowNo;
    }

    public String getFromExternalId() {
        return fromExternalId;
    }

    public void setFromExternalId(String fromExternalId) {
        this.fromExternalId = fromExternalId;
    }

    public String getToExternalId() {
        return toExternalId;
    }

    public void setToExternalId(String toExternalId) {
        this.toExternalId = toExternalId;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Timestamp getEventTime() {
        return eventTime;
    }

    public void setEventTime(Timestamp eventTime) {
        this.eventTime = eventTime;
    }

    public String getReasonJson() {
        return reasonJson;
    }

    public void setReasonJson(String reasonJson) {
        this.reasonJson = reasonJson;
    }
}
