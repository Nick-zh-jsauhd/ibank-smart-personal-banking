package com.bank.dto;

public class AdminRiskGraphThresholdPlan {
    private String modelVersion;
    private int reviewCapacity;
    private int blockCapacity;
    private AdminRiskGraphCapacityMetric reviewMetric;
    private AdminRiskGraphCapacityMetric blockMetric;

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
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

    public AdminRiskGraphCapacityMetric getReviewMetric() {
        return reviewMetric;
    }

    public void setReviewMetric(AdminRiskGraphCapacityMetric reviewMetric) {
        this.reviewMetric = reviewMetric;
    }

    public AdminRiskGraphCapacityMetric getBlockMetric() {
        return blockMetric;
    }

    public void setBlockMetric(AdminRiskGraphCapacityMetric blockMetric) {
        this.blockMetric = blockMetric;
    }

    public int getReviewOnlySelectedCount() {
        return Math.max(0, selected(reviewMetric) - selected(blockMetric));
    }

    public long getReviewOnlyTruePositiveCount() {
        return Math.max(0L, truePositive(reviewMetric) - truePositive(blockMetric));
    }

    public long getReviewOnlyFalsePositiveCount() {
        return Math.max(0L, falsePositive(reviewMetric) - falsePositive(blockMetric));
    }

    private int selected(AdminRiskGraphCapacityMetric metric) {
        return metric == null ? 0 : metric.getSelectedCount();
    }

    private long truePositive(AdminRiskGraphCapacityMetric metric) {
        return metric == null ? 0L : metric.getTruePositiveCount();
    }

    private long falsePositive(AdminRiskGraphCapacityMetric metric) {
        return metric == null ? 0L : metric.getFalsePositiveCount();
    }
}
