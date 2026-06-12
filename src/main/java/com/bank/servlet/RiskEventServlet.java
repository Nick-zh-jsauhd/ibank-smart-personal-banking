package com.bank.servlet;

import com.bank.dto.RiskEventView;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.RiskService;
import com.bank.service.impl.RiskServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "RiskEventServlet", urlPatterns = "/risk/events")
public class RiskEventServlet extends HttpServlet {
    private final RiskService riskService = new RiskServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        String decision = RequestUtil.trim(request, "decision");
        ServiceResult<List<RiskEventView>> result = riskService.listEvents(sessionUser.getCustomerId(), decision);
        if (result.isSuccess()) {
            request.setAttribute("events", result.getData());
        } else {
            request.setAttribute("events", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedDecision", decision);
        request.getRequestDispatcher("/risk/eventList.jsp").forward(request, response);
    }
}
