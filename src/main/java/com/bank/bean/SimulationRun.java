package com.bank.bean;

import java.sql.Timestamp;

public class SimulationRun {
    private Long runId;
    private String runCode;
    private String scenarioCode;
    private String scenarioName;
    private String status;
    private String speed;
    private Integer requestedEventCount;
    private Integer successEventCount;
    private Integer failureEventCount;
    private Integer riskEventCount;
    private Long adminUserId;
    private String configJson;
    private String summary;
    private String errorMessage;
    private Timestamp startedAt;
    private Timestamp completedAt;

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getRunCode() {
        return runCode;
    }

    public void setRunCode(String runCode) {
        this.runCode = runCode;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public Integer getRequestedEventCount() {
        return requestedEventCount;
    }

    public void setRequestedEventCount(Integer requestedEventCount) {
        this.requestedEventCount = requestedEventCount;
    }

    public Integer getSuccessEventCount() {
        return successEventCount;
    }

    public void setSuccessEventCount(Integer successEventCount) {
        this.successEventCount = successEventCount;
    }

    public Integer getFailureEventCount() {
        return failureEventCount;
    }

    public void setFailureEventCount(Integer failureEventCount) {
        this.failureEventCount = failureEventCount;
    }

    public Integer getRiskEventCount() {
        return riskEventCount;
    }

    public void setRiskEventCount(Integer riskEventCount) {
        this.riskEventCount = riskEventCount;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
}
