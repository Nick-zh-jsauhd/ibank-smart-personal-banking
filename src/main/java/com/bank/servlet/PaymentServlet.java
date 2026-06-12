package com.bank.servlet;

import com.bank.bean.Account;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.dto.TransactionResult;
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

@WebServlet(name = "PaymentServlet", urlPatterns = "/payment")
public class PaymentServlet extends HttpServlet {
    private final AccountService accountService = new AccountServiceImpl();
    private final TransactionService transactionService = new TransactionServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadAccounts(request);
        request.getRequestDispatcher("/transaction/payment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        Long accountId = parseAccountId(RequestUtil.trim(request, "accountId"));
        String paymentType = RequestUtil.trim(request, "paymentType");
        String payerNo = RequestUtil.trim(request, "payerNo");
        String billingMonth = RequestUtil.trim(request, "billingMonth");
        String amount = RequestUtil.trim(request, "amount");
        String payPassword = RequestUtil.trim(request, "payPassword");
        String remark = RequestUtil.trim(request, "remark");

        if (accountId == null) {
            request.setAttribute("error", "请选择付款账户。");
            restoreForm(request, accountId, paymentType, payerNo, billingMonth, amount, remark);
            loadAccounts(request);
            request.getRequestDispatcher("/transaction/payment.jsp").forward(request, response);
            return;
        }

        ServiceResult<TransactionResult> result = transactionService.payBill(
                sessionUser.getCustomerId(),
                sessionUser.getUserId(),
                accountId,
                paymentType,
                payerNo,
                billingMonth,
                amount,
                payPassword,
                remark,
                RequestUtil.clientIp(request)
        );
        if (result.isSuccess()) {
            request.setAttribute("result", result.getData());
            request.getRequestDispatcher("/transaction/transactionResult.jsp").forward(request, response);
            return;
        }

        request.setAttribute("error", result.getMessage());
        restoreForm(request, accountId, paymentType, payerNo, billingMonth, amount, remark);
        loadAccounts(request);
        request.getRequestDispatcher("/transaction/payment.jsp").forward(request, response);
    }

    private void restoreForm(HttpServletRequest request, Long accountId, String paymentType, String payerNo,
                             String billingMonth, String amount, String remark) {
        request.setAttribute("selectedAccountId", accountId);
        request.setAttribute("selectedPaymentType", paymentType);
        request.setAttribute("payerNo", payerNo);
        request.setAttribute("billingMonth", billingMonth);
        request.setAttribute("amount", amount);
        request.setAttribute("remark", remark);
    }

    private void loadAccounts(HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<List<Account>> accountResult = accountService.listAccounts(sessionUser.getCustomerId());
        if (accountResult.isSuccess()) {
            request.setAttribute("accounts", accountResult.getData());
        } else {
            request.setAttribute("accounts", Collections.emptyList());
            request.setAttribute("error", accountResult.getMessage());
        }
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
