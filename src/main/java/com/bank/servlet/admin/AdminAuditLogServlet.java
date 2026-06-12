package com.bank.servlet.admin;

import com.bank.dto.AdminAuditLogView;
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

@WebServlet(name = "AdminAuditLogServlet", urlPatterns = "/admin/audit/admin-logs")
public class AdminAuditLogServlet extends HttpServlet {
    private final AdminAuditService adminAuditService = new AdminAuditServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String adminKeyword = RequestUtil.trim(request, "admin");
        String operationType = RequestUtil.trim(request, "operationType");
        String targetType = RequestUtil.trim(request, "targetType");
        String startDate = RequestUtil.trim(request, "startDate");
        String endDate = RequestUtil.trim(request, "endDate");
        boolean highRiskOnly = "1".equals(RequestUtil.trim(request, "highRiskOnly"));

        ServiceResult<List<AdminAuditLogView>> result = adminAuditService.listAdminAuditLogs(adminKeyword,
                operationType, targetType, startDate, endDate, highRiskOnly);
        if (result.isSuccess()) {
            request.setAttribute("logs", result.getData());
        } else {
            request.setAttribute("logs", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedAdmin", adminKeyword);
        request.setAttribute("selectedOperationType", operationType);
        request.setAttribute("selectedTargetType", targetType);
        request.setAttribute("selectedStartDate", startDate);
        request.setAttribute("selectedEndDate", endDate);
        request.setAttribute("highRiskOnly", highRiskOnly);
        request.getRequestDispatcher("/admin/auditAdminLogs.jsp").forward(request, response);
    }
}
