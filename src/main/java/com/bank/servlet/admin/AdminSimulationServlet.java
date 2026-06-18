package com.bank.servlet.admin;

import com.bank.bean.SimulationRun;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.dto.SimulationDashboardView;
import com.bank.service.SimulationService;
import com.bank.service.impl.SimulationServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminSimulationServlet", urlPatterns = "/admin/simulation")
public class AdminSimulationServlet extends HttpServlet {
    private final SimulationService simulationService = new SimulationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long runId = parseLong(RequestUtil.trim(request, "runId"));
        ServiceResult<SimulationDashboardView> result = simulationService.dashboard(runId);
        if (result.isSuccess()) {
            request.setAttribute("view", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/simulation.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        int eventCount = parseInt(RequestUtil.trim(request, "eventCount"), 10);
        ServiceResult<SimulationRun> result = simulationService.runOnce(adminUser.getUserId(),
                RequestUtil.trim(request, "scenarioCode"), eventCount, RequestUtil.trim(request, "speed"),
                RequestUtil.clientIp(request));

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (result.isSuccess() && result.getData() != null && result.getData().getRunId() != null) {
            response.sendRedirect(request.getContextPath() + "/admin/simulation?runId="
                    + result.getData().getRunId());
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/simulation");
        }
    }

    private void consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String success = (String) session.getAttribute("success");
        String error = (String) session.getAttribute("error");
        if (success != null) {
            request.setAttribute("success", success);
            session.removeAttribute("success");
        }
        if (error != null) {
            request.setAttribute("error", error);
            session.removeAttribute("error");
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

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
