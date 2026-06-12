package com.bank.servlet.admin;

import com.bank.dto.AdminRiskGraphReviewCaseView;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.service.RiskGraphReviewCaseService;
import com.bank.service.impl.RiskGraphReviewCaseServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;

@WebServlet(name = "AdminRiskGraphFeedbackExportServlet", urlPatterns = "/admin/risk/graph-feedback-export")
public class AdminRiskGraphFeedbackExportServlet extends HttpServlet {
    private final RiskGraphReviewCaseService reviewCaseService = new RiskGraphReviewCaseServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        if (adminUser == null) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return;
        }

        String modelVersion = RequestUtil.trim(request, "modelVersion");
        String reviewResult = RequestUtil.trim(request, "reviewResult");
        ServiceResult<List<AdminRiskGraphReviewCaseView>> result =
                reviewCaseService.exportFeedbackCases(modelVersion, reviewResult, adminUser.getUserId(),
                        RequestUtil.clientIp(request));
        if (!result.isSuccess()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result.getMessage());
            return;
        }

        List<AdminRiskGraphReviewCaseView> rows = result.getData();
        String fileModelVersion = rows == null || rows.isEmpty() ? safeFileToken(modelVersion)
                : safeFileToken(rows.get(0).getModelVersion());
        if (fileModelVersion.length() == 0) {
            fileModelVersion = "riskbrain";
        }
        String filename = "ibank-risk-feedback-" + fileModelVersion + ".csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20"));

        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writeHeader(writer);
        if (rows != null) {
            for (AdminRiskGraphReviewCaseView row : rows) {
                writeRow(writer, row);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void writeHeader(PrintWriter writer) {
        writer.println("case_id,graph_edge_id,model_version,feature_version,dataset_name,batch_code,source_row_no,"
                + "event_time,from_external_id,to_external_id,edge_type,currency,amount,"
                + "source_label_fraud,model_decision,business_decision,risk_score,risk_probability,"
                + "case_type,case_status,review_result,human_label,feedback_action,priority,"
                + "reason,review_note,reviewer,reviewed_at,reason_json");
    }

    private void writeRow(PrintWriter writer, AdminRiskGraphReviewCaseView row) {
        writer.println(csv(row.getCaseId())
                + "," + csv(row.getGraphEdgeId())
                + "," + csv(row.getModelVersion())
                + "," + csv(row.getFeatureVersion())
                + "," + csv(row.getDatasetName())
                + "," + csv(row.getBatchCode())
                + "," + csv(row.getSourceRowNo())
                + "," + csv(row.getEventTime())
                + "," + csv(row.getFromExternalId())
                + "," + csv(row.getToExternalId())
                + "," + csv(row.getEdgeType())
                + "," + csv(row.getCurrency())
                + "," + csv(row.getAmount())
                + "," + csv(row.isLabelFraud() ? "1" : "0")
                + "," + csv(row.getModelDecision())
                + "," + csv(row.getBusinessDecision())
                + "," + csv(row.getRiskScore())
                + "," + csv(row.getRiskProbability())
                + "," + csv(row.getCaseType())
                + "," + csv(row.getCaseStatus())
                + "," + csv(row.getReviewResult())
                + "," + csv(humanLabel(row.getReviewResult()))
                + "," + csv(feedbackAction(row.getReviewResult()))
                + "," + csv(row.getPriority())
                + "," + csv(row.getReason())
                + "," + csv(row.getReviewNote())
                + "," + csv(row.getReviewerUsername())
                + "," + csv(row.getReviewedAt())
                + "," + csv(row.getReasonJson()));
    }

    private String humanLabel(String reviewResult) {
        if ("CONFIRMED_RISK".equals(reviewResult)) {
            return "1";
        }
        if ("FALSE_POSITIVE".equals(reviewResult)) {
            return "0";
        }
        return "";
    }

    private String feedbackAction(String reviewResult) {
        if ("CONFIRMED_RISK".equals(reviewResult)) {
            return "POSITIVE_FEEDBACK";
        }
        if ("FALSE_POSITIVE".equals(reviewResult)) {
            return "NEGATIVE_FEEDBACK";
        }
        if ("NEED_MORE_DATA".equals(reviewResult)) {
            return "EVIDENCE_REQUIRED";
        }
        if ("IGNORE".equals(reviewResult)) {
            return "EXCLUDE_FROM_TRAINING";
        }
        return "";
    }

    private String safeFileToken(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String csv(long value) {
        return String.valueOf(value);
    }

    private String csv(int value) {
        return String.valueOf(value);
    }

    private String csv(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String csv(Timestamp value) {
        return value == null ? "" : csv(value.toString().substring(0, 19));
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace("\r", " ").replace("\n", " ");
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }
}
