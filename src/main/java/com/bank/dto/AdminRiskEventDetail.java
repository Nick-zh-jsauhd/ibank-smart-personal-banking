package com.bank.dto;

import com.bank.bean.Account;
import com.bank.bean.Customer;
import com.bank.bean.RiskActionLog;

import java.util.List;

public class AdminRiskEventDetail {
    private AdminRiskEventView event;
    private Customer customer;
    private Account account;
    private List<LedgerEntryView> recentLedgers;
    private List<RiskActionLog> actionLogs;

    public AdminRiskEventView getEvent() {
        return event;
    }

    public void setEvent(AdminRiskEventView event) {
        this.event = event;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<LedgerEntryView> getRecentLedgers() {
        return recentLedgers;
    }

    public void setRecentLedgers(List<LedgerEntryView> recentLedgers) {
        this.recentLedgers = recentLedgers;
    }

    public List<RiskActionLog> getActionLogs() {
        return actionLogs;
    }

    public void setActionLogs(List<RiskActionLog> actionLogs) {
        this.actionLogs = actionLogs;
    }
}
