package com.bank.dto;

public class AdminRiskGraphCapacityMetric {
    private String modelVersion;
    private int capacity;
    private int selectedCount;
    private int thresholdScore;
    private long truePositiveCount;
    private long falsePositiveCount;
    private long totalPositiveCount;

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }

    public int getThresholdScore() {
        return thresholdScore;
    }

    public void setThresholdScore(int thresholdScore) {
        this.thresholdScore = thresholdScore;
    }

    public long getTruePositiveCount() {
        return truePositiveCount;
    }

    public void setTruePositiveCount(long truePositiveCount) {
        this.truePositiveCount = truePositiveCount;
    }

    public long getFalsePositiveCount() {
        return falsePositiveCount;
    }

    public void setFalsePositiveCount(long falsePositiveCount) {
        this.falsePositiveCount = falsePositiveCount;
    }

    public long getTotalPositiveCount() {
        return totalPositiveCount;
    }

    public void setTotalPositiveCount(long totalPositiveCount) {
        this.totalPositiveCount = totalPositiveCount;
    }
}
