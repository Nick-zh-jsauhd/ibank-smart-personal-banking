package com.bank.servlet.admin;

import com.bank.dto.AdminRiskGraphReviewCasePage;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphReviewCaseService;
import com.bank.service.impl.RiskGraphReviewCaseServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminRiskGraphCaseServlet", urlPatterns = "/admin/risk/graph-cases")
public class AdminRiskGraphCaseServlet extends HttpServlet {
    private final RiskGraphReviewCaseService reviewCaseService = new RiskGraphReviewCaseServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        String modelVersion = RequestUtil.trim(request, "modelVersion");
        String caseStatus = RequestUtil.trim(request, "caseStatus");
        String caseType = RequestUtil.trim(request, "caseType");
        String reviewResult = RequestUtil.trim(request, "reviewResult");

        ServiceResult<AdminRiskGraphReviewCasePage> result =
                reviewCaseService.queryCases(modelVersion, caseStatus, caseType, reviewResult);
        if (result.isSuccess()) {
            request.setAttribute("casePage", result.getData());
            modelVersion = result.getData().getSelectedModelVersion();
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedModelVersion", modelVersion);
        request.setAttribute("selectedCaseStatus", caseStatus == null || caseStatus.length() == 0 ? "OPEN" : caseStatus);
        request.setAttribute("selectedCaseType", caseType);
        request.setAttribute("selectedReviewResult", reviewResult);
        request.setAttribute("activeNav", "risk-graph-cases");
        request.getRequestDispatcher("/admin/riskGraphCases.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<Integer> result = reviewCaseService.materializeCases(
                RequestUtil.trim(request, "modelVersion"),
                adminUser.getUserId(),
                RequestUtil.clientIp(request));

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        response.sendRedirect(request.getContextPath() + "/admin/risk/graph-cases");
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
