package com.bank.servlet.admin;

import com.bank.bean.ReconciliationActionLog;
import com.bank.bean.ReconciliationItem;
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
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AdminReconciliationItemDetailServlet", urlPatterns = "/admin/reconciliation/item/detail")
public class AdminReconciliationItemDetailServlet extends HttpServlet {
    private final ReconciliationService reconciliationService = new ReconciliationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long itemId = parseLong(RequestUtil.trim(request, "itemId"));
        if (itemId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/reconciliation/items");
            return;
        }

        ServiceResult<ReconciliationItem> itemResult = reconciliationService.getItem(itemId);
        if (itemResult.isSuccess()) {
            request.setAttribute("item", itemResult.getData());
        } else {
            request.setAttribute("error", itemResult.getMessage());
        }

        ServiceResult<List<ReconciliationActionLog>> logResult = reconciliationService.listActionLogs(itemId);
        if (logResult.isSuccess()) {
            request.setAttribute("actionLogs", logResult.getData());
        } else {
            request.setAttribute("actionLogs", Collections.emptyList());
            request.setAttribute("error", logResult.getMessage());
        }
        request.getRequestDispatcher("/admin/reconciliationItemDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long itemId = parseLong(RequestUtil.trim(request, "itemId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");

        ServiceResult<Void> result;
        if (itemId == null) {
            result = ServiceResult.failure("对账异常不存在。");
        } else {
            result = reconciliationService.handleItem(itemId, adminUser.getUserId(),
                    RequestUtil.trim(request, "targetStatus"),
                    RequestUtil.trim(request, "note"),
                    RequestUtil.clientIp(request));
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (itemId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/reconciliation/items");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/reconciliation/item/detail?itemId=" + itemId);
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
