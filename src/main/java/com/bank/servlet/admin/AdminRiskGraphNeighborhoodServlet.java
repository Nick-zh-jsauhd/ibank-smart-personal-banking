package com.bank.servlet.admin;

import com.bank.dto.AdminRiskGraphEdgeView;
import com.bank.dto.AdminRiskGraphNeighborhood;
import com.bank.dto.AdminRiskGraphNodeView;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphScoreService;
import com.bank.service.impl.RiskGraphScoreServiceImpl;
import com.bank.util.JsonUtil;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet(name = "AdminRiskGraphNeighborhoodServlet", urlPatterns = "/admin/risk/graph-neighborhood")
public class AdminRiskGraphNeighborhoodServlet extends HttpServlet {
    private final RiskGraphScoreService riskGraphScoreService = new RiskGraphScoreServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String graphEdgeId = RequestUtil.trim(request, "graphEdgeId");
        String modelVersion = RequestUtil.trim(request, "modelVersion");
        ServiceResult<AdminRiskGraphNeighborhood> result =
                riskGraphScoreService.queryNeighborhood(graphEdgeId, modelVersion);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        if (!result.isSuccess()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":" + JsonUtil.quote(result.getMessage()) + "}");
            return;
        }
        response.getWriter().write(toJson(result.getData()));
    }

    private String toJson(AdminRiskGraphNeighborhood graph) {
        StringBuilder json = new StringBuilder(16384);
        json.append("{\"success\":true");
        json.append(",\"centerEdgeId\":").append(graph.getCenterEdgeId());
        json.append(",\"modelVersion\":").append(JsonUtil.quote(graph.getModelVersion()));
        json.append(",\"nodeCount\":").append(graph.getNodes().size());
        json.append(",\"edgeCount\":").append(graph.getEdges().size());
        json.append(",\"fraudEdgeCount\":").append(graph.getFraudEdgeCount());
        json.append(",\"blockEdgeCount\":").append(graph.getBlockEdgeCount());
        json.append(",\"reviewEdgeCount\":").append(graph.getReviewEdgeCount());
        json.append(",\"nodes\":[");
        for (int i = 0; i < graph.getNodes().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            appendNode(json, graph.getNodes().get(i));
        }
        json.append("],\"edges\":[");
        for (int i = 0; i < graph.getEdges().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            appendEdge(json, graph.getEdges().get(i));
        }
        json.append("]}");
        return json.toString();
    }

    private void appendNode(StringBuilder json, AdminRiskGraphNodeView node) {
        String label = node.getDisplayName() == null || node.getDisplayName().trim().length() == 0
                ? compact(node.getExternalNodeId()) : node.getDisplayName();
        int degree = node.getInDegree() + node.getOutDegree();
        String classes = "role-" + node.getRole();
        if (node.getNeighborhoodRiskEdgeCount() > 0) {
            classes += " risk-node";
        }
        json.append("{\"group\":\"nodes\",\"classes\":").append(JsonUtil.quote(classes)).append(",\"data\":{");
        json.append("\"id\":").append(JsonUtil.quote(nodeId(node.getGraphNodeId())));
        json.append(",\"label\":").append(JsonUtil.quote(label));
        json.append(",\"externalId\":").append(JsonUtil.quote(node.getExternalNodeId()));
        json.append(",\"nodeType\":").append(JsonUtil.quote(node.getNodeType()));
        json.append(",\"role\":").append(JsonUtil.quote(node.getRole()));
        json.append(",\"degree\":").append(degree);
        json.append(",\"inDegree\":").append(node.getInDegree());
        json.append(",\"outDegree\":").append(node.getOutDegree());
        json.append(",\"fraudDegree\":").append(node.getFraudInDegree() + node.getFraudOutDegree());
        json.append(",\"neighborhoodEdges\":").append(node.getNeighborhoodEdgeCount());
        json.append(",\"neighborhoodRiskEdges\":").append(node.getNeighborhoodRiskEdgeCount());
        json.append(",\"totalInAmount\":").append(number(node.getTotalInAmount()));
        json.append(",\"totalOutAmount\":").append(number(node.getTotalOutAmount()));
        json.append("}}");
    }

    private void appendEdge(StringBuilder json, AdminRiskGraphEdgeView edge) {
        String decision = edge.getDecision() == null ? "UNSCORED" : edge.getDecision();
        StringBuilder classes = new StringBuilder();
        classes.append("decision-").append(decision.toLowerCase());
        if (edge.isLabelConflict()) {
            classes.append(" label-conflict-edge");
        }
        if (edge.isCenterEdge()) {
            classes.append(" center-edge");
        }
        if (edge.isLabelFraud()) {
            classes.append(" fraud-edge");
        }
        json.append("{\"group\":\"edges\",\"classes\":").append(JsonUtil.quote(classes.toString())).append(",\"data\":{");
        json.append("\"id\":").append(JsonUtil.quote(edgeId(edge.getGraphEdgeId())));
        json.append(",\"source\":").append(JsonUtil.quote(nodeId(edge.getFromNodeId())));
        json.append(",\"target\":").append(JsonUtil.quote(nodeId(edge.getToNodeId())));
        json.append(",\"label\":").append(JsonUtil.quote(decision + " " + edge.getRiskScore()));
        json.append(",\"graphEdgeId\":").append(edge.getGraphEdgeId());
        json.append(",\"sourceRowNo\":").append(edge.getSourceRowNo());
        json.append(",\"batchCode\":").append(JsonUtil.quote(edge.getBatchCode()));
        json.append(",\"edgeType\":").append(JsonUtil.quote(edge.getEdgeType()));
        json.append(",\"amount\":").append(number(edge.getAmount()));
        json.append(",\"currency\":").append(JsonUtil.quote(edge.getCurrency()));
        json.append(",\"eventTime\":").append(JsonUtil.quote(edge.getEventTime() == null ? "" : edge.getEventTime().toString()));
        json.append(",\"labelFraud\":").append(edge.isLabelFraud());
        json.append(",\"modelVersion\":").append(JsonUtil.quote(edge.getModelVersion()));
        json.append(",\"riskScore\":").append(edge.getRiskScore());
        json.append(",\"riskProbability\":").append(number(edge.getRiskProbability()));
        json.append(",\"decision\":").append(JsonUtil.quote(decision));
        json.append(",\"businessDecision\":").append(JsonUtil.quote(edge.getBusinessDecision()));
        json.append(",\"businessDecisionLabel\":").append(JsonUtil.quote(edge.getBusinessDecisionLabel()));
        json.append(",\"businessDecisionReason\":").append(JsonUtil.quote(edge.getBusinessDecisionReason()));
        json.append(",\"businessDecisionSeverity\":").append(JsonUtil.quote(edge.getBusinessDecisionSeverity()));
        json.append(",\"labelConflict\":").append(edge.isLabelConflict());
        json.append(",\"reasonJson\":").append(JsonUtil.quote(edge.getReasonJson()));
        json.append(",\"center\":").append(edge.isCenterEdge());
        json.append("}}");
    }

    private String nodeId(long graphNodeId) {
        return "n" + graphNodeId;
    }

    private String edgeId(long graphEdgeId) {
        return "e" + graphEdgeId;
    }

    private String number(BigDecimal value) {
        return value == null ? "null" : value.stripTrailingZeros().toPlainString();
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 18) {
            return value;
        }
        return value.substring(0, 9) + "..." + value.substring(value.length() - 6);
    }
}
