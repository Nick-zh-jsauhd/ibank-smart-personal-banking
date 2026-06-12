package com.bank.servlet.admin;

import com.bank.dto.AdminRiskEventDetail;
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

@WebServlet(name = "AdminRiskEventDetailServlet", urlPatterns = "/admin/risk/event/detail")
public class AdminRiskEventDetailServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);

        Long eventId = parseLong(RequestUtil.trim(request, "eventId"));
        if (eventId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/risk/events");
            return;
        }

        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<AdminRiskEventDetail> result = adminService.getRiskEventDetail(eventId,
                adminUser.getUserId(), RequestUtil.clientIp(request));
        if (result.isSuccess()) {
            request.setAttribute("detail", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/riskEventDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long eventId = parseLong(RequestUtil.trim(request, "eventId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");

        ServiceResult<Void> result;
        if (eventId == null) {
            result = ServiceResult.failure("风险事件不存在。");
        } else {
            result = adminService.handleRiskEvent(adminUser.getUserId(), eventId,
                    RequestUtil.trim(request, "handleResult"),
                    RequestUtil.trim(request, "accountAction"),
                    RequestUtil.trim(request, "note"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (eventId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/risk/events");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/risk/event/detail?eventId=" + eventId);
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
