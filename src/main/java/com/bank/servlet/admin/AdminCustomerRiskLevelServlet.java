package com.bank.servlet.admin;

import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminService;
import com.bank.service.impl.AdminServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AdminCustomerRiskLevelServlet", urlPatterns = "/admin/customer/risk-level")
public class AdminCustomerRiskLevelServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        Long customerId = parseLong(RequestUtil.trim(request, "customerId"));

        ServiceResult<Void> result;
        if (customerId == null) {
            result = ServiceResult.failure("客户不存在。");
        } else {
            result = adminService.adjustCustomerRiskLevel(adminUser.getUserId(), customerId,
                    RequestUtil.trim(request, "riskLevel"),
                    RequestUtil.trim(request, "reason"),
                    RequestUtil.clientIp(request));
        }

        request.getSession().setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (customerId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/customer/detail?customerId=" + customerId);
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
}
