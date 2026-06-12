package com.bank.service.impl;

import com.bank.bean.Account;
import com.bank.bean.Customer;
import com.bank.bean.ServiceTicket;
import com.bank.dao.CustomerDao;
import com.bank.dao.LedgerDao;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dao.impl.LedgerDaoImpl;
import com.bank.dto.CategorySummary;
import com.bank.dto.CustomerDashboardView;
import com.bank.dto.CustomerInsightSummary;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.MonthlyBillSummary;
import com.bank.dto.RiskEventView;
import com.bank.dto.ServiceResult;
import com.bank.service.AccountService;
import com.bank.service.BillService;
import com.bank.service.CustomerDashboardService;
import com.bank.service.NotificationService;
import com.bank.service.RiskService;
import com.bank.service.TicketService;
import com.bank.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerDashboardServiceImpl implements CustomerDashboardService {
    private static final int RECENT_LEDGER_LIMIT = 8;
    private static final int RECENT_RISK_LIMIT = 3;
    private static final int RECENT_TICKET_LIMIT = 3;

    private final AccountService accountService = new AccountServiceImpl();
    private final BillService billService = new BillServiceImpl();
    private final NotificationService notificationService = new NotificationServiceImpl();
    private final TicketService ticketService = new TicketServiceImpl();
    private final RiskService riskService = new RiskServiceImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();
    private final LedgerDao ledgerDao = new LedgerDaoImpl();

    @Override
    public ServiceResult<CustomerDashboardView> loadDashboard(long customerId) {
        CustomerDashboardView view = new CustomerDashboardView();
        List<String> warnings = new ArrayList<String>();

        loadAccounts(customerId, view, warnings);
        loadCustomer(customerId, view, warnings);
        loadMonthlyBill(customerId, view, warnings);
        loadRecentLedgers(customerId, view, warnings);
        loadNotifications(customerId, view, warnings);
        loadTickets(customerId, view, warnings);
        loadRiskEvents(customerId, view, warnings);
        view.setInsight(buildInsight(view.getMonthlyBill(), view.getRecentLedgers()));

        if (!warnings.isEmpty()) {
            view.setLoadWarning(joinWarnings(warnings));
        }
        return ServiceResult.success("客户工作台加载成功。", view);
    }

    private void loadAccounts(long customerId, CustomerDashboardView view, List<String> warnings) {
        ServiceResult<List<Account>> result = accountService.listAccounts(customerId);
        if (!result.isSuccess()) {
            warnings.add(result.getMessage());
            view.setAccounts(Collections.<Account>emptyList());
            view.setTotalAvailableBalance(BigDecimal.ZERO.setScale(2));
            return;
        }
        List<Account> accounts = result.getData() == null
                ? Collections.<Account>emptyList()
                : result.getData();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Account account : accounts) {
            if (account.getAvailableBalance() != null) {
                total = total.add(account.getAvailableBalance());
            }
        }
        view.setAccounts(accounts);
        view.setTotalAvailableBalance(total);
    }

    private void loadCustomer(long customerId, CustomerDashboardView view, List<String> warnings) {
        try (Connection connection = DBUtil.getConnection()) {
            Customer customer = customerDao.findById(connection, customerId);
            if (customer != null) {
                view.setCustomerRiskLevel(customer.getRiskLevel() == null ? "C2" : customer.getRiskLevel());
                view.setRiskLevelSource(customer.getRiskLevelSource() == null ? "SYSTEM" : customer.getRiskLevelSource());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            warnings.add("客户风险等级读取失败。");
        }
    }

    private void loadMonthlyBill(long customerId, CustomerDashboardView view, List<String> warnings) {
        ServiceResult<MonthlyBillSummary> result = billService.getMonthlyBill(customerId, null, null);
        if (result.isSuccess() && result.getData() != null) {
            view.setMonthlyBill(result.getData());
        } else if (!result.isSuccess()) {
            warnings.add(result.getMessage());
        }
    }

    private void loadRecentLedgers(long customerId, CustomerDashboardView view, List<String> warnings) {
        try (Connection connection = DBUtil.getConnection()) {
            view.setRecentLedgers(ledgerDao.findByCustomer(connection, customerId, null, null, null,
                    RECENT_LEDGER_LIMIT));
        } catch (SQLException e) {
            e.printStackTrace();
            warnings.add("近期流水读取失败。");
        }
    }

    private void loadNotifications(long customerId, CustomerDashboardView view, List<String> warnings) {
        ServiceResult<Integer> result = notificationService.countUnread(customerId);
        if (result.isSuccess() && result.getData() != null) {
            view.setUnreadNotificationCount(result.getData());
        } else if (!result.isSuccess()) {
            warnings.add(result.getMessage());
        }
    }

    private void loadTickets(long customerId, CustomerDashboardView view, List<String> warnings) {
        ServiceResult<List<ServiceTicket>> result = ticketService.listCustomerTickets(customerId);
        if (!result.isSuccess()) {
            warnings.add(result.getMessage());
            return;
        }
        List<ServiceTicket> tickets = result.getData() == null
                ? Collections.<ServiceTicket>emptyList()
                : result.getData();
        int active = 0;
        int waiting = 0;
        int resolved = 0;
        List<ServiceTicket> recent = new ArrayList<ServiceTicket>();
        for (ServiceTicket ticket : tickets) {
            if ("WAITING_CUSTOMER".equals(ticket.getStatus())) {
                waiting++;
            } else if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
                resolved++;
            } else {
                active++;
            }
            if (recent.size() < RECENT_TICKET_LIMIT) {
                recent.add(ticket);
            }
        }
        view.setActiveTicketCount(active);
        view.setWaitingTicketCount(waiting);
        view.setResolvedTicketCount(resolved);
        view.setRecentTickets(recent);
    }

    private void loadRiskEvents(long customerId, CustomerDashboardView view, List<String> warnings) {
        ServiceResult<List<RiskEventView>> result = riskService.listEvents(customerId, null);
        if (!result.isSuccess()) {
            warnings.add(result.getMessage());
            return;
        }
        List<RiskEventView> events = result.getData() == null
                ? Collections.<RiskEventView>emptyList()
                : result.getData();
        List<RiskEventView> recent = new ArrayList<RiskEventView>();
        for (RiskEventView event : events) {
            if (recent.size() >= RECENT_RISK_LIMIT) {
                break;
            }
            recent.add(event);
        }
        view.setRecentRiskEvents(recent);
    }

    private CustomerInsightSummary buildInsight(MonthlyBillSummary bill, List<LedgerEntryView> recentLedgers) {
        CustomerInsightSummary insight = new CustomerInsightSummary();
        if (bill != null) {
            insight.setMonthlyIncome(nonNull(bill.getTotalIncome()));
            insight.setMonthlyExpense(nonNull(bill.getTotalExpense()));
            insight.setMonthlyNetIncome(nonNull(bill.getNetIncome()));
            insight.setIncomeCount(bill.getIncomeCount());
            insight.setExpenseCount(bill.getExpenseCount());
            insight.setTopExpenseCategory(findTopExpenseCategory(bill.getCategories()));
            insight.setLargestOutflow(findLargestOutflow(bill.getEntries()));
        }
        if (recentLedgers != null && !recentLedgers.isEmpty()) {
            insight.setLatestLedger(recentLedgers.get(0));
        }
        return insight;
    }

    private CategorySummary findTopExpenseCategory(List<CategorySummary> categories) {
        CategorySummary top = null;
        if (categories == null) {
            return null;
        }
        for (CategorySummary category : categories) {
            if (!"OUT".equals(category.getDirection())) {
                continue;
            }
            if (top == null || nonNull(category.getTotalAmount()).compareTo(nonNull(top.getTotalAmount())) > 0) {
                top = category;
            }
        }
        return top;
    }

    private LedgerEntryView findLargestOutflow(List<LedgerEntryView> entries) {
        LedgerEntryView largest = null;
        if (entries == null) {
            return null;
        }
        for (LedgerEntryView entry : entries) {
            if (!"OUT".equals(entry.getDirection())) {
                continue;
            }
            if (largest == null || nonNull(entry.getAmount()).compareTo(nonNull(largest.getAmount())) > 0) {
                largest = entry;
            }
        }
        return largest;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private String joinWarnings(List<String> warnings) {
        StringBuilder builder = new StringBuilder();
        for (String warning : warnings) {
            if (warning == null || warning.trim().length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append(warning.trim());
        }
        return builder.toString();
    }
}
