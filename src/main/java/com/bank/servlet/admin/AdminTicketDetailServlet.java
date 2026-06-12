package com.bank.servlet.admin;

import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.dto.TicketDetail;
import com.bank.service.TicketService;
import com.bank.service.impl.TicketServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminTicketDetailServlet", urlPatterns = "/admin/ticket/detail")
public class AdminTicketDetailServlet extends HttpServlet {
    private final TicketService ticketService = new TicketServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long ticketId = parseLong(RequestUtil.trim(request, "ticketId"));
        if (ticketId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/tickets");
            return;
        }
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<TicketDetail> result = ticketService.getAdminTicket(adminUser.getUserId(), ticketId);
        if (result.isSuccess()) {
            request.setAttribute("detail", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/ticketDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long ticketId = parseLong(RequestUtil.trim(request, "ticketId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<Void> result;
        if (ticketId == null) {
            result = ServiceResult.failure("工单不存在。");
        } else {
            result = ticketService.handleAdminAction(adminUser.getUserId(),
                    ticketId,
                    RequestUtil.trim(request, "action"),
                    RequestUtil.trim(request, "assignedRoleCode"),
                    RequestUtil.trim(request, "replyContent"),
                    RequestUtil.trim(request, "note"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (ticketId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/tickets");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/ticket/detail?ticketId=" + ticketId);
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
