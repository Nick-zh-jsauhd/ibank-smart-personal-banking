package com.bank.dto;

import java.math.BigDecimal;

public class TimeBucketSummary {
    private String bucketKey;
    private String bucketLabel;
    private BigDecimal totalIncome = BigDecimal.ZERO.setScale(2);
    private BigDecimal totalExpense = BigDecimal.ZERO.setScale(2);
    private BigDecimal netIncome = BigDecimal.ZERO.setScale(2);
    private int entryCount;

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String bucketKey) {
        this.bucketKey = bucketKey;
    }

    public String getBucketLabel() {
        return bucketLabel;
    }

    public void setBucketLabel(String bucketLabel) {
        this.bucketLabel = bucketLabel;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }
}
