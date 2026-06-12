package com.bank.servlet.admin;

import com.bank.bean.AdjustmentRequest;
import com.bank.dto.ServiceResult;
import com.bank.service.AdjustmentService;
import com.bank.service.impl.AdjustmentServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminAdjustmentListServlet", urlPatterns = "/admin/adjustments")
public class AdminAdjustmentListServlet extends HttpServlet {
    private final AdjustmentService adjustmentService = new AdjustmentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String status = RequestUtil.trim(request, "status");
        ServiceResult<List<AdjustmentRequest>> result = adjustmentService.listRequests(status);
        if (result.isSuccess()) {
            request.setAttribute("adjustments", result.getData());
        } else {
            request.setAttribute("adjustments", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedStatus", status);
        request.getRequestDispatcher("/admin/adjustments.jsp").forward(request, response);
    }
}
