package com.bank.dto;

import java.math.BigDecimal;

public class AdminDashboardMetrics {
    private int customerCount;
    private int accountCount;
    private int todayTransactionCount;
    private BigDecimal todayTransactionAmount;
    private int todayRiskBlockCount;
    private int unreadNotificationCount;
    private int openAdminAlertCount;
    private int openServiceTicketCount;
    private int openAdjustmentReviewCount;
    private int openReconciliationItemCount;
    private int openRiskGraphReviewCaseCount;
    private BigDecimal wealthHoldingPrincipal;

    public int getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(int customerCount) {
        this.customerCount = customerCount;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public int getTodayTransactionCount() {
        return todayTransactionCount;
    }

    public void setTodayTransactionCount(int todayTransactionCount) {
        this.todayTransactionCount = todayTransactionCount;
    }

    public BigDecimal getTodayTransactionAmount() {
        return todayTransactionAmount;
    }

    public void setTodayTransactionAmount(BigDecimal todayTransactionAmount) {
        this.todayTransactionAmount = todayTransactionAmount;
    }

    public int getTodayRiskBlockCount() {
        return todayRiskBlockCount;
    }

    public void setTodayRiskBlockCount(int todayRiskBlockCount) {
        this.todayRiskBlockCount = todayRiskBlockCount;
    }

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(int unreadNotificationCount) {
        this.unreadNotificationCount = unreadNotificationCount;
    }

    public int getOpenAdminAlertCount() {
        return openAdminAlertCount;
    }

    public void setOpenAdminAlertCount(int openAdminAlertCount) {
        this.openAdminAlertCount = openAdminAlertCount;
    }

    public int getOpenServiceTicketCount() {
        return openServiceTicketCount;
    }

    public void setOpenServiceTicketCount(int openServiceTicketCount) {
        this.openServiceTicketCount = openServiceTicketCount;
    }

    public int getOpenAdjustmentReviewCount() {
        return openAdjustmentReviewCount;
    }

    public void setOpenAdjustmentReviewCount(int openAdjustmentReviewCount) {
        this.openAdjustmentReviewCount = openAdjustmentReviewCount;
    }

    public int getOpenReconciliationItemCount() {
        return openReconciliationItemCount;
    }

    public void setOpenReconciliationItemCount(int openReconciliationItemCount) {
        this.openReconciliationItemCount = openReconciliationItemCount;
    }

    public int getOpenRiskGraphReviewCaseCount() {
        return openRiskGraphReviewCaseCount;
    }

    public void setOpenRiskGraphReviewCaseCount(int openRiskGraphReviewCaseCount) {
        this.openRiskGraphReviewCaseCount = openRiskGraphReviewCaseCount;
    }

    public BigDecimal getWealthHoldingPrincipal() {
        return wealthHoldingPrincipal;
    }

    public void setWealthHoldingPrincipal(BigDecimal wealthHoldingPrincipal) {
        this.wealthHoldingPrincipal = wealthHoldingPrincipal;
    }
}
