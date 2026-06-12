package com.bank.servlet.admin;

import com.bank.bean.AdjustmentActionLog;
import com.bank.bean.AdjustmentRequest;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.AdjustmentService;
import com.bank.service.impl.AdjustmentServiceImpl;
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

@WebServlet(name = "AdminAdjustmentDetailServlet", urlPatterns = "/admin/adjustment/detail")
public class AdminAdjustmentDetailServlet extends HttpServlet {
    private final AdjustmentService adjustmentService = new AdjustmentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        Long adjustmentId = parseLong(RequestUtil.trim(request, "adjustmentId"));
        if (adjustmentId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/adjustments");
            return;
        }
        loadDetail(request, adjustmentId);
        request.getRequestDispatcher("/admin/adjustmentDetail.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long adjustmentId = parseLong(RequestUtil.trim(request, "adjustmentId"));
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        String action = RequestUtil.trim(request, "action");

        ServiceResult<Void> result;
        if (adjustmentId == null) {
            result = ServiceResult.failure("调账申请不存在。");
        } else if ("review".equals(action)) {
            result = adjustmentService.review(adjustmentId, adminUser.getUserId(),
                    RequestUtil.trim(request, "decision"),
                    RequestUtil.trim(request, "note"),
                    RequestUtil.clientIp(request));
        } else if ("execute".equals(action)) {
            result = adjustmentService.execute(adjustmentId, adminUser.getUserId(), RequestUtil.clientIp(request));
        } else {
            result = ServiceResult.failure("请选择正确的操作。");
        }

        HttpSession session = request.getSession();
        session.setAttribute(result.isSuccess() ? "success" : "error", result.getMessage());
        if (adjustmentId == null) {
            response.sendRedirect(request.getContextPath() + "/admin/adjustments");
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/adjustment/detail?adjustmentId="
                    + adjustmentId);
        }
    }

    private void loadDetail(HttpServletRequest request, long adjustmentId) {
        ServiceResult<AdjustmentRequest> requestResult = adjustmentService.getRequest(adjustmentId);
        if (requestResult.isSuccess()) {
            request.setAttribute("adjustment", requestResult.getData());
        } else {
            request.setAttribute("error", requestResult.getMessage());
        }

        ServiceResult<List<AdjustmentActionLog>> logResult = adjustmentService.listActionLogs(adjustmentId);
        if (logResult.isSuccess()) {
            request.setAttribute("actionLogs", logResult.getData());
        } else {
            request.setAttribute("actionLogs", Collections.emptyList());
            request.setAttribute("error", logResult.getMessage());
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
