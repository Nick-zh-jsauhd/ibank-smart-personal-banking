package com.bank.servlet;

import com.bank.bean.Account;
import com.bank.dto.LedgerEntryView;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.AccountService;
import com.bank.service.TransactionService;
import com.bank.service.impl.AccountServiceImpl;
import com.bank.service.impl.TransactionServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "TransactionQueryServlet", urlPatterns = "/transactions")
public class TransactionQueryServlet extends HttpServlet {
    private final AccountService accountService = new AccountServiceImpl();
    private final TransactionService transactionService = new TransactionServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        Long accountId = parseAccountId(RequestUtil.trim(request, "accountId"));
        String direction = RequestUtil.trim(request, "direction");
        String txnType = RequestUtil.trim(request, "txnType");

        ServiceResult<List<Account>> accountResult = accountService.listAccounts(sessionUser.getCustomerId());
        if (accountResult.isSuccess()) {
            request.setAttribute("accounts", accountResult.getData());
        } else {
            request.setAttribute("accounts", Collections.emptyList());
            request.setAttribute("error", accountResult.getMessage());
        }

        ServiceResult<List<LedgerEntryView>> ledgerResult = transactionService.listLedgerEntries(
                sessionUser.getCustomerId(), accountId, direction, txnType);
        if (ledgerResult.isSuccess()) {
            request.setAttribute("entries", ledgerResult.getData());
        } else {
            request.setAttribute("entries", Collections.emptyList());
            request.setAttribute("error", ledgerResult.getMessage());
        }

        request.setAttribute("selectedAccountId", accountId);
        request.setAttribute("selectedDirection", direction);
        request.setAttribute("selectedTxnType", txnType);
        request.getRequestDispatcher("/transaction/transactionList.jsp").forward(request, response);
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
