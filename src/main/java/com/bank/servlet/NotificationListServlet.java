package com.bank.servlet;

import com.bank.bean.Notification;
import com.bank.dto.ServiceResult;
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
import java.util.Collections;
import java.util.List;

@WebServlet(name = "NotificationListServlet", urlPatterns = "/notifications")
public class NotificationListServlet extends HttpServlet {
    private final NotificationService notificationService = new NotificationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        String type = RequestUtil.trim(request, "type");
        ServiceResult<List<Notification>> result =
                notificationService.listNotifications(sessionUser.getCustomerId(), type);
        if (result.isSuccess()) {
            request.setAttribute("notifications", result.getData());
        } else {
            request.setAttribute("notifications", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }

        ServiceResult<Integer> unreadResult = notificationService.countUnread(sessionUser.getCustomerId());
        request.setAttribute("unreadCount", unreadResult.isSuccess() ? unreadResult.getData() : 0);
        request.setAttribute("selectedType", type);
        request.getRequestDispatcher("/notification/notificationList.jsp").forward(request, response);
    }
}
