package com.bank.servlet.admin;

import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.dto.WealthOrderView;
import com.bank.dto.WealthSettlementSummary;
import com.bank.service.WealthService;
import com.bank.service.impl.WealthServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AdminWealthSettlementServlet extends HttpServlet {
    private final WealthService wealthService = new WealthServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadPage(request);
        request.getRequestDispatcher("/admin/wealthSettlement.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        String action = RequestUtil.trim(request, "action");
        ServiceResult<WealthSettlementSummary> result = wealthService.runSettlement(
                adminUser.getUserId(), action, RequestUtil.clientIp(request));
        if (result.isSuccess()) {
            request.setAttribute("success", result.getMessage()
                    + " 申购确认 " + result.getData().getConfirmedBuyOrders()
                    + " 笔，赎回到账 " + result.getData().getSettledRedeemOrders() + " 笔。");
        } else {
            request.setAttribute("error", result.getMessage());
        }
        loadPage(request);
        request.getRequestDispatcher("/admin/wealthSettlement.jsp").forward(request, response);
    }

    private void loadPage(HttpServletRequest request) {
        String status = RequestUtil.trim(request, "status");
        ServiceResult<WealthSettlementSummary> summaryResult = wealthService.settlementSummary();
        if (summaryResult.isSuccess()) {
            request.setAttribute("summary", summaryResult.getData());
        } else {
            request.setAttribute("error", summaryResult.getMessage());
        }

        ServiceResult<List<WealthOrderView>> orderResult = wealthService.listSettlementOrders(status);
        if (orderResult.isSuccess()) {
            request.setAttribute("orders", orderResult.getData());
        } else {
            request.setAttribute("orders", Collections.emptyList());
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", orderResult.getMessage());
            }
        }
        request.setAttribute("selectedStatus", status);
        request.setAttribute("activeNav", "wealth-settlement");
    }
}
