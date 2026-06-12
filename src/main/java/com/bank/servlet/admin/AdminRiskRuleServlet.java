package com.bank.servlet.admin;

import com.bank.bean.RiskLimitRule;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminService;
import com.bank.service.impl.AdminServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminRiskRuleServlet", urlPatterns = "/admin/risk/rules")
public class AdminRiskRuleServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);

        String txnType = RequestUtil.trim(request, "txnType");
        String riskLevel = RequestUtil.trim(request, "riskLevel");
        ServiceResult<List<RiskLimitRule>> result = adminService.listRiskRules(txnType, riskLevel);
        if (result.isSuccess()) {
            request.setAttribute("rules", result.getData());
        } else {
            request.setAttribute("rules", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedTxnType", txnType);
        request.setAttribute("selectedRiskLevel", riskLevel);
        request.getRequestDispatcher("/admin/riskRules.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        Long ruleId = parseLong(RequestUtil.trim(request, "ruleId"));
        String txnType = RequestUtil.trim(request, "filterTxnType");
        String riskLevel = RequestUtil.trim(request, "filterRiskLevel");

        ServiceResult<Void> result;
        if (ruleId == null) {
            result = ServiceResult.failure("风控规则不存在。");
        } else {
            result = adminService.updateRiskRule(adminUser.getUserId(), ruleId,
                    RequestUtil.trim(request, "singleLimit"),
                    RequestUtil.trim(request, "dailyAmountLimit"),
                    RequestUtil.trim(request, "dailyCountLimit"),
                    RequestUtil.trim(request, "status"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        response.sendRedirect(request.getContextPath() + "/admin/risk/rules"
                + "?txnType=" + encode(txnType) + "&riskLevel=" + encode(riskLevel));
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

    private String encode(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }
}
