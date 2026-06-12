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
import java.io.IOException;

@WebServlet(name = "PayPasswordServlet", urlPatterns = "/security/pay-password")
public class PayPasswordServlet extends HttpServlet {
    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/security/payPassword.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        String loginPassword = RequestUtil.trim(request, "loginPassword");
        String payPassword = RequestUtil.trim(request, "payPassword");
        String confirmPayPassword = RequestUtil.trim(request, "confirmPayPassword");

        ServiceResult<Void> result = userService.setPayPassword(
                sessionUser.getUserId(), loginPassword, payPassword, confirmPayPassword);
        if (result.isSuccess()) {
            request.setAttribute("message", result.getMessage());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/security/payPassword.jsp").forward(request, response);
    }
}
