<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminLoginLogView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String roleName(String role) {
        if ("ADMIN".equals(role)) return "后台管理员";
        if ("CUSTOMER".equals(role)) return "客户";
        return role == null ? "未知" : role;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminLoginLogView> logs = (List<AdminLoginLogView>) request.getAttribute("logs");
    String error = (String) request.getAttribute("error");
    String selectedIdentity = (String) request.getAttribute("selectedIdentity");
    String selectedUserRole = (String) request.getAttribute("selectedUserRole");
    String selectedSuccess = (String) request.getAttribute("selectedSuccess");
    String selectedStartDate = (String) request.getAttribute("selectedStartDate");
    String selectedEndDate = (String) request.getAttribute("selectedEndDate");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>登录日志 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">登录日志</p>
        <h1>登录日志</h1>
        <p class="muted">查看后台和客户侧登录行为、失败原因和来源 IP。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/audit/login-logs" class="filter-form">
            <label>
                <span>账号 / IP</span>
                <input name="identity" value="<%= HtmlUtil.escape(selectedIdentity) %>" placeholder="登录账号、用户名或IP">
            </label>
            <label>
                <span>用户类型</span>
                <select name="userRole">
                    <option value="">全部</option>
                    <option value="ADMIN" <%= "ADMIN".equals(selectedUserRole) ? "selected" : "" %>>后台管理员</option>
                    <option value="CUSTOMER" <%= "CUSTOMER".equals(selectedUserRole) ? "selected" : "" %>>客户</option>
                </select>
            </label>
            <label>
                <span>登录结果</span>
                <select name="success">
                    <option value="">全部</option>
                    <option value="SUCCESS" <%= "SUCCESS".equals(selectedSuccess) ? "selected" : "" %>>成功</option>
                    <option value="FAILURE" <%= "FAILURE".equals(selectedSuccess) ? "selected" : "" %>>失败</option>
                </select>
            </label>
            <label>
                <span>开始日期</span>
                <input type="date" name="startDate" value="<%= HtmlUtil.escape(selectedStartDate) %>">
            </label>
            <label>
                <span>结束日期</span>
                <input type="date" name="endDate" value="<%= HtmlUtil.escape(selectedEndDate) %>">
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>登录记录</h2>
                <p class="section-note">默认最多展示最近 200 条记录。</p>
            </div>
            <div class="section-actions">
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/audit">返回审计中心</a>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/audit/admin-logs">操作日志</a>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>时间</th>
                    <th>登录标识</th>
                    <th>绑定用户</th>
                    <th>类型</th>
                    <th>结果</th>
                    <th>失败原因</th>
                    <th>IP</th>
                    <th>User-Agent</th>
                </tr>
                </thead>
                <tbody>
                <% if (logs == null || logs.isEmpty()) { %>
                    <tr><td colspan="8" class="empty">暂无登录日志</td></tr>
                <% } else {
                    for (AdminLoginLogView log : logs) {
                %>
                    <tr>
                        <td><%= timeText(log.getCreatedAt()) %></td>
                        <td><strong><%= HtmlUtil.escape(log.getLoginIdentity()) %></strong></td>
                        <td><%= log.getUsername() == null ? "未绑定" : HtmlUtil.escape(log.getUsername()) %></td>
                        <td><span class="tag"><%= HtmlUtil.escape(roleName(log.getUserRole())) %></span></td>
                        <td>
                            <% if (log.isSuccess()) { %>
                                <span class="status">成功</span>
                            <% } else { %>
                                <span class="direction direction-out">失败</span>
                            <% } %>
                        </td>
                        <td><%= HtmlUtil.escape(log.getFailureReason()) %></td>
                        <td><%= HtmlUtil.escape(log.getIpAddress()) %></td>
                        <td><%= HtmlUtil.escape(log.getUserAgent()) %></td>
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

