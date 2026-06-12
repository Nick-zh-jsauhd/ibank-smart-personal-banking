package com.bank.dto;

import java.math.BigDecimal;

public class CustomerInsightSummary {
    private BigDecimal monthlyIncome = BigDecimal.ZERO.setScale(2);
    private BigDecimal monthlyExpense = BigDecimal.ZERO.setScale(2);
    private BigDecimal monthlyNetIncome = BigDecimal.ZERO.setScale(2);
    private int incomeCount;
    private int expenseCount;
    private CategorySummary topExpenseCategory;
    private LedgerEntryView latestLedger;
    private LedgerEntryView largestOutflow;

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getMonthlyExpense() {
        return monthlyExpense;
    }

    public void setMonthlyExpense(BigDecimal monthlyExpense) {
        this.monthlyExpense = monthlyExpense;
    }

    public BigDecimal getMonthlyNetIncome() {
        return monthlyNetIncome;
    }

    public void setMonthlyNetIncome(BigDecimal monthlyNetIncome) {
        this.monthlyNetIncome = monthlyNetIncome;
    }

    public int getIncomeCount() {
        return incomeCount;
    }

    public void setIncomeCount(int incomeCount) {
        this.incomeCount = incomeCount;
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(int expenseCount) {
        this.expenseCount = expenseCount;
    }

    public CategorySummary getTopExpenseCategory() {
        return topExpenseCategory;
    }

    public void setTopExpenseCategory(CategorySummary topExpenseCategory) {
        this.topExpenseCategory = topExpenseCategory;
    }

    public LedgerEntryView getLatestLedger() {
        return latestLedger;
    }

    public void setLatestLedger(LedgerEntryView latestLedger) {
        this.latestLedger = latestLedger;
    }

    public LedgerEntryView getLargestOutflow() {
        return largestOutflow;
    }

    public void setLargestOutflow(LedgerEntryView largestOutflow) {
        this.largestOutflow = largestOutflow;
    }
}
