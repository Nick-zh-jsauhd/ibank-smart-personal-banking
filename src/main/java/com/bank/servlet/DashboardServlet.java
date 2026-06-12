package com.bank.servlet;

import com.bank.dto.CustomerDashboardView;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.CustomerDashboardService;
import com.bank.service.impl.CustomerDashboardServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "DashboardServlet", urlPatterns = "/dashboard")
public class DashboardServlet extends HttpServlet {
    private final CustomerDashboardService dashboardService = new CustomerDashboardServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<CustomerDashboardView> result = dashboardService.loadDashboard(sessionUser.getCustomerId());
        request.setAttribute("dashboardView", result.getData());
        request.setAttribute("activeNav", "dashboard");
        if (!result.isSuccess()) {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
    }
}
