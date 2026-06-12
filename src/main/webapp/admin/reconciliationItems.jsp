<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ReconciliationItem,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.util.List" %>
<%!
    private String statusName(String status) {
        if ("OPEN".equals(status)) return "待处理";
        if ("INVESTIGATING".equals(status)) return "处理中";
        if ("CONFIRMED_EXCEPTION".equals(status)) return "确认异常";
        if ("ACCEPTED".equals(status)) return "可接受差异";
        if ("FIXED".equals(status)) return "已修复";
        if ("CLOSED".equals(status)) return "已关闭";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("OPEN".equals(status) || "CONFIRMED_EXCEPTION".equals(status)) return "direction direction-out";
        if ("FIXED".equals(status) || "ACCEPTED".equals(status) || "CLOSED".equals(status)) return "status";
        return "tag";
    }

    private String checkTypeName(String checkType) {
        if ("TRANSACTION_LEDGER".equals(checkType)) return "交易流水";
        if ("ACCOUNT_BALANCE".equals(checkType)) return "账户余额";
        if ("WEALTH_BUY".equals(checkType)) return "理财申购";
        if ("WEALTH_REDEEM".equals(checkType)) return "理财赎回";
        if ("RISK_BLOCK".equals(checkType)) return "风控拦截";
        return checkType == null ? "" : checkType;
    }

    private String severityName(String severity) {
        if ("CRITICAL".equals(severity)) return "严重";
        if ("WARN".equals(severity)) return "警告";
        return severity == null ? "" : severity;
    }

    private String timeText(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<ReconciliationItem> items = (List<ReconciliationItem>) request.getAttribute("items");
    String error = (String) request.getAttribute("error");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    String selectedSeverity = (String) request.getAttribute("selectedSeverity");
    String selectedCheckType = (String) request.getAttribute("selectedCheckType");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>对账异常处理 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Reconciliation Exceptions</p>
        <h1>对账异常处理中心</h1>
        <p class="muted">集中查看、接手和关闭对账异常。当前版本只记录人工处置结论，不自动修改账户、流水或持仓。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/reconciliation/items" class="filter-form">
            <label>
                <span>处理状态</span>
                <select name="status">
                    <option value="">全部</option>
                    <option value="OPEN" <%= "OPEN".equals(selectedStatus) ? "selected" : "" %>>待处理</option>
                    <option value="INVESTIGATING" <%= "INVESTIGATING".equals(selectedStatus) ? "selected" : "" %>>处理中</option>
                    <option value="CONFIRMED_EXCEPTION" <%= "CONFIRMED_EXCEPTION".equals(selectedStatus) ? "selected" : "" %>>确认异常</option>
                    <option value="ACCEPTED" <%= "ACCEPTED".equals(selectedStatus) ? "selected" : "" %>>可接受差异</option>
                    <option value="FIXED" <%= "FIXED".equals(selectedStatus) ? "selected" : "" %>>已修复</option>
                    <option value="CLOSED" <%= "CLOSED".equals(selectedStatus) ? "selected" : "" %>>已关闭</option>
                </select>
            </label>
            <label>
                <span>严重度</span>
                <select name="severity">
                    <option value="">全部</option>
                    <option value="CRITICAL" <%= "CRITICAL".equals(selectedSeverity) ? "selected" : "" %>>严重</option>
                    <option value="WARN" <%= "WARN".equals(selectedSeverity) ? "selected" : "" %>>警告</option>
                </select>
            </label>
            <label>
                <span>检查类型</span>
                <select name="checkType">
                    <option value="">全部</option>
                    <option value="TRANSACTION_LEDGER" <%= "TRANSACTION_LEDGER".equals(selectedCheckType) ? "selected" : "" %>>交易流水</option>
                    <option value="ACCOUNT_BALANCE" <%= "ACCOUNT_BALANCE".equals(selectedCheckType) ? "selected" : "" %>>账户余额</option>
                    <option value="WEALTH_BUY" <%= "WEALTH_BUY".equals(selectedCheckType) ? "selected" : "" %>>理财申购</option>
                    <option value="WEALTH_REDEEM" <%= "WEALTH_REDEEM".equals(selectedCheckType) ? "selected" : "" %>>理财赎回</option>
                    <option value="RISK_BLOCK" <%= "RISK_BLOCK".equals(selectedCheckType) ? "selected" : "" %>>风控拦截</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>异常事项</h2>
                <p class="section-note">默认最多展示最近 200 条异常，进入详情页可以查看处置记录并更新状态。</p>
            </div>
            <div class="section-actions">
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation">返回对账批次</a>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table admin-product-table">
                <thead>
                <tr>
                    <th>异常号</th>
                    <th>对账日期</th>
                    <th>检查类型</th>
                    <th>严重度</th>
                    <th>业务类型</th>
                    <th>业务标识</th>
                    <th>期望</th>
                    <th>实际</th>
                    <th>状态</th>
                    <th>处理人</th>
                    <th>更新时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (items == null || items.isEmpty()) { %>
                    <tr><td colspan="12" class="empty">暂无符合条件的对账异常</td></tr>
                <% } else {
                    for (ReconciliationItem item : items) {
                %>
                    <tr>
                        <td><strong>#<%= item.getItemId() %></strong></td>
                        <td><%= item.getReconDate() == null ? "" : item.getReconDate().toString() %></td>
                        <td><%= HtmlUtil.escape(checkTypeName(item.getCheckType())) %></td>
                        <td><span class="<%= "CRITICAL".equals(item.getSeverity()) ? "direction direction-out" : "tag" %>"><%= severityName(item.getSeverity()) %></span></td>
                        <td><%= HtmlUtil.escape(item.getBusinessType()) %></td>
                        <td><strong><%= HtmlUtil.escape(item.getBusinessId()) %></strong></td>
                        <td><%= HtmlUtil.escape(item.getExpectedValue()) %></td>
                        <td><%= HtmlUtil.escape(item.getActualValue()) %></td>
                        <td><span class="<%= statusClass(item.getStatus()) %>"><%= statusName(item.getStatus()) %></span></td>
                        <td><%= item.getHandlerUsername() == null ? "未分配" : HtmlUtil.escape(item.getHandlerUsername()) %></td>
                        <td><%= timeText(item.getUpdatedAt()) %></td>
                        <td>
                            <a class="button secondary compact"
                               href="<%= request.getContextPath() %>/admin/reconciliation/item/detail?itemId=<%= item.getItemId() %>">处理</a>
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

