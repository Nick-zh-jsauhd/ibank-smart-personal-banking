package com.bank.servlet.admin;

import com.bank.dto.AdminLoginLogView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAuditService;
import com.bank.service.impl.AdminAuditServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminAuditLoginLogServlet", urlPatterns = "/admin/audit/login-logs")
public class AdminAuditLoginLogServlet extends HttpServlet {
    private final AdminAuditService adminAuditService = new AdminAuditServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identity = RequestUtil.trim(request, "identity");
        String userRole = RequestUtil.trim(request, "userRole");
        String success = RequestUtil.trim(request, "success");
        String startDate = RequestUtil.trim(request, "startDate");
        String endDate = RequestUtil.trim(request, "endDate");

        ServiceResult<List<AdminLoginLogView>> result = adminAuditService.listLoginLogs(identity,
                userRole, success, startDate, endDate);
        if (result.isSuccess()) {
            request.setAttribute("logs", result.getData());
        } else {
            request.setAttribute("logs", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedIdentity", identity);
        request.setAttribute("selectedUserRole", userRole);
        request.setAttribute("selectedSuccess", success);
        request.setAttribute("selectedStartDate", startDate);
        request.setAttribute("selectedEndDate", endDate);
        request.getRequestDispatcher("/admin/auditLoginLogs.jsp").forward(request, response);
    }
}
