package com.bank.servlet.admin;

import com.bank.bean.ReconciliationBatch;
import com.bank.dto.AdminSessionUser;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminReconciliationServlet", urlPatterns = "/admin/reconciliation")
public class AdminReconciliationServlet extends HttpServlet {
    private final ReconciliationService reconciliationService = new ReconciliationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        ServiceResult<List<ReconciliationBatch>> result = reconciliationService.listRecentBatches();
        if (result.isSuccess()) {
            request.setAttribute("batches", result.getData());
        } else {
            request.setAttribute("batches", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedDate", LocalDate.now().toString());
        request.getRequestDispatcher("/admin/reconciliation.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        LocalDate reconDate = parseDate(RequestUtil.trim(request, "reconDate"));

        ServiceResult<ReconciliationBatch> result;
        if (reconDate == null) {
            result = ServiceResult.failure("请选择正确的对账日期。");
        } else {
            result = reconciliationService.run(reconDate, adminUser.getUserId(), RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (result.isSuccess() && result.getData() != null && result.getData().getBatchId() != null) {
            response.sendRedirect(request.getContextPath() + "/admin/reconciliation/detail?batchId="
                    + result.getData().getBatchId());
            return;
        }
        response.sendRedirect(request.getContextPath() + "/admin/reconciliation");
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
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
