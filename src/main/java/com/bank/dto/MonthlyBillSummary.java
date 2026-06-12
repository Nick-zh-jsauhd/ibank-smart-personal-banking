package com.bank.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MonthlyBillSummary {
    private String yearMonth;
    private BigDecimal totalIncome = BigDecimal.ZERO.setScale(2);
    private BigDecimal totalExpense = BigDecimal.ZERO.setScale(2);
    private BigDecimal netIncome = BigDecimal.ZERO.setScale(2);
    private int incomeCount;
    private int expenseCount;
    private List<CategorySummary> categories = new ArrayList<CategorySummary>();
    private List<LedgerEntryView> entries = new ArrayList<LedgerEntryView>();

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
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

    public List<CategorySummary> getCategories() {
        return categories;
    }

    public void setCategories(List<CategorySummary> categories) {
        this.categories = categories;
    }

    public List<LedgerEntryView> getEntries() {
        return entries;
    }

    public void setEntries(List<LedgerEntryView> entries) {
        this.entries = entries;
    }
}
