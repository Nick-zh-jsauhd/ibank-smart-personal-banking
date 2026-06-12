package com.bank.servlet.admin;

import com.bank.dto.AdminSessionUser;
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

@WebServlet(name = "AdminLogoutServlet", urlPatterns = "/admin/logout")
public class AdminLogoutServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
            if (adminUser != null) {
                adminService.audit(adminUser.getUserId(), "ADMIN_LOGOUT", "ADMIN",
                        String.valueOf(adminUser.getUserId()), "管理员退出登录", RequestUtil.clientIp(request));
            }
            session.removeAttribute("adminUser");
        }
        response.sendRedirect(request.getContextPath() + "/admin/login");
    }
}
