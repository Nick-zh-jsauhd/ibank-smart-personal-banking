package com.bank.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AdminRiskGraphScoreView {
    private long graphEdgeId;
    private String batchCode;
    private String datasetName;
    private int sourceRowNo;
    private long fromNodeId;
    private long toNodeId;
    private String fromExternalId;
    private String toExternalId;
    private String edgeType;
    private BigDecimal amount;
    private String currency;
    private Timestamp eventTime;
    private boolean labelFraud;
    private String modelVersion;
    private String featureVersion;
    private int riskScore;
    private BigDecimal riskProbability;
    private String decision;
    private BigDecimal reviewThreshold;
    private BigDecimal blockThreshold;
    private String reasonJson;
    private Timestamp scoredAt;
    private int neighborhoodBlockCount;
    private int neighborhoodReviewCount;
    private int neighborhoodFraudCount;
    private String businessDecision;
    private String businessDecisionLabel;
    private String businessDecisionReason;
    private String businessDecisionSeverity;
    private boolean labelConflict;

    public long getGraphEdgeId() {
        return graphEdgeId;
    }

    public void setGraphEdgeId(long graphEdgeId) {
        this.graphEdgeId = graphEdgeId;
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

    public long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public long getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(long toNodeId) {
        this.toNodeId = toNodeId;
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

    public boolean isLabelFraud() {
        return labelFraud;
    }

    public void setLabelFraud(boolean labelFraud) {
        this.labelFraud = labelFraud;
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

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public BigDecimal getReviewThreshold() {
        return reviewThreshold;
    }

    public void setReviewThreshold(BigDecimal reviewThreshold) {
        this.reviewThreshold = reviewThreshold;
    }

    public BigDecimal getBlockThreshold() {
        return blockThreshold;
    }

    public void setBlockThreshold(BigDecimal blockThreshold) {
        this.blockThreshold = blockThreshold;
    }

    public String getReasonJson() {
        return reasonJson;
    }

    public void setReasonJson(String reasonJson) {
        this.reasonJson = reasonJson;
    }

    public Timestamp getScoredAt() {
        return scoredAt;
    }

    public void setScoredAt(Timestamp scoredAt) {
        this.scoredAt = scoredAt;
    }

    public int getNeighborhoodBlockCount() {
        return neighborhoodBlockCount;
    }

    public void setNeighborhoodBlockCount(int neighborhoodBlockCount) {
        this.neighborhoodBlockCount = neighborhoodBlockCount;
    }

    public int getNeighborhoodReviewCount() {
        return neighborhoodReviewCount;
    }

    public void setNeighborhoodReviewCount(int neighborhoodReviewCount) {
        this.neighborhoodReviewCount = neighborhoodReviewCount;
    }

    public int getNeighborhoodFraudCount() {
        return neighborhoodFraudCount;
    }

    public void setNeighborhoodFraudCount(int neighborhoodFraudCount) {
        this.neighborhoodFraudCount = neighborhoodFraudCount;
    }

    public String getBusinessDecision() {
        return businessDecision;
    }

    public void setBusinessDecision(String businessDecision) {
        this.businessDecision = businessDecision;
    }

    public String getBusinessDecisionLabel() {
        return businessDecisionLabel;
    }

    public void setBusinessDecisionLabel(String businessDecisionLabel) {
        this.businessDecisionLabel = businessDecisionLabel;
    }

    public String getBusinessDecisionReason() {
        return businessDecisionReason;
    }

    public void setBusinessDecisionReason(String businessDecisionReason) {
        this.businessDecisionReason = businessDecisionReason;
    }

    public String getBusinessDecisionSeverity() {
        return businessDecisionSeverity;
    }

    public void setBusinessDecisionSeverity(String businessDecisionSeverity) {
        this.businessDecisionSeverity = businessDecisionSeverity;
    }

    public boolean isLabelConflict() {
        return labelConflict;
    }

    public void setLabelConflict(boolean labelConflict) {
        this.labelConflict = labelConflict;
    }
}
