<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.AdminAlert,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String statusName(String status) {
        if ("NEW".equals(status)) return "待确认";
        if ("ACKED".equals(status)) return "处理中";
        if ("RESOLVED".equals(status)) return "已解决";
        if ("CLOSED".equals(status)) return "已关闭";
        return status == null ? "" : status;
    }

    private String severityName(String severity) {
        if ("CRITICAL".equals(severity)) return "严重";
        if ("HIGH".equals(severity)) return "高";
        if ("WARNING".equals(severity)) return "预警";
        if ("INFO".equals(severity)) return "提示";
        return severity == null ? "" : severity;
    }

    private String roleName(String roleCode) {
        if ("RISK_OPERATOR".equals(roleCode)) return "风控运营";
        if ("RISK_MANAGER".equals(roleCode)) return "风控规则管理员";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "账务运营";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "账务复核员";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "理财产品管理员";
        if ("SUPER_ADMIN".equals(roleCode)) return "超级管理员";
        return roleCode == null || roleCode.length() == 0 ? "全局" : roleCode;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminAlert> alerts = (List<AdminAlert>) request.getAttribute("alerts");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    String selectedSeverity = (String) request.getAttribute("selectedSeverity");
    Integer openAlertCount = (Integer) request.getAttribute("openAlertCount");
    if (openAlertCount == null) {
        openAlertCount = 0;
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>消息中心 - iBank Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Message Center</p>
        <h1>后台消息中心</h1>
        <p class="muted">集中处理风控拦截、对账异常、调账复核等需要管理端跟进的业务告警和待办。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <div class="section-title">
            <div>
                <h2>待处理消息</h2>
                <p class="section-note">当前可见待处理 <%= openAlertCount %> 条，默认最多展示最近 200 条。</p>
            </div>
        </div>
        <form class="filter-form" method="get" action="<%= request.getContextPath() %>/admin/messages">
            <label>
                <span>状态</span>
                <select name="status">
                    <option value="ALL">全部</option>
                    <option value="NEW" <%= "NEW".equals(selectedStatus) ? "selected" : "" %>>待确认</option>
                    <option value="ACKED" <%= "ACKED".equals(selectedStatus) ? "selected" : "" %>>处理中</option>
                    <option value="RESOLVED" <%= "RESOLVED".equals(selectedStatus) ? "selected" : "" %>>已解决</option>
                    <option value="CLOSED" <%= "CLOSED".equals(selectedStatus) ? "selected" : "" %>>已关闭</option>
                </select>
            </label>
            <label>
                <span>级别</span>
                <select name="severity">
                    <option value="ALL">全部</option>
                    <option value="CRITICAL" <%= "CRITICAL".equals(selectedSeverity) ? "selected" : "" %>>严重</option>
                    <option value="HIGH" <%= "HIGH".equals(selectedSeverity) ? "selected" : "" %>>高</option>
                    <option value="WARNING" <%= "WARNING".equals(selectedSeverity) ? "selected" : "" %>>预警</option>
                    <option value="INFO" <%= "INFO".equals(selectedSeverity) ? "selected" : "" %>>提示</option>
                </select>
            </label>
            <div class="filter-actions">
                <button class="button primary compact" type="submit">筛选</button>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/messages">重置</a>
            </div>
        </form>
    </section>

    <section class="content-section">
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>时间</th>
                    <th>级别</th>
                    <th>状态</th>
                    <th>标题</th>
                    <th>责任角色</th>
                    <th>关联对象</th>
                    <th>处理人</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (alerts == null || alerts.isEmpty()) { %>
                    <tr><td colspan="8" class="empty">暂无后台消息</td></tr>
                <% } else {
                    for (AdminAlert alert : alerts) {
                %>
                    <tr>
                        <td><%= timeText(alert.getCreatedAt()) %></td>
                        <td><span class="tag severity-<%= HtmlUtil.escape(alert.getSeverity() == null ? "INFO" : alert.getSeverity().toLowerCase()) %>"><%= HtmlUtil.escape(severityName(alert.getSeverity())) %></span></td>
                        <td><span class="status"><%= HtmlUtil.escape(statusName(alert.getStatus())) %></span></td>
                        <td>
                            <strong><%= HtmlUtil.escape(alert.getTitle()) %></strong>
                            <p class="cell-note"><%= HtmlUtil.escape(alert.getContent()) %></p>
                        </td>
                        <td><%= HtmlUtil.escape(roleName(alert.getResponsibleRoleCode())) %></td>
                        <td><%= HtmlUtil.escape(alert.getTargetType()) %> #<%= HtmlUtil.escape(alert.getTargetId()) %></td>
                        <td><%= HtmlUtil.escape(alert.getHandledByAdminUsername() == null ? alert.getAssignedAdminUsername() : alert.getHandledByAdminUsername()) %></td>
                        <td><a class="button secondary compact" href="<%= request.getContextPath() %>/admin/message/detail?alertId=<%= alert.getAlertId() %>">详情</a></td>
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
