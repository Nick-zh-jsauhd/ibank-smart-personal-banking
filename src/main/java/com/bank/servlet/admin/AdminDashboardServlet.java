package com.bank.servlet.admin;

import com.bank.dto.AdminDashboardMetrics;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminService;
import com.bank.service.impl.AdminServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = "/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServiceResult<AdminDashboardMetrics> result = adminService.dashboardMetrics();
        if (result.isSuccess()) {
            request.setAttribute("metrics", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/dashboard.jsp").forward(request, response);
    }
}
