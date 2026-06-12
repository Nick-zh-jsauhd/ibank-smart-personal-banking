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
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminMessageListServlet", urlPatterns = "/admin/messages")
public class AdminMessageListServlet extends HttpServlet {
    private final AdminAlertService adminAlertService = new AdminAlertServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        String status = RequestUtil.trim(request, "status");
        String severity = RequestUtil.trim(request, "severity");

        ServiceResult<List<AdminAlert>> result =
                adminAlertService.listAlerts(adminUser.getUserId(), status, severity);
        if (result.isSuccess()) {
            request.setAttribute("alerts", result.getData());
        } else {
            request.setAttribute("alerts", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }

        ServiceResult<Integer> openCount = adminAlertService.countOpenAlerts(adminUser.getUserId());
        request.setAttribute("openAlertCount", openCount.isSuccess() ? openCount.getData() : 0);
        request.setAttribute("selectedStatus", status);
        request.setAttribute("selectedSeverity", severity);
        request.getRequestDispatcher("/admin/messages.jsp").forward(request, response);
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
}
