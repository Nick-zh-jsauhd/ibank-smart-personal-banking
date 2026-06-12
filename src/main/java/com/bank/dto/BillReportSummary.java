package com.bank.dto;

import java.math.BigDecimal;

public class BillReportSummary {
    private BigDecimal totalIncome = BigDecimal.ZERO.setScale(2);
    private BigDecimal totalExpense = BigDecimal.ZERO.setScale(2);
    private BigDecimal netIncome = BigDecimal.ZERO.setScale(2);
    private BigDecimal largestIncome = BigDecimal.ZERO.setScale(2);
    private BigDecimal largestExpense = BigDecimal.ZERO.setScale(2);
    private BigDecimal savingRate = BigDecimal.ZERO.setScale(2);
    private int incomeCount;
    private int expenseCount;
    private int totalCount;

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

    public BigDecimal getLargestIncome() {
        return largestIncome;
    }

    public void setLargestIncome(BigDecimal largestIncome) {
        this.largestIncome = largestIncome;
    }

    public BigDecimal getLargestExpense() {
        return largestExpense;
    }

    public void setLargestExpense(BigDecimal largestExpense) {
        this.largestExpense = largestExpense;
    }

    public BigDecimal getSavingRate() {
        return savingRate;
    }

    public void setSavingRate(BigDecimal savingRate) {
        this.savingRate = savingRate;
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

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
