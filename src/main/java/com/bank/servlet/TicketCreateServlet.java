package com.bank.servlet;

import com.bank.bean.ServiceTicket;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
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

@WebServlet(name = "TicketCreateServlet", urlPatterns = "/ticket/create")
public class TicketCreateServlet extends HttpServlet {
    private final TicketService ticketService = new TicketServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("prefillType", RequestUtil.trim(request, "ticketType"));
        request.setAttribute("prefillBusinessType", RequestUtil.trim(request, "businessType"));
        request.setAttribute("prefillBusinessId", RequestUtil.trim(request, "businessId"));
        request.getRequestDispatcher("/ticket/ticketCreate.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        SessionUser loginUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<ServiceTicket> result = ticketService.createTicket(
                loginUser.getCustomerId(),
                loginUser.getUserId(),
                RequestUtil.trim(request, "ticketType"),
                RequestUtil.trim(request, "priority"),
                RequestUtil.trim(request, "title"),
                RequestUtil.trim(request, "description"),
                RequestUtil.trim(request, "relatedBusinessType"),
                RequestUtil.trim(request, "relatedBusinessId"));

        if (result.isSuccess()) {
            HttpSession session = request.getSession();
            session.setAttribute("success", result.getMessage());
            response.sendRedirect(request.getContextPath() + "/ticket/detail?ticketId="
                    + result.getData().getTicketId());
            return;
        }

        request.setAttribute("error", result.getMessage());
        request.setAttribute("prefillType", RequestUtil.trim(request, "ticketType"));
        request.setAttribute("prefillPriority", RequestUtil.trim(request, "priority"));
        request.setAttribute("prefillTitle", RequestUtil.trim(request, "title"));
        request.setAttribute("prefillDescription", RequestUtil.trim(request, "description"));
        request.setAttribute("prefillBusinessType", RequestUtil.trim(request, "relatedBusinessType"));
        request.setAttribute("prefillBusinessId", RequestUtil.trim(request, "relatedBusinessId"));
        request.getRequestDispatcher("/ticket/ticketCreate.jsp").forward(request, response);
    }
}
