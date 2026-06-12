package com.bank.servlet;

import com.bank.bean.Customer;
import com.bank.bean.RiskAssessment;
import com.bank.dao.CustomerDao;
import com.bank.dao.impl.CustomerDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.dto.SessionUser;
import com.bank.service.RiskAssessmentService;
import com.bank.service.impl.RiskAssessmentServiceImpl;
import com.bank.util.DBUtil;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet(name = "RiskAssessmentServlet", urlPatterns = "/risk/assessment")
public class RiskAssessmentServlet extends HttpServlet {
    private final RiskAssessmentService riskAssessmentService = new RiskAssessmentServiceImpl();
    private final CustomerDao customerDao = new CustomerDaoImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        consumeFlash(request);
        loadPageData(request);
        request.getRequestDispatcher("/risk/assessment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<RiskAssessment> result = riskAssessmentService.submitAssessment(
                sessionUser.getCustomerId(), sessionUser.getUserId(),
                RequestUtil.trim(request, "age"),
                RequestUtil.trim(request, "experience"),
                RequestUtil.trim(request, "lossTolerance"),
                RequestUtil.trim(request, "goal"),
                RequestUtil.trim(request, "horizon"),
                RequestUtil.trim(request, "liquidity"),
                RequestUtil.trim(request, "knowledge"));

        if (result.isSuccess()) {
            request.getSession().setAttribute("success", result.getMessage());
            response.sendRedirect(request.getContextPath() + "/risk/assessment");
            return;
        }
        request.setAttribute("error", result.getMessage());
        loadPageData(request);
        request.getRequestDispatcher("/risk/assessment.jsp").forward(request, response);
    }

    private void loadPageData(HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("loginUser");
        ServiceResult<RiskAssessment> latestResult =
                riskAssessmentService.getLatestAssessment(sessionUser.getCustomerId());
        if (latestResult.isSuccess()) {
            request.setAttribute("latestAssessment", latestResult.getData());
        }
        try (Connection connection = DBUtil.getConnection()) {
            Customer customer = customerDao.findById(connection, sessionUser.getCustomerId());
            request.setAttribute("customer", customer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String success = (String) session.getAttribute("success");
        if (success != null) {
            request.setAttribute("success", success);
            session.removeAttribute("success");
        }
    }
}
