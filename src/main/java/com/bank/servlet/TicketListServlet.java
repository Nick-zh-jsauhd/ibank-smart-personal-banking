package com.bank.servlet;

import com.bank.bean.ServiceTicket;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.TicketService;
import com.bank.service.impl.TicketServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "TicketListServlet", urlPatterns = "/tickets")
public class TicketListServlet extends HttpServlet {
    private final TicketService ticketService = new TicketServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        SessionUser loginUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<List<ServiceTicket>> result = ticketService.listCustomerTickets(loginUser.getCustomerId());
        if (result.isSuccess()) {
            request.setAttribute("tickets", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/ticket/ticketList.jsp").forward(request, response);
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
