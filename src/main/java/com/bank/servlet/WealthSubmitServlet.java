package com.bank.servlet;

import com.bank.bean.Account;
import com.bank.bean.WealthProduct;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.dto.TransactionResult;
import com.bank.dto.WealthPurchasePreview;
import com.bank.service.AccountService;
import com.bank.service.WealthService;
import com.bank.service.impl.AccountServiceImpl;
import com.bank.service.impl.WealthServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "WealthSubmitServlet", urlPatterns = "/wealth/submit")
public class WealthSubmitServlet extends HttpServlet {
    private final WealthService wealthService = new WealthServiceImpl();
    private final AccountService accountService = new AccountServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        Long productId = parseLong(RequestUtil.trim(request, "productId"));
        Long accountId = parseLong(RequestUtil.trim(request, "accountId"));
        String amount = RequestUtil.trim(request, "amount");
        String payPassword = RequestUtil.trim(request, "payPassword");

        if (productId == null || accountId == null) {
            request.setAttribute("error", productId == null ? "请选择理财产品。" : "请选择付款账户。");
            restoreBuyForm(request, accountId, amount);
            loadBuyContext(request, productId);
            request.getRequestDispatcher("/wealth/buy.jsp").forward(request, response);
            return;
        }

        ServiceResult<TransactionResult> result = wealthService.confirmBuyProduct(
                sessionUser.getCustomerId(),
                sessionUser.getUserId(),
                productId,
                accountId,
                amount,
                payPassword,
                checked(request, "productDisclosure"),
                checked(request, "nonDepositDisclosure"),
                checked(request, "yieldDisclosure"),
                checked(request, "accountConfirm"),
                RequestUtil.clientIp(request));
        if (result.isSuccess()) {
            request.setAttribute("result", result.getData());
            request.getRequestDispatcher("/transaction/transactionResult.jsp").forward(request, response);
            return;
        }

        request.setAttribute("error", result.getMessage());
        ServiceResult<WealthPurchasePreview> previewResult = wealthService.previewPurchase(
                sessionUser.getCustomerId(), productId, accountId, amount);
        if (previewResult.isSuccess()) {
            request.setAttribute("preview", previewResult.getData());
            request.getRequestDispatcher("/wealth/confirm.jsp").forward(request, response);
            return;
        }

        restoreBuyForm(request, accountId, amount);
        loadBuyContext(request, productId);
        request.getRequestDispatcher("/wealth/buy.jsp").forward(request, response);
    }

    private boolean checked(HttpServletRequest request, String name) {
        return request.getParameter(name) != null;
    }

    private void restoreBuyForm(HttpServletRequest request, Long accountId, String amount) {
        request.setAttribute("selectedAccountId", accountId);
        request.setAttribute("amount", amount);
    }

    private void loadBuyContext(HttpServletRequest request, Long productId) {
        if (productId == null) {
            setErrorIfAbsent(request, "理财产品参数无效。");
        } else {
            ServiceResult<WealthProduct> productResult = wealthService.getProduct(productId);
            if (productResult.isSuccess()) {
                request.setAttribute("product", productResult.getData());
            } else {
                setErrorIfAbsent(request, productResult.getMessage());
            }
        }

        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<List<Account>> accountResult = accountService.listAccounts(sessionUser.getCustomerId());
        if (accountResult.isSuccess()) {
            request.setAttribute("accounts", accountResult.getData());
        } else {
            request.setAttribute("accounts", Collections.emptyList());
            setErrorIfAbsent(request, accountResult.getMessage());
        }
    }

    private void setErrorIfAbsent(HttpServletRequest request, String error) {
        if (request.getAttribute("error") == null) {
            request.setAttribute("error", error);
        }
    }

    private Long parseLong(String value) {
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
