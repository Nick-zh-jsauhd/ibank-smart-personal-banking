<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ServiceTicket,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private boolean selected(String current, String expected) {
        return expected != null && expected.equals(current);
    }

    private String typeName(String type) {
        if ("TRANSACTION_DISPUTE".equals(type)) return "交易争议";
        if ("RISK_APPEAL".equals(type)) return "风控申诉";
        if ("ACCOUNT_SERVICE".equals(type)) return "账户服务";
        if ("WEALTH_SERVICE".equals(type)) return "理财服务";
        if ("ADJUSTMENT_INQUIRY".equals(type)) return "调账咨询";
        if ("GENERAL".equals(type)) return "其他问题";
        return type == null ? "" : type;
    }

    private String statusName(String status) {
        if ("SUBMITTED".equals(status)) return "待受理";
        if ("ACCEPTED".equals(status)) return "已受理";
        if ("INVESTIGATING".equals(status)) return "调查中";
        if ("WAITING_CUSTOMER".equals(status)) return "待客户补充";
        if ("RESOLVED".equals(status)) return "已处理";
        if ("CLOSED".equals(status)) return "已关闭";
        if ("REOPENED".equals(status)) return "客户重开";
        if ("REJECTED".equals(status)) return "不予受理";
        return status == null ? "" : status;
    }

    private String priorityName(String priority) {
        if ("URGENT".equals(priority)) return "紧急";
        if ("HIGH".equals(priority)) return "高";
        if ("NORMAL".equals(priority)) return "普通";
        if ("LOW".equals(priority)) return "低";
        return priority == null ? "" : priority;
    }

    private String roleName(String roleCode) {
        if ("CUSTOMER_OPERATOR".equals(roleCode)) return "客户运营";
        if ("RISK_OPERATOR".equals(roleCode)) return "风控运营";
        if ("RISK_MANAGER".equals(roleCode)) return "风控管理";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "账务运营";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "账务复核";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "理财产品";
        if ("SUPER_ADMIN".equals(roleCode)) return "超级管理员";
        return roleCode == null ? "" : roleCode;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<ServiceTicket> tickets = (List<ServiceTicket>) request.getAttribute("tickets");
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    String selectedTicketType = (String) request.getAttribute("selectedTicketType");
    String selectedPriority = (String) request.getAttribute("selectedPriority");
    String keyword = (String) request.getAttribute("keyword");
    Boolean assignedToMe = (Boolean) request.getAttribute("assignedToMe");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>服务工单 - iBank Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Ticket Center</p>
        <h1>客户服务工单</h1>
        <p class="muted">统一承接客户服务请求、交易争议和风控申诉，并通过状态流转完成处理闭环。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form class="filter-form" method="get" action="<%= request.getContextPath() %>/admin/tickets">
            <label>
                <span>状态</span>
                <select name="status">
                    <option value="ALL">全部</option>
                    <option value="SUBMITTED" <%= selected(selectedStatus, "SUBMITTED") ? "selected" : "" %>>待受理</option>
                    <option value="REOPENED" <%= selected(selectedStatus, "REOPENED") ? "selected" : "" %>>客户重开</option>
                    <option value="ACCEPTED" <%= selected(selectedStatus, "ACCEPTED") ? "selected" : "" %>>已受理</option>
                    <option value="INVESTIGATING" <%= selected(selectedStatus, "INVESTIGATING") ? "selected" : "" %>>调查中</option>
                    <option value="WAITING_CUSTOMER" <%= selected(selectedStatus, "WAITING_CUSTOMER") ? "selected" : "" %>>待客户补充</option>
                    <option value="RESOLVED" <%= selected(selectedStatus, "RESOLVED") ? "selected" : "" %>>已处理</option>
                    <option value="CLOSED" <%= selected(selectedStatus, "CLOSED") ? "selected" : "" %>>已关闭</option>
                    <option value="REJECTED" <%= selected(selectedStatus, "REJECTED") ? "selected" : "" %>>不予受理</option>
                </select>
            </label>
            <label>
                <span>类型</span>
                <select name="ticketType">
                    <option value="ALL">全部</option>
                    <option value="TRANSACTION_DISPUTE" <%= selected(selectedTicketType, "TRANSACTION_DISPUTE") ? "selected" : "" %>>交易争议</option>
                    <option value="RISK_APPEAL" <%= selected(selectedTicketType, "RISK_APPEAL") ? "selected" : "" %>>风控申诉</option>
                    <option value="ACCOUNT_SERVICE" <%= selected(selectedTicketType, "ACCOUNT_SERVICE") ? "selected" : "" %>>账户服务</option>
                    <option value="WEALTH_SERVICE" <%= selected(selectedTicketType, "WEALTH_SERVICE") ? "selected" : "" %>>理财服务</option>
                    <option value="ADJUSTMENT_INQUIRY" <%= selected(selectedTicketType, "ADJUSTMENT_INQUIRY") ? "selected" : "" %>>调账咨询</option>
                    <option value="GENERAL" <%= selected(selectedTicketType, "GENERAL") ? "selected" : "" %>>其他问题</option>
                </select>
            </label>
            <label>
                <span>优先级</span>
                <select name="priority">
                    <option value="ALL">全部</option>
                    <option value="URGENT" <%= selected(selectedPriority, "URGENT") ? "selected" : "" %>>紧急</option>
                    <option value="HIGH" <%= selected(selectedPriority, "HIGH") ? "selected" : "" %>>高</option>
                    <option value="NORMAL" <%= selected(selectedPriority, "NORMAL") ? "selected" : "" %>>普通</option>
                    <option value="LOW" <%= selected(selectedPriority, "LOW") ? "selected" : "" %>>低</option>
                </select>
            </label>
            <label>
                <span>关键词</span>
                <input name="keyword" value="<%= HtmlUtil.escape(keyword) %>" placeholder="编号、客户、手机号、业务号">
            </label>
            <label class="checkbox-line">
                <input type="checkbox" name="assignedToMe" value="1" <%= assignedToMe != null && assignedToMe.booleanValue() ? "checked" : "" %>>
                <span>只看分配给我的工单</span>
            </label>
            <div class="filter-actions">
                <button class="button primary compact" type="submit">筛选</button>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/tickets">重置</a>
            </div>
        </form>
    </section>

    <section class="content-section">
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>更新时间</th>
                    <th>工单编号</th>
                    <th>客户</th>
                    <th>类型</th>
                    <th>优先级</th>
                    <th>状态</th>
                    <th>责任角色</th>
                    <th>处理人</th>
                    <th>标题</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (tickets == null || tickets.isEmpty()) { %>
                    <tr><td colspan="10" class="empty">暂无可见服务工单</td></tr>
                <% } else {
                    for (ServiceTicket ticket : tickets) {
                %>
                    <tr>
                        <td><%= timeText(ticket.getUpdatedAt()) %></td>
                        <td><%= HtmlUtil.escape(ticket.getTicketNo()) %></td>
                        <td>
                            <strong><%= HtmlUtil.escape(ticket.getCustomerName()) %></strong>
                            <p class="cell-note"><%= HtmlUtil.escape(ticket.getPhone()) %></p>
                        </td>
                        <td><%= HtmlUtil.escape(typeName(ticket.getTicketType())) %></td>
                        <td><span class="tag severity-<%= "URGENT".equals(ticket.getPriority()) ? "critical" : ("HIGH".equals(ticket.getPriority()) ? "high" : "info") %>"><%= HtmlUtil.escape(priorityName(ticket.getPriority())) %></span></td>
                        <td><span class="status"><%= HtmlUtil.escape(statusName(ticket.getStatus())) %></span></td>
                        <td><%= HtmlUtil.escape(roleName(ticket.getAssignedRoleCode())) %></td>
                        <td><%= HtmlUtil.escape(ticket.getAssignedAdminUsername()) %></td>
                        <td><%= HtmlUtil.escape(ticket.getTitle()) %></td>
                        <td><a class="button secondary compact" href="<%= request.getContextPath() %>/admin/ticket/detail?ticketId=<%= ticket.getTicketId() %>">详情</a></td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>
</main>
</body>
</html>
