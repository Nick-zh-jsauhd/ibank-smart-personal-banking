package com.bank.servlet;

import com.bank.dto.BillPaymentView;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.TransactionService;
import com.bank.service.impl.TransactionServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "PaymentRecordServlet", urlPatterns = "/payment-records")
public class PaymentRecordServlet extends HttpServlet {
    private final TransactionService transactionService = new TransactionServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        String paymentType = RequestUtil.trim(request, "paymentType");
        ServiceResult<List<BillPaymentView>> result = transactionService.listBillPayments(
                sessionUser.getCustomerId(), paymentType);
        if (result.isSuccess()) {
            request.setAttribute("records", result.getData());
        } else {
            request.setAttribute("records", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedPaymentType", paymentType);
        request.getRequestDispatcher("/transaction/paymentRecords.jsp").forward(request, response);
    }
}
