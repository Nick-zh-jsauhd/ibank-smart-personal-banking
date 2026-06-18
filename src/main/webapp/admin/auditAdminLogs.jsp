<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminAuditLogView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String operationName(String operationType) {
        if ("ADMIN_LOGIN".equals(operationType)) return "后台登录";
        if ("ADMIN_LOGOUT".equals(operationType)) return "后台退出";
        if ("ADMIN_PERMISSION_DENIED".equals(operationType)) return "越权访问";
        if ("VIEW_CUSTOMER_DETAIL".equals(operationType)) return "查看客户详情";
        if ("ADJUST_CUSTOMER_RISK_LEVEL".equals(operationType)) return "调整客户风险";
        if ("HANDLE_RISK_EVENT".equals(operationType)) return "处置风控事件";
        if ("UPDATE_RISK_RULE".equals(operationType)) return "修改风控规则";
        if ("UPDATE_WEALTH_PRODUCT".equals(operationType)) return "修改理财产品";
        if ("RUN_RECONCILIATION".equals(operationType)) return "发起对账";
        if ("HANDLE_RECONCILIATION_ITEM".equals(operationType)) return "处理对账异常";
        if ("CREATE_ADJUSTMENT".equals(operationType)) return "创建调账";
        if ("REVIEW_ADJUSTMENT".equals(operationType)) return "复核调账";
        if ("EXECUTE_ADJUSTMENT".equals(operationType)) return "执行调账";
        if ("CREATE_ADMIN_USER".equals(operationType)) return "创建管理员";
        if ("UPDATE_ADMIN_USER".equals(operationType)) return "更新管理员";
        return operationType == null ? "" : operationType;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminAuditLogView> logs = (List<AdminAuditLogView>) request.getAttribute("logs");
    String error = (String) request.getAttribute("error");
    String selectedAdmin = (String) request.getAttribute("selectedAdmin");
    String selectedOperationType = (String) request.getAttribute("selectedOperationType");
    String selectedTargetType = (String) request.getAttribute("selectedTargetType");
    String selectedStartDate = (String) request.getAttribute("selectedStartDate");
    String selectedEndDate = (String) request.getAttribute("selectedEndDate");
    Boolean highRiskOnly = (Boolean) request.getAttribute("highRiskOnly");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>管理员操作日志 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">操作审计</p>
        <h1>管理员操作日志</h1>
        <p class="muted">查询后台敏感操作、越权访问和关键业务变更记录。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/audit/admin-logs" class="filter-form">
            <label>
                <span>管理员</span>
                <input name="admin" value="<%= HtmlUtil.escape(selectedAdmin) %>" placeholder="账号或用户ID">
            </label>
            <label>
                <span>操作类型</span>
                <input name="operationType" value="<%= HtmlUtil.escape(selectedOperationType) %>" placeholder="如 UPDATE_ADMIN_USER">
            </label>
            <label>
                <span>目标类型</span>
                <input name="targetType" value="<%= HtmlUtil.escape(selectedTargetType) %>" placeholder="如 ADMIN">
            </label>
            <label>
                <span>开始日期</span>
                <input type="date" name="startDate" value="<%= HtmlUtil.escape(selectedStartDate) %>">
            </label>
            <label>
                <span>结束日期</span>
                <input type="date" name="endDate" value="<%= HtmlUtil.escape(selectedEndDate) %>">
            </label>
            <label class="checkbox-line">
                <input type="checkbox" name="highRiskOnly" value="1" <%= Boolean.TRUE.equals(highRiskOnly) ? "checked" : "" %>>
                <span>只看高风险</span>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>操作记录</h2>
                <p class="section-note">默认最多展示最近 200 条记录。</p>
            </div>
            <div class="section-actions">
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/audit">返回审计中心</a>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/audit/login-logs">登录日志</a>
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
                    <th>风险</th>
                </tr>
                </thead>
                <tbody>
                <% if (logs == null || logs.isEmpty()) { %>
                    <tr><td colspan="7" class="empty">暂无操作日志</td></tr>
                <% } else {
                    for (AdminAuditLogView log : logs) {
                %>
                    <tr>
                        <td><%= timeText(log.getCreatedAt()) %></td>
                        <td><%= HtmlUtil.escape(log.getAdminUsername()) %></td>
                        <td><strong><%= HtmlUtil.escape(operationName(log.getOperationType())) %></strong><p class="cell-note"><%= HtmlUtil.escape(log.getOperationType()) %></p></td>
                        <td><%= HtmlUtil.escape(log.getTargetType()) %> #<%= HtmlUtil.escape(log.getTargetId()) %></td>
                        <td><%= HtmlUtil.escape(log.getIpAddress()) %></td>
                        <td><%= HtmlUtil.escape(log.getDetail()) %></td>
                        <td>
                            <% if (log.isHighRisk()) { %>
                                <span class="direction direction-out">高风险</span>
                            <% } else { %>
                                <span class="status">普通</span>
                            <% } %>
                        </td>
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

