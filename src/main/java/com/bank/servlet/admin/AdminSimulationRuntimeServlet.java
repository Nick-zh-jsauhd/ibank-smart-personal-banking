package com.bank.servlet.admin;

import com.bank.bean.SimulationEvent;
import com.bank.bean.SimulationRun;
import com.bank.dto.AdminSessionUser;
import com.bank.dto.ServiceResult;
import com.bank.dto.SimulationDashboardView;
import com.bank.dto.SimulationRuntimeState;
import com.bank.service.SimulationService;
import com.bank.service.impl.SimulationRuntimeManager;
import com.bank.service.impl.SimulationServiceImpl;
import com.bank.util.RequestUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet(name = "AdminSimulationRuntimeServlet", urlPatterns = "/admin/simulation/runtime")
public class AdminSimulationRuntimeServlet extends HttpServlet {
    private final SimulationRuntimeManager runtimeManager = SimulationRuntimeManager.getInstance();
    private final SimulationService simulationService = new SimulationServiceImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        writeState(response, true, "OK");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AdminSessionUser adminUser = (AdminSessionUser) request.getSession().getAttribute("adminUser");
        String action = RequestUtil.trim(request, "action");
        ServiceResult<SimulationRuntimeState> result;
        if ("start".equals(action)) {
            result = runtimeManager.start(adminUser.getUserId(),
                    RequestUtil.trim(request, "scenarioCode"),
                    parseInt(RequestUtil.trim(request, "targetEventCount"), 120),
                    parseInt(RequestUtil.trim(request, "eventsPerTick"), 2),
                    RequestUtil.trim(request, "speed"),
                    RequestUtil.clientIp(request));
        } else if ("pause".equals(action)) {
            result = runtimeManager.pause();
        } else if ("resume".equals(action)) {
            result = runtimeManager.resume();
        } else if ("stop".equals(action)) {
            result = runtimeManager.stop(RequestUtil.clientIp(request));
        } else {
            result = ServiceResult.failure("未知的仿真控制动作");
        }
        writeState(response, result.isSuccess(), result.getMessage());
    }

    private void writeState(HttpServletResponse response, boolean success, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        SimulationRuntimeState runtimeState = runtimeManager.snapshot();
        Long selectedRunId = runtimeState.getRunId();
        ServiceResult<SimulationDashboardView> dashboardResult = simulationService.dashboard(selectedRunId);
        SimulationDashboardView view = dashboardResult.isSuccess() ? dashboardResult.getData() : null;

        StringBuilder builder = new StringBuilder();
        builder.append("{\"success\":").append(success)
                .append(",\"message\":\"").append(json(message)).append("\"")
                .append(",\"runtime\":").append(runtimeJson(runtimeState));
        if (view != null) {
            builder.append(",\"totals\":{")
                    .append("\"runs\":").append(view.getTotalRunCount()).append(',')
                    .append("\"events\":").append(view.getTotalEventCount()).append(',')
                    .append("\"businessSuccess\":").append(view.getTotalBusinessSuccessCount()).append(',')
                    .append("\"riskWarnings\":").append(view.getTotalRiskWarningCount())
                    .append("}");
            builder.append(",\"selectedRun\":").append(runJson(view.getSelectedRun()));
            builder.append(",\"events\":").append(eventsJson(view.getSelectedRunEvents()));
        } else {
            builder.append(",\"totals\":{\"runs\":0,\"events\":0,\"businessSuccess\":0,\"riskWarnings\":0}");
            builder.append(",\"selectedRun\":null,\"events\":[]");
        }
        builder.append("}");
        response.getWriter().write(builder.toString());
    }

    private String runtimeJson(SimulationRuntimeState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("{")
                .append("\"running\":").append(state.isRunning()).append(',')
                .append("\"paused\":").append(state.isPaused()).append(',')
                .append("\"runId\":").append(state.getRunId() == null ? "null" : state.getRunId()).append(',')
                .append("\"runCode\":\"").append(json(state.getRunCode())).append("\",")
                .append("\"scenarioCode\":\"").append(json(state.getScenarioCode())).append("\",")
                .append("\"scenarioName\":\"").append(json(state.getScenarioName())).append("\",")
                .append("\"speed\":\"").append(json(state.getSpeed())).append("\",")
                .append("\"targetEventCount\":").append(state.getTargetEventCount()).append(',')
                .append("\"eventsPerTick\":").append(state.getEventsPerTick()).append(',')
                .append("\"intervalMillis\":").append(state.getIntervalMillis()).append(',')
                .append("\"generatedEventCount\":").append(state.getGeneratedEventCount()).append(',')
                .append("\"riskEventCount\":").append(state.getRiskEventCount()).append(',')
                .append("\"failureEventCount\":").append(state.getFailureEventCount()).append(',')
                .append("\"message\":\"").append(json(state.getMessage())).append("\",")
                .append("\"startedAt\":\"").append(json(timeText(state.getStartedAt()))).append("\",")
                .append("\"lastTickAt\":\"").append(json(timeText(state.getLastTickAt()))).append("\"")
                .append("}");
        return builder.toString();
    }

    private String runJson(SimulationRun run) {
        if (run == null) {
            return "null";
        }
        return "{\"runId\":" + run.getRunId()
                + ",\"runCode\":\"" + json(run.getRunCode())
                + "\",\"scenarioName\":\"" + json(run.getScenarioName())
                + "\",\"status\":\"" + json(run.getStatus())
                + "\",\"requestedEventCount\":" + intValue(run.getRequestedEventCount())
                + ",\"successEventCount\":" + intValue(run.getSuccessEventCount())
                + ",\"failureEventCount\":" + intValue(run.getFailureEventCount())
                + ",\"riskEventCount\":" + intValue(run.getRiskEventCount())
                + ",\"summary\":\"" + json(run.getSummary())
                + "\",\"startedAt\":\"" + json(timeText(run.getStartedAt()))
                + "\"}";
    }

    private String eventsJson(List<SimulationEvent> events) {
        StringBuilder builder = new StringBuilder("[");
        if (events != null) {
            for (int i = 0; i < events.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                SimulationEvent event = events.get(i);
                builder.append("{")
                        .append("\"sequence\":").append(intValue(event.getEventSequence())).append(',')
                        .append("\"eventType\":\"").append(json(event.getEventType())).append("\",")
                        .append("\"businessType\":\"").append(json(event.getBusinessType())).append("\",")
                        .append("\"businessId\":\"").append(json(event.getBusinessId())).append("\",")
                        .append("\"customerName\":\"").append(json(event.getCustomerName())).append("\",")
                        .append("\"accountNo\":\"").append(json(maskAccount(event.getAccountNo()))).append("\",")
                        .append("\"amount\":\"").append(json(moneyText(event.getAmount()))).append("\",")
                        .append("\"status\":\"").append(json(event.getStatus())).append("\",")
                        .append("\"message\":\"").append(json(event.getMessage())).append("\",")
                        .append("\"createdAt\":\"").append(json(timeText(event.getCreatedAt()))).append("\"")
                        .append("}");
            }
        }
        builder.append(']');
        return builder.toString();
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() <= 8) {
            return accountNo;
        }
        return accountNo.substring(0, 4) + " **** " + accountNo.substring(accountNo.length() - 4);
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private String timeText(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return new SimpleDateFormat("HH:mm:ss").format(timestamp);
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
