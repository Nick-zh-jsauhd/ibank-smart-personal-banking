package com.bank.servlet;

import com.bank.bean.WealthProduct;
import com.bank.dto.ServiceResult;
import com.bank.service.WealthService;
import com.bank.service.impl.WealthServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "WealthProductServlet", urlPatterns = "/wealth/products")
public class WealthProductServlet extends HttpServlet {
    private final WealthService wealthService = new WealthServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServiceResult<List<WealthProduct>> result = wealthService.listProducts();
        if (result.isSuccess()) {
            request.setAttribute("products", result.getData());
        } else {
            request.setAttribute("products", Collections.emptyList());
            request.setAttribute("error", result.getMessage());
        }
        request.getRequestDispatcher("/wealth/productList.jsp").forward(request, response);
    }
}
