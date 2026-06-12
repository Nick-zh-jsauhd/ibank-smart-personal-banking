package com.bank.dto;

import com.bank.bean.Account;
import com.bank.bean.Customer;
import com.bank.bean.Notification;
import com.bank.bean.RiskAssessment;
import com.bank.bean.User;

import java.util.List;

public class AdminCustomerDetail {
    private Customer customer;
    private User user;
    private List<Account> accounts;
    private List<LedgerEntryView> recentLedgers;
    private List<RiskEventView> riskEvents;
    private List<Notification> notifications;
    private List<RiskAssessment> riskAssessments;
    private List<WealthOrderConfirmView> wealthOrderConfirms;

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public List<LedgerEntryView> getRecentLedgers() {
        return recentLedgers;
    }

    public void setRecentLedgers(List<LedgerEntryView> recentLedgers) {
        this.recentLedgers = recentLedgers;
    }

    public List<RiskEventView> getRiskEvents() {
        return riskEvents;
    }

    public void setRiskEvents(List<RiskEventView> riskEvents) {
        this.riskEvents = riskEvents;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public List<RiskAssessment> getRiskAssessments() {
        return riskAssessments;
    }

    public void setRiskAssessments(List<RiskAssessment> riskAssessments) {
        this.riskAssessments = riskAssessments;
    }

    public List<WealthOrderConfirmView> getWealthOrderConfirms() {
        return wealthOrderConfirms;
    }

    public void setWealthOrderConfirms(List<WealthOrderConfirmView> wealthOrderConfirms) {
        this.wealthOrderConfirms = wealthOrderConfirms;
    }
}
