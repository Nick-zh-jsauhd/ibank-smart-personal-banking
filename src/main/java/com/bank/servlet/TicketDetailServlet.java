package com.bank.servlet;

import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
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

@WebServlet(name = "TicketDetailServlet", urlPatterns = "/ticket/detail")
public class TicketDetailServlet extends HttpServlet {
    private final TicketService ticketService = new TicketServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long ticketId = parseLong(RequestUtil.trim(request, "ticketId"));
        if (ticketId == null) {
            response.sendRedirect(request.getContextPath() + "/tickets");
            return;
        }
        SessionUser loginUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<TicketDetail> result = ticketService.getCustomerTicket(loginUser.getCustomerId(), ticketId);
        if (result.isSuccess()) {
            request.setAttribute("detail", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/ticket/ticketDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long ticketId = parseLong(RequestUtil.trim(request, "ticketId"));
        SessionUser loginUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<Void> result;
        if (ticketId == null) {
            result = ServiceResult.failure("工单不存在。");
        } else {
            String action = RequestUtil.trim(request, "action");
            if ("reply".equals(action)) {
                result = ticketService.addCustomerReply(loginUser.getCustomerId(), loginUser.getUserId(),
                        ticketId, RequestUtil.trim(request, "content"));
            } else if ("close".equals(action)) {
                result = ticketService.closeByCustomer(loginUser.getCustomerId(), loginUser.getUserId(),
                        ticketId, RequestUtil.trim(request, "note"));
            } else if ("reopen".equals(action)) {
                result = ticketService.reopenByCustomer(loginUser.getCustomerId(), loginUser.getUserId(),
                        ticketId, RequestUtil.trim(request, "note"));
            } else {
                result = ServiceResult.failure("请选择正确的工单操作。");
            }
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (ticketId == null) {
            response.sendRedirect(request.getContextPath() + "/tickets");
        } else {
            response.sendRedirect(request.getContextPath() + "/ticket/detail?ticketId=" + ticketId);
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
