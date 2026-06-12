package com.bank.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminRiskGraphNeighborhood {
    private long centerEdgeId;
    private String modelVersion;
    private List<AdminRiskGraphNodeView> nodes = new ArrayList<AdminRiskGraphNodeView>();
    private List<AdminRiskGraphEdgeView> edges = new ArrayList<AdminRiskGraphEdgeView>();
    private int fraudEdgeCount;
    private int blockEdgeCount;
    private int reviewEdgeCount;

    public long getCenterEdgeId() {
        return centerEdgeId;
    }

    public void setCenterEdgeId(long centerEdgeId) {
        this.centerEdgeId = centerEdgeId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public List<AdminRiskGraphNodeView> getNodes() {
        return nodes;
    }

    public void setNodes(List<AdminRiskGraphNodeView> nodes) {
        this.nodes = nodes;
    }

    public List<AdminRiskGraphEdgeView> getEdges() {
        return edges;
    }

    public void setEdges(List<AdminRiskGraphEdgeView> edges) {
        this.edges = edges;
    }

    public int getFraudEdgeCount() {
        return fraudEdgeCount;
    }

    public void setFraudEdgeCount(int fraudEdgeCount) {
        this.fraudEdgeCount = fraudEdgeCount;
    }

    public int getBlockEdgeCount() {
        return blockEdgeCount;
    }

    public void setBlockEdgeCount(int blockEdgeCount) {
        this.blockEdgeCount = blockEdgeCount;
    }

    public int getReviewEdgeCount() {
        return reviewEdgeCount;
    }

    public void setReviewEdgeCount(int reviewEdgeCount) {
        this.reviewEdgeCount = reviewEdgeCount;
    }
}
