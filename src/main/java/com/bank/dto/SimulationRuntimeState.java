package com.bank.dto;

import java.sql.Timestamp;

public class SimulationRuntimeState {
    private boolean running;
    private boolean paused;
    private Long runId;
    private String runCode;
    private String scenarioCode;
    private String scenarioName;
    private String speed;
    private int targetEventCount;
    private int eventsPerTick;
    private int intervalMillis;
    private int generatedEventCount;
    private int riskEventCount;
    private int failureEventCount;
    private String message;
    private Timestamp startedAt;
    private Timestamp lastTickAt;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

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

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public int getTargetEventCount() {
        return targetEventCount;
    }

    public void setTargetEventCount(int targetEventCount) {
        this.targetEventCount = targetEventCount;
    }

    public int getEventsPerTick() {
        return eventsPerTick;
    }

    public void setEventsPerTick(int eventsPerTick) {
        this.eventsPerTick = eventsPerTick;
    }

    public int getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(int intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public int getGeneratedEventCount() {
        return generatedEventCount;
    }

    public void setGeneratedEventCount(int generatedEventCount) {
        this.generatedEventCount = generatedEventCount;
    }

    public int getRiskEventCount() {
        return riskEventCount;
    }

    public void setRiskEventCount(int riskEventCount) {
        this.riskEventCount = riskEventCount;
    }

    public int getFailureEventCount() {
        return failureEventCount;
    }

    public void setFailureEventCount(int failureEventCount) {
        this.failureEventCount = failureEventCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getLastTickAt() {
        return lastTickAt;
    }

    public void setLastTickAt(Timestamp lastTickAt) {
        this.lastTickAt = lastTickAt;
    }
}
