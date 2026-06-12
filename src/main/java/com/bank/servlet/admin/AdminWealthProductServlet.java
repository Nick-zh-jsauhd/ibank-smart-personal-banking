package com.bank.servlet.admin;

import com.bank.bean.WealthProduct;
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
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminWealthProductServlet", urlPatterns = "/admin/wealth/products")
public class AdminWealthProductServlet extends HttpServlet {
    private final AdminService adminService = new AdminServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);

        String status = RequestUtil.trim(request, "status");
        ServiceResult<List<WealthProduct>> result = adminService.listWealthProducts(status);
        if (result.isSuccess()) {
            request.setAttribute("products", result.getData());
        } else {
            request.setAttribute("products", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedStatus", status);
        request.getRequestDispatcher("/admin/wealthProducts.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        Long productId = parseLong(RequestUtil.trim(request, "productId"));
        String filterStatus = RequestUtil.trim(request, "filterStatus");

        ServiceResult<Void> result;
        if (productId == null) {
            result = ServiceResult.failure("理财产品不存在。");
        } else {
            result = adminService.updateWealthProduct(adminUser.getUserId(), productId,
                    RequestUtil.trim(request, "productName"),
                    RequestUtil.trim(request, "riskLevel"),
                    RequestUtil.trim(request, "expectedRate"),
                    RequestUtil.trim(request, "periodDays"),
                    RequestUtil.trim(request, "minAmount"),
                    RequestUtil.trim(request, "maxAmount"),
                    RequestUtil.trim(request, "status"),
                    RequestUtil.trim(request, "description"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        response.sendRedirect(request.getContextPath() + "/admin/wealth/products"
                + "?status=" + encode(filterStatus));
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

    private String encode(String value) throws IOException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }
}
