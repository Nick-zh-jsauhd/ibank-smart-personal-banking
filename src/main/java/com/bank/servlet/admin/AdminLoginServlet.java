package com.bank.servlet.admin;

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

@WebServlet(name = "AdminLoginServlet", urlPatterns = "/admin/login")
public class AdminLoginServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("adminUser") != null) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            return;
        }
        request.getRequestDispatcher("/admin/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identity = RequestUtil.trim(request, "identity");
        String password = RequestUtil.trim(request, "password");
        ServiceResult<AdminSessionUser> result = adminService.login(
                identity,
                password,
                RequestUtil.clientIp(request),
                RequestUtil.userAgent(request)
        );
        if (result.isSuccess()) {
            request.getSession(true).setAttribute("adminUser", result.getData());
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            return;
        }
        request.setAttribute("error", result.getMessage());
        request.setAttribute("identity", identity);
        request.getRequestDispatcher("/admin/login.jsp").forward(request, response);
    }
}
