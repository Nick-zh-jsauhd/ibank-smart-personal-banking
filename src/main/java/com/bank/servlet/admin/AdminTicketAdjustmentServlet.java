package com.bank.servlet.admin;

import com.bank.bean.AdjustmentRequest;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.AdjustmentService;
import com.bank.service.impl.AdjustmentServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminTicketAdjustmentServlet", urlPatterns = "/admin/ticket/adjustment")
public class AdminTicketAdjustmentServlet extends HttpServlet {
    private final AdjustmentService adjustmentService = new AdjustmentServiceImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long ticketId = parseLong(RequestUtil.trim(request, "ticketId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");

        ServiceResult<AdjustmentRequest> result;
        if (ticketId == null) {
            result = ServiceResult.failure("工单不存在。");
        } else {
            result = adjustmentService.createFromTicket(ticketId, adminUser.getUserId(),
                    RequestUtil.trim(request, "accountNo"),
                    RequestUtil.trim(request, "direction"),
                    RequestUtil.trim(request, "amount"),
                    RequestUtil.trim(request, "reason"),
                    RequestUtil.trim(request, "evidence"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (result.isSuccess() && result.getData() != null) {
            response.sendRedirect(request.getContextPath() + "/admin/adjustment/detail?adjustmentId="
                    + result.getData().getAdjustmentId());
        } else if (ticketId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/tickets");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/ticket/detail?ticketId=" + ticketId);
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
