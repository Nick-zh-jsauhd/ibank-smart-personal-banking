package com.bank.dto;

public class RiskDecision {
    private String decision;
    private int riskScore;
    private String riskLevel;
    private String hitRules;
    private String reason;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getHitRules() {
        return hitRules;
    }

    public void setHitRules(String hitRules) {
        this.hitRules = hitRules;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isBlocked() {
        return "BLOCK".equals(decision);
    }
}
