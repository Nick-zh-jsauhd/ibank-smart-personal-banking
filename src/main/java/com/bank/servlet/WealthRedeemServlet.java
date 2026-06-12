package com.bank.servlet;

import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.dto.TransactionResult;
import com.bank.dto.WealthHoldingView;
import com.bank.service.WealthService;
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

@WebServlet(name = "WealthRedeemServlet", urlPatterns = "/wealth/redeem")
public class WealthRedeemServlet extends HttpServlet {
    private final WealthService wealthService = new WealthServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/wealth/holdings");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        Long holdingId = parseLong(RequestUtil.trim(request, "holdingId"));
        String payPassword = RequestUtil.trim(request, "payPassword");

        if (holdingId == null) {
            forwardHoldings(request, response, "请选择要赎回的持仓。");
            return;
        }

        ServiceResult<TransactionResult> result = wealthService.redeemHolding(
                sessionUser.getCustomerId(),
                sessionUser.getUserId(),
                holdingId,
                payPassword,
                RequestUtil.clientIp(request)
        );
        if (result.isSuccess()) {
            request.setAttribute("result", result.getData());
            request.getRequestDispatcher("/transaction/transactionResult.jsp").forward(request, response);
            return;
        }

        forwardHoldings(request, response, result.getMessage());
    }

    private void forwardHoldings(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<List<WealthHoldingView>> result = wealthService.listHoldings(sessionUser.getCustomerId());
        request.setAttribute("holdings", result.isSuccess() ? result.getData() : Collections.emptyList());
        if (!result.isSuccess() && request.getAttribute("error") == null) {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/wealth/holdingList.jsp").forward(request, response);
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
