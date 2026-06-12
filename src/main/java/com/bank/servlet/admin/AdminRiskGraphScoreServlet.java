package com.bank.servlet.admin;

import com.bank.dto.AdminRiskGraphScorePage;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphScoreService;
import com.bank.service.impl.RiskGraphScoreServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AdminRiskGraphScoreServlet", urlPatterns = "/admin/risk/graph-scores")
public class AdminRiskGraphScoreServlet extends HttpServlet {
    private final RiskGraphScoreService riskGraphScoreService = new RiskGraphScoreServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String modelVersion = RequestUtil.trim(request, "modelVersion");
        String decision = RequestUtil.trim(request, "decision");
        String edgeType = RequestUtil.trim(request, "edgeType");
        String minScore = RequestUtil.trim(request, "minScore");
        String labelFraud = RequestUtil.trim(request, "labelFraud");

        ServiceResult<AdminRiskGraphScorePage> result =
                riskGraphScoreService.queryScores(modelVersion, decision, edgeType, minScore, labelFraud);
        if (result.isSuccess()) {
            request.setAttribute("scorePage", result.getData());
            modelVersion = result.getData().getSelectedModelVersion();
        } else {
            request.setAttribute("error", result.getMessage());
        }
        request.setAttribute("selectedModelVersion", modelVersion);
        request.setAttribute("selectedDecision", decision);
        request.setAttribute("selectedEdgeType", edgeType);
        request.setAttribute("selectedMinScore", minScore);
        request.setAttribute("selectedLabelFraud", labelFraud);
        request.setAttribute("activeNav", "risk-graph-scores");
        request.getRequestDispatcher("/admin/riskGraphScores.jsp").forward(request, response);
    }
}
