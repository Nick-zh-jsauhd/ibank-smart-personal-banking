package com.bank.servlet.admin;

import com.bank.dto.AdminSessionUser;
import com.bank.dto.AdminUserDetail;
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

@WebServlet(name = "AdminSecurityUserDetailServlet", urlPatterns = "/admin/security/admin/detail")
public class AdminSecurityUserDetailServlet extends HttpServlet {
    private final AdminSecurityService adminSecurityService = new AdminSecurityServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long userId = parseLong(RequestUtil.trim(request, "userId"));
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/security/admins");
            return;
        }
        renderDetail(request, response, userId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        Long userId = parseLong(RequestUtil.trim(request, "userId"));
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/security/admins");
            return;
        }
        ServiceResult<Void> result = adminSecurityService.updateAdmin(adminUser.getUserId(), userId,
                RequestUtil.trim(request, "status"), request.getParameterValues("roleCode"),
                RequestUtil.trim(request, "resetPassword"), RequestUtil.clientIp(request));
        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        response.sendRedirect(request.getContextPath() + "/admin/security/admin/detail?userId=" + userId);
    }

    private void renderDetail(HttpServletRequest request, HttpServletResponse response, long userId)
            throws ServletException, IOException {
        ServiceResult<AdminUserDetail> result = adminSecurityService.getAdminUserDetail(userId);
        if (result.isSuccess()) {
            request.setAttribute("detail", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/securityAdminDetail.jsp").forward(request, response);
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
}
