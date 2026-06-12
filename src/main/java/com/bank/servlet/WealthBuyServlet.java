package com.bank.servlet;

import com.bank.bean.Account;
import com.bank.bean.WealthProduct;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
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

@WebServlet(name = "WealthBuyServlet", urlPatterns = {"/wealth/buy", "/wealth/confirm"})
public class WealthBuyServlet extends HttpServlet {
    private final WealthService wealthService = new WealthServiceImpl();
    private final AccountService accountService = new AccountServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long productId = parseLong(RequestUtil.trim(request, "productId"));
        loadBuyContext(request, productId);
        request.getRequestDispatcher("/wealth/buy.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        Long productId = parseLong(RequestUtil.trim(request, "productId"));
        Long accountId = parseLong(RequestUtil.trim(request, "accountId"));
        String amount = RequestUtil.trim(request, "amount");

        if (productId == null || accountId == null) {
            request.setAttribute("error", productId == null ? "请选择理财产品。" : "请选择付款账户。");
            restoreForm(request, accountId, amount);
            loadBuyContext(request, productId);
            request.getRequestDispatcher("/wealth/buy.jsp").forward(request, response);
            return;
        }

        ServiceResult<WealthPurchasePreview> result = wealthService.previewPurchase(
                sessionUser.getCustomerId(), productId, accountId, amount);
        if (result.isSuccess()) {
            request.setAttribute("preview", result.getData());
            request.getRequestDispatcher("/wealth/confirm.jsp").forward(request, response);
            return;
        }

        request.setAttribute("error", result.getMessage());
        restoreForm(request, accountId, amount);
        loadBuyContext(request, productId);
        request.getRequestDispatcher("/wealth/buy.jsp").forward(request, response);
    }

    private void restoreForm(HttpServletRequest request, Long accountId, String amount) {
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
