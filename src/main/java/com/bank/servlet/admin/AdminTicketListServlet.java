package com.bank.servlet.admin;

import com.bank.bean.ServiceTicket;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
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
import java.util.List;

@WebServlet(name = "AdminTicketListServlet", urlPatterns = "/admin/tickets")
public class AdminTicketListServlet extends HttpServlet {
    private final TicketService ticketService = new TicketServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        String status = RequestUtil.trim(request, "status");
        String ticketType = RequestUtil.trim(request, "ticketType");
        String priority = RequestUtil.trim(request, "priority");
        String keyword = RequestUtil.trim(request, "keyword");
        boolean assignedToMe = "1".equals(RequestUtil.trim(request, "assignedToMe"));

        ServiceResult<List<ServiceTicket>> result = ticketService.listAdminTickets(adminUser.getUserId(),
                status, ticketType, priority, assignedToMe, keyword);
        if (result.isSuccess()) {
            request.setAttribute("tickets", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedStatus", status);
        request.setAttribute("selectedTicketType", ticketType);
        request.setAttribute("selectedPriority", priority);
        request.setAttribute("assignedToMe", Boolean.valueOf(assignedToMe));
        request.setAttribute("keyword", keyword);
        request.getRequestDispatcher("/admin/tickets.jsp").forward(request, response);
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
