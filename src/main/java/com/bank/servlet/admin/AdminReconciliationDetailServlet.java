package com.bank.servlet.admin;

import com.bank.bean.ReconciliationBatch;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminReconciliationDetailServlet", urlPatterns = "/admin/reconciliation/detail")
public class AdminReconciliationDetailServlet extends HttpServlet {
    private final ReconciliationService reconciliationService = new ReconciliationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long batchId = parseLong(RequestUtil.trim(request, "batchId"));
        if (batchId == null) {
            request.setAttribute("error", "对账批次不存在。");
            request.setAttribute("items", Collections.emptyList());
            request.getRequestDispatcher("/admin/reconciliationDetail.jsp").forward(request, response);
            return;
        }

        ServiceResult<ReconciliationBatch> batchResult = reconciliationService.getBatch(batchId);
        if (batchResult.isSuccess()) {
            request.setAttribute("batch", batchResult.getData());
        } else {
            request.setAttribute("error", batchResult.getMessage());
        }

        ServiceResult<List<ReconciliationItem>> itemResult = reconciliationService.listItems(batchId);
        if (itemResult.isSuccess()) {
            request.setAttribute("items", itemResult.getData());
        } else {
            request.setAttribute("items", Collections.emptyList());
            request.setAttribute("error", itemResult.getMessage());
        }
        request.getRequestDispatcher("/admin/reconciliationDetail.jsp").forward(request, response);
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
