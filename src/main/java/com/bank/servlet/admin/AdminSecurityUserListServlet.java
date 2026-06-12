package com.bank.servlet.admin;

import com.bank.dto.AdminRoleView;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.AdminUserView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminSecurityService;
import com.bank.service.impl.AdminSecurityServiceImpl;
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

@WebServlet(name = "AdminSecurityUserListServlet", urlPatterns = "/admin/security/admins")
public class AdminSecurityUserListServlet extends HttpServlet {
    private final AdminSecurityService adminSecurityService = new AdminSecurityServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        renderList(request, response, RequestUtil.trim(request, "keyword"), RequestUtil.trim(request, "status"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        String username = RequestUtil.trim(request, "username");
        String phone = RequestUtil.trim(request, "phone");
        ServiceResult<Long> result = adminSecurityService.createAdmin(adminUser.getUserId(), username, phone,
                RequestUtil.trim(request, "password"), request.getParameterValues("roleCode"),
                RequestUtil.clientIp(request));

        if (result.isSuccess()) {
            HttpSession session = request.getSession();
            session.setAttribute("success", result.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/security/admin/detail?userId=" + result.getData());
            return;
        }
        request.setAttribute("error", result.getMessage());
        request.setAttribute("formUsername", username);
        request.setAttribute("formPhone", phone);
        request.setAttribute("formRoleCodes", request.getParameterValues("roleCode"));
        renderList(request, response, RequestUtil.trim(request, "keyword"), RequestUtil.trim(request, "status"));
    }

    private void renderList(HttpServletRequest request, HttpServletResponse response, String keyword, String status)
            throws ServletException, IOException {
        ServiceResult<List<AdminUserView>> usersResult = adminSecurityService.listAdminUsers(keyword, status);
        ServiceResult<List<AdminRoleView>> rolesResult = adminSecurityService.listRoles();
        request.setAttribute("admins", usersResult.isSuccess() ? usersResult.getData() : Collections.emptyList());
        request.setAttribute("roles", rolesResult.isSuccess() ? rolesResult.getData() : Collections.emptyList());
        if (!usersResult.isSuccess()) {
            request.setAttribute("error", usersResult.getMessage());
        } else if (!rolesResult.isSuccess()) {
            request.setAttribute("error", rolesResult.getMessage());
        }
        request.setAttribute("selectedKeyword", keyword);
        request.setAttribute("selectedStatus", status);
        request.getRequestDispatcher("/admin/securityAdmins.jsp").forward(request, response);
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

    @SuppressWarnings("unused")
    private String encode(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }
}
