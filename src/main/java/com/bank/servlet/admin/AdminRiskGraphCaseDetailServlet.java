package com.bank.servlet.admin;

import com.bank.dto.AdminRiskGraphReviewCaseView;
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

@WebServlet(name = "AdminRiskGraphCaseDetailServlet", urlPatterns = "/admin/risk/graph-case/detail")
public class AdminRiskGraphCaseDetailServlet extends HttpServlet {
    private final RiskGraphReviewCaseService reviewCaseService = new RiskGraphReviewCaseServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        String caseId = RequestUtil.trim(request, "caseId");
        ServiceResult<AdminRiskGraphReviewCaseView> result = reviewCaseService.getCase(caseId);
        if (result.isSuccess()) {
            request.setAttribute("reviewCase", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("activeNav", "risk-graph-cases");
        request.getRequestDispatcher("/admin/riskGraphCaseDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String caseId = RequestUtil.trim(request, "caseId");
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<Void> result = reviewCaseService.reviewCase(adminUser.getUserId(), caseId,
                RequestUtil.trim(request, "reviewResult"),
                RequestUtil.trim(request, "reviewNote"),
                RequestUtil.clientIp(request));

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (caseId == null || caseId.length() == 0) {
            response.sendRedirect(request.getContextPath() + "/admin/risk/graph-cases");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/risk/graph-case/detail?caseId=" + caseId);
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
}
