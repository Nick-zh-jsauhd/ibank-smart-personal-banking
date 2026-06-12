package com.bank.dto;

import com.bank.bean.Account;
import com.bank.bean.ServiceTicket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardView {
    private List<Account> accounts = new ArrayList<Account>();
    private BigDecimal totalAvailableBalance = BigDecimal.ZERO.setScale(2);
    private String customerRiskLevel = "C2";
    private String riskLevelSource = "SYSTEM";
    private int unreadNotificationCount;
    private MonthlyBillSummary monthlyBill = new MonthlyBillSummary();
    private CustomerInsightSummary insight = new CustomerInsightSummary();
    private List<LedgerEntryView> recentLedgers = new ArrayList<LedgerEntryView>();
    private List<ServiceTicket> recentTickets = new ArrayList<ServiceTicket>();
    private List<RiskEventView> recentRiskEvents = new ArrayList<RiskEventView>();
    private int activeTicketCount;
    private int waitingTicketCount;
    private int resolvedTicketCount;
    private String loadWarning;

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public BigDecimal getTotalAvailableBalance() {
        return totalAvailableBalance;
    }

    public void setTotalAvailableBalance(BigDecimal totalAvailableBalance) {
        this.totalAvailableBalance = totalAvailableBalance;
    }

    public String getCustomerRiskLevel() {
        return customerRiskLevel;
    }

    public void setCustomerRiskLevel(String customerRiskLevel) {
        this.customerRiskLevel = customerRiskLevel;
    }

    public String getRiskLevelSource() {
        return riskLevelSource;
    }

    public void setRiskLevelSource(String riskLevelSource) {
        this.riskLevelSource = riskLevelSource;
    }

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(int unreadNotificationCount) {
        this.unreadNotificationCount = unreadNotificationCount;
    }

    public MonthlyBillSummary getMonthlyBill() {
        return monthlyBill;
    }

    public void setMonthlyBill(MonthlyBillSummary monthlyBill) {
        this.monthlyBill = monthlyBill;
    }

    public CustomerInsightSummary getInsight() {
        return insight;
    }

    public void setInsight(CustomerInsightSummary insight) {
        this.insight = insight;
    }

    public List<LedgerEntryView> getRecentLedgers() {
        return recentLedgers;
    }

    public void setRecentLedgers(List<LedgerEntryView> recentLedgers) {
        this.recentLedgers = recentLedgers;
    }

    public List<ServiceTicket> getRecentTickets() {
        return recentTickets;
    }

    public void setRecentTickets(List<ServiceTicket> recentTickets) {
        this.recentTickets = recentTickets;
    }

    public List<RiskEventView> getRecentRiskEvents() {
        return recentRiskEvents;
    }

    public void setRecentRiskEvents(List<RiskEventView> recentRiskEvents) {
        this.recentRiskEvents = recentRiskEvents;
    }

    public int getActiveTicketCount() {
        return activeTicketCount;
    }

    public void setActiveTicketCount(int activeTicketCount) {
        this.activeTicketCount = activeTicketCount;
    }

    public int getWaitingTicketCount() {
        return waitingTicketCount;
    }

    public void setWaitingTicketCount(int waitingTicketCount) {
        this.waitingTicketCount = waitingTicketCount;
    }

    public int getResolvedTicketCount() {
        return resolvedTicketCount;
    }

    public void setResolvedTicketCount(int resolvedTicketCount) {
        this.resolvedTicketCount = resolvedTicketCount;
    }

    public String getLoadWarning() {
        return loadWarning;
    }

    public void setLoadWarning(String loadWarning) {
        this.loadWarning = loadWarning;
    }
}
