package com.bank.servlet.admin;

import com.bank.dto.AdminRoleView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminSecurityService;
import com.bank.service.impl.AdminSecurityServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminSecurityRoleListServlet", urlPatterns = "/admin/security/roles")
public class AdminSecurityRoleListServlet extends HttpServlet {
    private final AdminSecurityService adminSecurityService = new AdminSecurityServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServiceResult<List<AdminRoleView>> result = adminSecurityService.listRoles();
        if (result.isSuccess()) {
            request.setAttribute("roles", result.getData());
        } else {
            request.setAttribute("roles", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/securityRoles.jsp").forward(request, response);
    }
}
