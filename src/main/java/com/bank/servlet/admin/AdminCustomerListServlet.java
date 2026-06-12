package com.bank.servlet.admin;

import com.bank.dto.AdminCustomerView;
import com.bank.dto.ServiceResult;
import com.bank.service.AdminService;
import com.bank.service.impl.AdminServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminCustomerListServlet", urlPatterns = "/admin/customers")
public class AdminCustomerListServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = RequestUtil.trim(request, "keyword");
        String riskLevel = RequestUtil.trim(request, "riskLevel");
        ServiceResult<List<AdminCustomerView>> result = adminService.listCustomers(keyword, riskLevel);
        if (result.isSuccess()) {
            request.setAttribute("customers", result.getData());
        } else {
            request.setAttribute("customers", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("riskLevel", riskLevel);
        request.getRequestDispatcher("/admin/customers.jsp").forward(request, response);
    }
}
