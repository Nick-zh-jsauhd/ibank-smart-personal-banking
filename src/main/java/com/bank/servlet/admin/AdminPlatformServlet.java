package com.bank.servlet.admin;

import com.bank.dto.AdminDashboardMetrics;
import com.bank.dto.ServiceResult;
import com.bank.dto.SimulationDashboardView;
import com.bank.service.AdminService;
import com.bank.service.SimulationService;
import com.bank.service.impl.AdminServiceImpl;
import com.bank.service.impl.SimulationServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AdminPlatformServlet", urlPatterns = "/admin/platform")
public class AdminPlatformServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();
    private final SimulationService simulationService = new SimulationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServiceResult<AdminDashboardMetrics> metricsResult = adminService.dashboardMetrics();
        if (metricsResult.isSuccess()) {
            request.setAttribute("metrics", metricsResult.getData());
        }
        ServiceResult<SimulationDashboardView> simulationResult = simulationService.dashboard(null);
        if (simulationResult.isSuccess()) {
            request.setAttribute("simulationView", simulationResult.getData());
        }
        request.getRequestDispatcher("/admin/platform.jsp").forward(request, response);
    }
}
