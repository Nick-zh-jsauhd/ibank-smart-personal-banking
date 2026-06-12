package com.bank.servlet;

import com.bank.dto.SessionUser;
import com.bank.service.NotificationService;
import com.bank.service.impl.NotificationServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "NotificationReadAllServlet", urlPatterns = "/notifications/read-all")
public class NotificationReadAllServlet extends HttpServlet {
    private final NotificationService notificationService = new NotificationServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        notificationService.markAllRead(sessionUser.getCustomerId());
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}
