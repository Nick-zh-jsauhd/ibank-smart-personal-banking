package com.bank.servlet;

import com.bank.bean.Account;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.AccountService;
import com.bank.service.impl.AccountServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "AccountListServlet", urlPatterns = "/accounts")
public class AccountListServlet extends HttpServlet {
    private final AccountService accountService = new AccountServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<List<Account>> result = accountService.listAccounts(sessionUser.getCustomerId());
        if (result.isSuccess()) {
            request.setAttribute("accounts", result.getData());
        } else {
            request.setAttribute("error", result.getMessage());
            request.setAttribute("accounts", Collections.emptyList());
        }
        request.getRequestDispatcher("/account/accountList.jsp").forward(request, response);
    }
}
