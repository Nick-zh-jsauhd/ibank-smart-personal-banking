package com.bank.servlet.admin;

import com.bank.dto.AdminRiskGraphModelComparePage;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphScoreService;
import com.bank.service.impl.RiskGraphScoreServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminRiskGraphModelServlet", urlPatterns = "/admin/risk/graph-models")
public class AdminRiskGraphModelServlet extends HttpServlet {
    private final RiskGraphScoreService riskGraphScoreService = new RiskGraphScoreServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        String baselineModelVersion = RequestUtil.trim(request, "baselineModelVersion");
        String candidateModelVersion = RequestUtil.trim(request, "candidateModelVersion");
        String reviewCapacity = RequestUtil.trim(request, "reviewCapacity");
        String blockCapacity = RequestUtil.trim(request, "blockCapacity");

        ServiceResult<AdminRiskGraphModelComparePage> result =
                riskGraphScoreService.queryModelComparison(baselineModelVersion, candidateModelVersion,
                        reviewCapacity, blockCapacity);
        if (result.isSuccess()) {
            request.setAttribute("modelPage", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("activeNav", "risk-graph-models");
        request.getRequestDispatcher("/admin/riskGraphModels.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        if (adminUser == null) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return;
        }
        boolean operational = "1".equals(RequestUtil.trim(request, "operational"));
        ServiceResult<Void> result = riskGraphScoreService.updateModelGovernance(
                RequestUtil.trim(request, "modelVersion"),
                RequestUtil.trim(request, "modelRole"),
                RequestUtil.trim(request, "lifecycleStatus"),
                RequestUtil.trim(request, "onlineMode"),
                operational,
                RequestUtil.trim(request, "governanceNote"),
                adminUser.getUserId(),
                RequestUtil.clientIp(request));

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        response.sendRedirect(request.getContextPath() + "/admin/risk/graph-models");
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
}
