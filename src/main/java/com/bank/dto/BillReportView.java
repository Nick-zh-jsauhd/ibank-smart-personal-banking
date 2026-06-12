package com.bank.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BillReportView {
    private String periodType;
    private String periodValue;
    private String periodLabel;
    private String accountScope = "全部账户";
    private Timestamp startAt;
    private Timestamp endAt;
    private BillReportSummary summary = new BillReportSummary();
    private List<TimeBucketSummary> timeBuckets = new ArrayList<TimeBucketSummary>();
    private List<CategorySummary> categories = new ArrayList<CategorySummary>();
    private List<LedgerEntryView> topMovements = new ArrayList<LedgerEntryView>();
    private List<LedgerEntryView> entries = new ArrayList<LedgerEntryView>();
    private String insightText;
    private String nextStepText;

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getPeriodValue() {
        return periodValue;
    }

    public void setPeriodValue(String periodValue) {
        this.periodValue = periodValue;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public String getAccountScope() {
        return accountScope;
    }

    public void setAccountScope(String accountScope) {
        this.accountScope = accountScope;
    }

    public Timestamp getStartAt() {
        return startAt;
    }

    public void setStartAt(Timestamp startAt) {
        this.startAt = startAt;
    }

    public Timestamp getEndAt() {
        return endAt;
    }

    public void setEndAt(Timestamp endAt) {
        this.endAt = endAt;
    }

    public BillReportSummary getSummary() {
        return summary;
    }

    public void setSummary(BillReportSummary summary) {
        this.summary = summary;
    }

    public List<TimeBucketSummary> getTimeBuckets() {
        return timeBuckets;
    }

    public void setTimeBuckets(List<TimeBucketSummary> timeBuckets) {
        this.timeBuckets = timeBuckets;
    }

    public List<CategorySummary> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySummary> categories) {
        this.categories = categories;
    }

    public List<LedgerEntryView> getTopMovements() {
        return topMovements;
    }

    public void setTopMovements(List<LedgerEntryView> topMovements) {
        this.topMovements = topMovements;
    }

    public List<LedgerEntryView> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryView> entries) {
        this.entries = entries;
    }

    public String getInsightText() {
        return insightText;
    }

    public void setInsightText(String insightText) {
        this.insightText = insightText;
    }

    public String getNextStepText() {
        return nextStepText;
    }

    public void setNextStepText(String nextStepText) {
        this.nextStepText = nextStepText;
    }
}
