package com.bank.servlet.admin;

import com.bank.dto.AdminRiskEventView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminService;
import com.bank.service.impl.AdminServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminRiskEventServlet", urlPatterns = "/admin/risk/events")
public class AdminRiskEventServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String decision = RequestUtil.trim(request, "decision");
        String handleStatus = RequestUtil.trim(request, "handleStatus");
        ServiceResult<List<AdminRiskEventView>> result = adminService.listRiskEvents(decision, handleStatus);
        if (result.isSuccess()) {
            request.setAttribute("events", result.getData());
        } else {
            request.setAttribute("events", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedDecision", decision);
        request.setAttribute("selectedHandleStatus", handleStatus);
        request.getRequestDispatcher("/admin/riskEvents.jsp").forward(request, response);
    }
}
