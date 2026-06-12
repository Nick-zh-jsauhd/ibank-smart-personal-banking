package com.bank.servlet.admin;

import com.bank.bean.AdjustmentRequest;
import com.bank.bean.ReconciliationItem;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.AdjustmentService;
import com.bank.service.ReconciliationService;
import com.bank.service.impl.AdjustmentServiceImpl;
import com.bank.service.impl.ReconciliationServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "AdminAdjustmentCreateServlet", urlPatterns = "/admin/adjustment/create")
public class AdminAdjustmentCreateServlet extends HttpServlet {
    private final AdjustmentService adjustmentService = new AdjustmentServiceImpl();
    private final ReconciliationService reconciliationService = new ReconciliationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long itemId = parseLong(RequestUtil.trim(request, "itemId"));
        if (itemId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/reconciliation/items");
            return;
        }
        loadItem(request, itemId);
        request.getRequestDispatcher("/admin/adjustmentCreate.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long itemId = parseLong(RequestUtil.trim(request, "itemId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");

        ServiceResult<AdjustmentRequest> result;
        if (itemId == null) {
            result = ServiceResult.failure("对账异常不存在。");
        } else {
            result = adjustmentService.createRequest(itemId, adminUser.getUserId(),
                    RequestUtil.trim(request, "accountNo"),
                    RequestUtil.trim(request, "direction"),
                    RequestUtil.trim(request, "amount"),
                    RequestUtil.trim(request, "reason"),
                    RequestUtil.trim(request, "evidence"),
                    RequestUtil.clientIp(request));
        }

        if (result.isSuccess() && result.getData() != null) {
            HttpSession session = request.getSession();
            session.setAttribute("success", result.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/adjustment/detail?adjustmentId="
                    + result.getData().getAdjustmentId());
            return;
        }

        request.setAttribute("error", result.getMessage());
        request.setAttribute("accountNo", RequestUtil.trim(request, "accountNo"));
        request.setAttribute("direction", RequestUtil.trim(request, "direction"));
        request.setAttribute("amount", RequestUtil.trim(request, "amount"));
        request.setAttribute("reason", RequestUtil.trim(request, "reason"));
        request.setAttribute("evidence", RequestUtil.trim(request, "evidence"));
        if (itemId != null) {
            loadItem(request, itemId);
        }
        request.getRequestDispatcher("/admin/adjustmentCreate.jsp").forward(request, response);
    }

    private void loadItem(HttpServletRequest request, long itemId) {
        ServiceResult<ReconciliationItem> result = reconciliationService.getItem(itemId);
        if (result.isSuccess()) {
            ReconciliationItem item = result.getData();
            request.setAttribute("item", item);
            if ("ACCOUNT".equals(item.getBusinessType())) {
                request.setAttribute("suggestedAccountNo", item.getBusinessId());
            }
        } else {
            request.setAttribute("error", result.getMessage());
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
