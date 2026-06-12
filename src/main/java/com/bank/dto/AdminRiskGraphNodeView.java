package com.bank.dto;

import java.math.BigDecimal;

public class AdminRiskGraphNodeView {
    private long graphNodeId;
    private String externalNodeId;
    private String nodeType;
    private String displayName;
    private String role = "neighbor";
    private int inDegree;
    private int outDegree;
    private int fraudInDegree;
    private int fraudOutDegree;
    private BigDecimal totalInAmount;
    private BigDecimal totalOutAmount;
    private int neighborhoodEdgeCount;
    private int neighborhoodRiskEdgeCount;

    public long getGraphNodeId() {
        return graphNodeId;
    }

    public void setGraphNodeId(long graphNodeId) {
        this.graphNodeId = graphNodeId;
    }

    public String getExternalNodeId() {
        return externalNodeId;
    }

    public void setExternalNodeId(String externalNodeId) {
        this.externalNodeId = externalNodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getInDegree() {
        return inDegree;
    }

    public void setInDegree(int inDegree) {
        this.inDegree = inDegree;
    }

    public int getOutDegree() {
        return outDegree;
    }

    public void setOutDegree(int outDegree) {
        this.outDegree = outDegree;
    }

    public int getFraudInDegree() {
        return fraudInDegree;
    }

    public void setFraudInDegree(int fraudInDegree) {
        this.fraudInDegree = fraudInDegree;
    }

    public int getFraudOutDegree() {
        return fraudOutDegree;
    }

    public void setFraudOutDegree(int fraudOutDegree) {
        this.fraudOutDegree = fraudOutDegree;
    }

    public BigDecimal getTotalInAmount() {
        return totalInAmount;
    }

    public void setTotalInAmount(BigDecimal totalInAmount) {
        this.totalInAmount = totalInAmount;
    }

    public BigDecimal getTotalOutAmount() {
        return totalOutAmount;
    }

    public void setTotalOutAmount(BigDecimal totalOutAmount) {
        this.totalOutAmount = totalOutAmount;
    }

    public int getNeighborhoodEdgeCount() {
        return neighborhoodEdgeCount;
    }

    public void setNeighborhoodEdgeCount(int neighborhoodEdgeCount) {
        this.neighborhoodEdgeCount = neighborhoodEdgeCount;
    }

    public int getNeighborhoodRiskEdgeCount() {
        return neighborhoodRiskEdgeCount;
    }

    public void setNeighborhoodRiskEdgeCount(int neighborhoodRiskEdgeCount) {
        this.neighborhoodRiskEdgeCount = neighborhoodRiskEdgeCount;
    }
}
