<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminAuditLogView,com.bank.dto.AdminAuditOverview,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String operationName(String operationType) {
        if ("ADMIN_PERMISSION_DENIED".equals(operationType)) return "越权访问";
        if ("CREATE_ADMIN_USER".equals(operationType)) return "创建管理员";
        if ("UPDATE_ADMIN_USER".equals(operationType)) return "更新管理员";
        if ("UPDATE_RISK_RULE".equals(operationType)) return "修改风控规则";
        if ("UPDATE_WEALTH_PRODUCT".equals(operationType)) return "修改理财产品";
        if ("HANDLE_RISK_EVENT".equals(operationType)) return "处置风控事件";
        if ("CREATE_ADJUSTMENT".equals(operationType)) return "创建调账";
        if ("REVIEW_ADJUSTMENT".equals(operationType)) return "复核调账";
        if ("EXECUTE_ADJUSTMENT".equals(operationType)) return "执行调账";
        if ("HANDLE_ADMIN_ALERT".equals(operationType)) return "处理后台消息";
        if ("AUTO_SYNC_ADMIN_ALERT".equals(operationType)) return "自动同步消息";
        if ("RUN_RECONCILIATION".equals(operationType)) return "发起对账";
        return operationType == null ? "" : operationType;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminAuditOverview overview = (AdminAuditOverview) request.getAttribute("overview");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>审计中心 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Audit Center</p>
        <h1>审计中心</h1>
        <p class="muted">集中查看后台高风险操作、越权访问和登录安全事件。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (overview != null) { %>
        <section class="metric-grid">
            <article class="metric-card">
                <span>今日后台成功登录</span>
                <strong><%= overview.getTodayAdminLoginCount() %></strong>
            </article>
            <article class="metric-card">
                <span>今日登录失败</span>
                <strong><%= overview.getTodayLoginFailureCount() %></strong>
            </article>
            <article class="metric-card">
                <span>今日越权访问</span>
                <strong><%= overview.getTodayPermissionDeniedCount() %></strong>
            </article>
            <article class="metric-card">
                <span>今日高风险操作</span>
                <strong><%= overview.getTodayHighRiskOperationCount() %></strong>
            </article>
        </section>

        <section class="quick-actions">
            <a class="button primary" href="<%= request.getContextPath() %>/admin/audit/admin-logs?highRiskOnly=1">查看高风险操作</a>
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/audit/admin-logs?operationType=ADMIN_PERMISSION_DENIED">查看越权访问</a>
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/audit/login-logs">查看登录日志</a>
        </section>

        <section class="content-section filter-section">
            <div class="section-title">
                <div>
                    <h2>最近高风险操作</h2>
                    <p class="section-note">包含风控、理财、对账、调账、管理员权限和越权访问等敏感动作。</p>
                </div>
            </div>
            <div class="table-wrap">
                <table class="wide-table">
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>管理员</th>
                        <th>操作</th>
                        <th>目标</th>
                        <th>IP</th>
                        <th>详情</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (overview.getRecentHighRiskLogs().isEmpty()) { %>
                        <tr><td colspan="6" class="empty">暂无高风险操作</td></tr>
                    <% } else {
                        for (AdminAuditLogView log : overview.getRecentHighRiskLogs()) {
                    %>
                        <tr>
                            <td><%= timeText(log.getCreatedAt()) %></td>
                            <td><%= HtmlUtil.escape(log.getAdminUsername()) %></td>
                            <td><span class="tag"><%= HtmlUtil.escape(operationName(log.getOperationType())) %></span></td>
                            <td><%= HtmlUtil.escape(log.getTargetType()) %> #<%= HtmlUtil.escape(log.getTargetId()) %></td>
                            <td><%= HtmlUtil.escape(log.getIpAddress()) %></td>
                            <td><%= HtmlUtil.escape(log.getDetail()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>最近越权访问</h2>
                    <p class="section-note">越权记录用于排查权限配置、异常账号行为和后台误操作。</p>
                </div>
            </div>
            <div class="table-wrap">
                <table class="wide-table">
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>管理员</th>
                        <th>缺失权限</th>
                        <th>IP</th>
                        <th>详情</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (overview.getRecentPermissionDeniedLogs().isEmpty()) { %>
                        <tr><td colspan="5" class="empty">暂无越权访问</td></tr>
                    <% } else {
                        for (AdminAuditLogView log : overview.getRecentPermissionDeniedLogs()) {
                    %>
                        <tr>
                            <td><%= timeText(log.getCreatedAt()) %></td>
                            <td><%= HtmlUtil.escape(log.getAdminUsername()) %></td>
                            <td><span class="direction direction-out"><%= HtmlUtil.escape(log.getTargetId()) %></span></td>
                            <td><%= HtmlUtil.escape(log.getIpAddress()) %></td>
                            <td><%= HtmlUtil.escape(log.getDetail()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>
    <% } %>
</main>
</body>
</html>

