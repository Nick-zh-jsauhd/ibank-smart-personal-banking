package com.bank.servlet;

import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.dto.WealthHoldingView;
import com.bank.service.WealthService;
import com.bank.service.impl.WealthServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "WealthHoldingServlet", urlPatterns = "/wealth/holdings")
public class WealthHoldingServlet extends HttpServlet {
    private final WealthService wealthService = new WealthServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        loadHoldings(request);
        request.getRequestDispatcher("/wealth/holdingList.jsp").forward(request, response);
    }

    private void loadHoldings(HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<List<WealthHoldingView>> result = wealthService.listHoldings(sessionUser.getCustomerId());
        if (result.isSuccess()) {
            request.setAttribute("holdings", result.getData());
        } else {
            request.setAttribute("holdings", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
    }
}
