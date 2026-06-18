<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ReconciliationBatch,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.time.LocalDate,java.util.List" %>
<%!
    private String statusName(String status) {
        if ("SUCCESS".equals(status)) return "通过";
        if ("HAS_EXCEPTION".equals(status)) return "有异常";
        if ("FAILED".equals(status)) return "失败";
        if ("RUNNING".equals(status)) return "执行中";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("SUCCESS".equals(status)) return "status";
        if ("HAS_EXCEPTION".equals(status) || "FAILED".equals(status)) return "direction direction-out";
        return "tag";
    }

    private String timeText(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<ReconciliationBatch> batches = (List<ReconciliationBatch>) request.getAttribute("batches");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    String selectedDate = (String) request.getAttribute("selectedDate");
    String today = LocalDate.now().toString();
    if (selectedDate == null || selectedDate.length() == 0) {
        selectedDate = today;
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>账务对账 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">账务对账</p>
        <h1>账务对账</h1>
        <p class="muted">按日期检查交易、账户流水、理财持仓和风控拦截之间的一致性，辅助管理员做日终核算。</p>
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
                <h2>发起对账</h2>
                <p class="section-note">当前版本会执行五类检查：成功交易与流水、账户当前余额与最后流水、理财申购、理财赎回、风控拦截未落成功交易。</p>
            </div>
            <% if (adminUser.hasPermission("RECONCILIATION_ITEM_VIEW")) { %><div class="section-actions">
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation/items">异常处理中心</a>
            </div><% } %>
        </div>
        <form method="post" action="<%= request.getContextPath() %>/admin/reconciliation" class="filter-form two-col">
            <label>
                <span>对账日期</span>
                <input type="date" name="reconDate" value="<%= HtmlUtil.escape(selectedDate) %>" max="<%= today %>">
            </label>
            <% if (adminUser.hasPermission("RECONCILIATION_RUN")) { %>
            <button class="button primary" type="submit">开始对账</button>
            <% } %>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>最近批次</h2>
                <p class="section-note">异常批次需要进入详情查看具体交易号、账户号和核对差异。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>批次号</th>
                    <th>对账日期</th>
                    <th>状态</th>
                    <th>检查项</th>
                    <th>异常数</th>
                    <th>发起人</th>
                    <th>开始时间</th>
                    <th>完成时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (batches == null || batches.isEmpty()) { %>
                    <tr><td colspan="9" class="empty">暂无对账批次</td></tr>
                <% } else {
                    for (ReconciliationBatch batch : batches) {
                %>
                    <tr>
                        <td><strong>#<%= batch.getBatchId() %></strong></td>
                        <td><%= batch.getReconDate() == null ? "" : batch.getReconDate().toString() %></td>
                        <td><span class="<%= statusClass(batch.getStatus()) %>"><%= statusName(batch.getStatus()) %></span></td>
                        <td><%= batch.getTotalChecks() %></td>
                        <td><%= batch.getExceptionCount() %></td>
                        <td><%= HtmlUtil.escape(batch.getCreatedByUsername()) %></td>
                        <td><%= timeText(batch.getStartedAt()) %></td>
                        <td><%= timeText(batch.getFinishedAt()) %></td>
                        <td>
                            <a class="button secondary compact"
                               href="<%= request.getContextPath() %>/admin/reconciliation/detail?batchId=<%= batch.getBatchId() %>">详情</a>
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

