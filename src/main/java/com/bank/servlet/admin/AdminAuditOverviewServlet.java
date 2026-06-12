package com.bank.servlet.admin;

import com.bank.dto.AdminAuditOverview;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAuditService;
import com.bank.service.impl.AdminAuditServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AdminAuditOverviewServlet", urlPatterns = "/admin/audit")
public class AdminAuditOverviewServlet extends HttpServlet {
    private final AdminAuditService adminAuditService = new AdminAuditServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServiceResult<AdminAuditOverview> result = adminAuditService.overview();
        if (result.isSuccess()) {
            request.setAttribute("overview", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/audit.jsp").forward(request, response);
    }
}
