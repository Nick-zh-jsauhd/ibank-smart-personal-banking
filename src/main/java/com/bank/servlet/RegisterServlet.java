package com.bank.servlet;

import com.bank.dto.RegisterRequest;
import com.bank.dto.ServiceResult;
import com.bank.service.UserService;
import com.bank.service.impl.UserServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RegisterServlet", urlPatterns = "/register")
public class RegisterServlet extends HttpServlet {
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/auth/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(RequestUtil.trim(request, "username"));
        registerRequest.setPhone(RequestUtil.trim(request, "phone"));
        registerRequest.setFullName(RequestUtil.trim(request, "fullName"));
        registerRequest.setEmail(RequestUtil.trim(request, "email"));
        registerRequest.setAddress(RequestUtil.trim(request, "address"));
        registerRequest.setPassword(RequestUtil.trim(request, "password"));
        registerRequest.setConfirmPassword(RequestUtil.trim(request, "confirmPassword"));

        ServiceResult<Void> result = userService.register(registerRequest);
        if (result.isSuccess()) {
            request.getSession(true).setAttribute("flash", result.getMessage());
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute("error", result.getMessage());
        request.setAttribute("form", registerRequest);
        request.getRequestDispatcher("/auth/register.jsp").forward(request, response);
    }
}
