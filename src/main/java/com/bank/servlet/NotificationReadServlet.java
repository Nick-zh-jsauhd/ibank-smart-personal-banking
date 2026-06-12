package com.bank.servlet;

import com.bank.dto.SessionUser;
import com.bank.service.NotificationService;
import com.bank.service.impl.NotificationServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "NotificationReadServlet", urlPatterns = "/notifications/read")
public class NotificationReadServlet extends HttpServlet {
    private final NotificationService notificationService = new NotificationServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        Long notificationId = parseLong(RequestUtil.trim(request, "notificationId"));
        if (notificationId != null) {
            notificationService.markRead(sessionUser.getCustomerId(), notificationId);
        }
        response.sendRedirect(request.getContextPath() + "/notifications");
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
