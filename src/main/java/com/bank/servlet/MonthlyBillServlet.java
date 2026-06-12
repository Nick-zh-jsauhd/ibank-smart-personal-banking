package com.bank.servlet;

import com.bank.bean.Account;
import com.bank.dto.BillReportQuery;
import com.bank.dto.BillReportView;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.AccountService;
import com.bank.service.BillReportService;
import com.bank.service.impl.AccountServiceImpl;
import com.bank.service.impl.BillReportServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "MonthlyBillServlet", urlPatterns = {"/bill/monthly", "/bill/report"})
public class MonthlyBillServlet extends HttpServlet {
    private final AccountService accountService = new AccountServiceImpl();
    private final BillReportService billReportService = new BillReportServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        BillReportQuery query = buildQuery(request, sessionUser);

        ServiceResult<List<Account>> accountResult = accountService.listAccounts(sessionUser.getCustomerId());
        if (accountResult.isSuccess()) {
            request.setAttribute("accounts", accountResult.getData());
        } else {
            request.setAttribute("accounts", Collections.emptyList());
            request.setAttribute("error", accountResult.getMessage());
        }

        ServiceResult<BillReportView> reportResult = billReportService.getReport(query);
        if (reportResult.isSuccess()) {
            request.setAttribute("report", reportResult.getData());
        } else {
            request.setAttribute("error", reportResult.getMessage());
        }

        request.setAttribute("query", query);
        request.setAttribute("selectedAccountId", query.getAccountId());
        request.getRequestDispatcher("/bill/monthlyBill.jsp").forward(request, response);
    }

    private BillReportQuery buildQuery(HttpServletRequest request, SessionUser sessionUser) {
        BillReportQuery query = new BillReportQuery();
        query.setCustomerId(sessionUser.getCustomerId());
        query.setAccountId(parseAccountId(RequestUtil.trim(request, "accountId")));
        query.setPeriodType(RequestUtil.trim(request, "periodType"));
        query.setDate(RequestUtil.trim(request, "date"));
        query.setYearMonth(RequestUtil.trim(request, "yearMonth"));
        query.setYear(RequestUtil.trim(request, "year"));
        query.setDirection(RequestUtil.trim(request, "direction"));
        query.setTxnType(RequestUtil.trim(request, "txnType"));
        query.setDetailLimit(500);
        return query;
    }

    private Long parseAccountId(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
