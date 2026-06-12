package com.bank.servlet.admin;

import com.bank.bean.AdminAlert;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminAlertService;
import com.bank.service.impl.AdminAlertServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminMessageDetailServlet", urlPatterns = "/admin/message/detail")
public class AdminMessageDetailServlet extends HttpServlet {
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long alertId = parseLong(RequestUtil.trim(request, "alertId"));
        if (alertId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/messages");
            return;
        }

        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<AdminAlert> result = adminAlertService.getAlert(adminUser.getUserId(), alertId);
        if (result.isSuccess()) {
            request.setAttribute("alert", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/messageDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long alertId = parseLong(RequestUtil.trim(request, "alertId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");

        ServiceResult<Void> result;
        if (alertId == null) {
            result = ServiceResult.failure("后台消息不存在。");
        } else {
            result = adminAlertService.handleAlert(adminUser.getUserId(), alertId,
                    RequestUtil.trim(request, "action"),
                    RequestUtil.trim(request, "note"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (alertId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/messages");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/message/detail?alertId=" + alertId);
        }
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
