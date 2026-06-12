package com.bank.dto;

import java.math.BigDecimal;

public class AdminRiskGraphModelVersionMetric {
    private String modelVersion;
    private String featureVersion;
    private long totalScores;
    private long positiveLabels;
    private long passCount;
    private long reviewCount;
    private long blockCount;
    private long passFraudCount;
    private long reviewFraudCount;
    private long blockFraudCount;
    private BigDecimal averageScore;
    private BigDecimal averageProbability;
    private BigDecimal reviewThreshold;
    private BigDecimal blockThreshold;

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

    public long getTotalScores() {
        return totalScores;
    }

    public void setTotalScores(long totalScores) {
        this.totalScores = totalScores;
    }

    public long getPositiveLabels() {
        return positiveLabels;
    }

    public void setPositiveLabels(long positiveLabels) {
        this.positiveLabels = positiveLabels;
    }

    public long getPassCount() {
        return passCount;
    }

    public void setPassCount(long passCount) {
        this.passCount = passCount;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public long getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(long blockCount) {
        this.blockCount = blockCount;
    }

    public long getPassFraudCount() {
        return passFraudCount;
    }

    public void setPassFraudCount(long passFraudCount) {
        this.passFraudCount = passFraudCount;
    }

    public long getReviewFraudCount() {
        return reviewFraudCount;
    }

    public void setReviewFraudCount(long reviewFraudCount) {
        this.reviewFraudCount = reviewFraudCount;
    }

    public long getBlockFraudCount() {
        return blockFraudCount;
    }

    public void setBlockFraudCount(long blockFraudCount) {
        this.blockFraudCount = blockFraudCount;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public BigDecimal getAverageProbability() {
        return averageProbability;
    }

    public void setAverageProbability(BigDecimal averageProbability) {
        this.averageProbability = averageProbability;
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

    public long getFlaggedCount() {
        return reviewCount + blockCount;
    }

    public long getFlaggedFraudCount() {
        return reviewFraudCount + blockFraudCount;
    }

    public long getPassNormalCount() {
        return passCount - passFraudCount;
    }
}
