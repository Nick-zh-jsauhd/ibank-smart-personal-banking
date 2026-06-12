package com.bank.servlet.admin;

import com.bank.dto.AdminCustomerDetail;
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

@WebServlet(name = "AdminCustomerDetailServlet", urlPatterns = "/admin/customer/detail")
public class AdminCustomerDetailServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long customerId = parseLong(RequestUtil.trim(request, "customerId"));
        if (customerId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        ServiceResult<AdminCustomerDetail> result = adminService.getCustomerDetail(
                customerId, adminUser.getUserId(), RequestUtil.clientIp(request));
        if (result.isSuccess()) {
            request.setAttribute("detail", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/admin/customerDetail.jsp").forward(request, response);
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
