package com.bank.bean;

import java.sql.Timestamp;

public class RiskAssessment {
    private Long assessmentId;
    private Long customerId;
    private int totalScore;
    private String riskLevel;
    private String answersJson;
    private String status;
    private Timestamp effectiveFrom;
    private Timestamp effectiveUntil;
    private Timestamp createdAt;

    public Long getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Long assessmentId) {
        this.assessmentId = assessmentId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public void setAnswersJson(String answersJson) {
        this.answersJson = answersJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Timestamp effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Timestamp getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(Timestamp effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
