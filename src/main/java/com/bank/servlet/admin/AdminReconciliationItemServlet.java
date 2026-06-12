package com.bank.servlet.admin;

import com.bank.bean.ReconciliationItem;
import com.bank.dto.ServiceResult;
import com.bank.service.ReconciliationService;
import com.bank.service.impl.ReconciliationServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminReconciliationItemServlet", urlPatterns = "/admin/reconciliation/items")
public class AdminReconciliationItemServlet extends HttpServlet {
    private final ReconciliationService reconciliationService = new ReconciliationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String status = RequestUtil.trim(request, "status");
        String severity = RequestUtil.trim(request, "severity");
        String checkType = RequestUtil.trim(request, "checkType");

        ServiceResult<List<ReconciliationItem>> result =
                reconciliationService.listExceptionItems(status, severity, checkType);
        if (result.isSuccess()) {
            request.setAttribute("items", result.getData());
        } else {
            request.setAttribute("items", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedStatus", status);
        request.setAttribute("selectedSeverity", severity);
        request.setAttribute("selectedCheckType", checkType);
        request.getRequestDispatcher("/admin/reconciliationItems.jsp").forward(request, response);
    }
}
