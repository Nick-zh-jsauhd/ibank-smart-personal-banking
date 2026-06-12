package com.bank.bean;

import java.sql.Timestamp;

public class ServiceTicket {
    private Long ticketId;
    private String ticketNo;
    private Long customerId;
    private Long userId;
    private String customerName;
    private String phone;
    private String ticketType;
    private String priority;
    private String status;
    private String title;
    private String description;
    private String relatedBusinessType;
    private String relatedBusinessId;
    private String assignedRoleCode;
    private Long assignedAdminUserId;
    private String assignedAdminUsername;
    private Timestamp acceptedAt;
    private Timestamp resolvedAt;
    private Timestamp closedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRelatedBusinessType() {
        return relatedBusinessType;
    }

    public void setRelatedBusinessType(String relatedBusinessType) {
        this.relatedBusinessType = relatedBusinessType;
    }

    public String getRelatedBusinessId() {
        return relatedBusinessId;
    }

    public void setRelatedBusinessId(String relatedBusinessId) {
        this.relatedBusinessId = relatedBusinessId;
    }

    public String getAssignedRoleCode() {
        return assignedRoleCode;
    }

    public void setAssignedRoleCode(String assignedRoleCode) {
        this.assignedRoleCode = assignedRoleCode;
    }

    public Long getAssignedAdminUserId() {
        return assignedAdminUserId;
    }

    public void setAssignedAdminUserId(Long assignedAdminUserId) {
        this.assignedAdminUserId = assignedAdminUserId;
    }

    public String getAssignedAdminUsername() {
        return assignedAdminUsername;
    }

    public void setAssignedAdminUsername(String assignedAdminUsername) {
        this.assignedAdminUsername = assignedAdminUsername;
    }

    public Timestamp getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Timestamp acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
