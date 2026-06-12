package com.bank.servlet;

import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.UserService;
import com.bank.service.impl.UserServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("loginUser") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/auth/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identity = RequestUtil.trim(request, "identity");
        String password = RequestUtil.trim(request, "password");
        ServiceResult<SessionUser> result = userService.login(
                identity,
                password,
                RequestUtil.clientIp(request),
                RequestUtil.userAgent(request)
        );

        if (result.isSuccess()) {
            request.getSession(true).setAttribute("loginUser", result.getData());
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.setAttribute("error", result.getMessage());
        request.setAttribute("identity", identity);
        request.getRequestDispatcher("/auth/login.jsp").forward(request, response);
    }
}
