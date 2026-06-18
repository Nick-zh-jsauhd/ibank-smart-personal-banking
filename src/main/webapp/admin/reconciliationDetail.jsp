<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ReconciliationBatch,com.bank.bean.ReconciliationItem,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.util.List" %>
<%!
    private String statusName(String status) {
        if ("SUCCESS".equals(status)) return "通过";
        if ("HAS_EXCEPTION".equals(status)) return "有异常";
        if ("FAILED".equals(status)) return "失败";
        if ("RUNNING".equals(status)) return "执行中";
        if ("OPEN".equals(status)) return "待处理";
        if ("INVESTIGATING".equals(status)) return "处理中";
        if ("CONFIRMED_EXCEPTION".equals(status)) return "确认异常";
        if ("ACCEPTED".equals(status)) return "可接受差异";
        if ("FIXED".equals(status)) return "已修复";
        if ("CLOSED".equals(status)) return "已关闭";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("SUCCESS".equals(status)) return "status";
        if ("HAS_EXCEPTION".equals(status) || "FAILED".equals(status) || "CRITICAL".equals(status)) {
            return "direction direction-out";
        }
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

    private String businessTypeName(String businessType) {
        if ("DEPOSIT".equals(businessType)) return "存款";
        if ("WITHDRAW".equals(businessType)) return "取款";
        if ("TRANSFER_INNER".equals(businessType)) return "本行转账";
        if ("PAYMENT".equals(businessType)) return "生活缴费";
        if ("BUY_WEALTH".equals(businessType)) return "理财申购";
        if ("REDEEM_WEALTH".equals(businessType)) return "理财赎回";
        if ("ACCOUNT".equals(businessType)) return "账户";
        if ("VERIFY".equals(businessType)) return "系统核验";
        if ("RISK".equals(businessType)) return "风控";
        return checkTypeName(businessType);
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
    ReconciliationBatch batch = (ReconciliationBatch) request.getAttribute("batch");
    List<ReconciliationItem> items = (List<ReconciliationItem>) request.getAttribute("items");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>对账详情 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">对账详情</p>
        <h1>对账详情</h1>
        <p class="muted">查看某次对账批次的执行结果和异常明细。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (batch != null) { %>
        <section class="metric-grid">
            <article class="metric-card">
                <span>对账日期</span>
                <strong><%= batch.getReconDate() == null ? "" : batch.getReconDate().toString() %></strong>
            </article>
            <article class="metric-card">
                <span>状态</span>
                <strong><%= statusName(batch.getStatus()) %></strong>
            </article>
            <article class="metric-card">
                <span>检查项</span>
                <strong><%= batch.getTotalChecks() %></strong>
            </article>
            <article class="metric-card">
                <span>异常数</span>
                <strong><%= batch.getExceptionCount() %></strong>
            </article>
        </section>

        <section class="content-section filter-section">
            <dl class="detail-list">
                <div>
                    <dt>批次号</dt>
                    <dd>#<%= batch.getBatchId() %></dd>
                </div>
                <div>
                    <dt>发起人</dt>
                    <dd><%= HtmlUtil.escape(batch.getCreatedByUsername()) %></dd>
                </div>
                <div>
                    <dt>开始时间</dt>
                    <dd><%= timeText(batch.getStartedAt()) %></dd>
                </div>
                <div>
                    <dt>完成时间</dt>
                    <dd><%= timeText(batch.getFinishedAt()) %></dd>
                </div>
            </dl>
        </section>
    <% } %>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>异常明细</h2>
                <p class="section-note">没有明细表示本批次未发现当前规则覆盖范围内的异常。</p>
            </div>
            <div class="section-actions">
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation">返回批次列表</a>
                <% if (adminUser.hasPermission("RECONCILIATION_ITEM_VIEW")) { %><a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation/items">异常处理中心</a><% } %>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>类型</th>
                    <th>严重度</th>
                    <th>业务类型</th>
                    <th>业务标识</th>
                    <th>期望</th>
                    <th>实际</th>
                    <th>说明</th>
                    <th>状态</th>
                    <th>发现时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (items == null || items.isEmpty()) { %>
                    <tr><td colspan="10" class="empty">本批次未发现异常</td></tr>
                <% } else {
                    for (ReconciliationItem item : items) {
                %>
                    <tr>
                        <td><%= HtmlUtil.escape(checkTypeName(item.getCheckType())) %></td>
                        <td><span class="<%= statusClass(item.getSeverity()) %>"><%= severityName(item.getSeverity()) %></span></td>
                        <td><%= HtmlUtil.escape(businessTypeName(item.getBusinessType())) %></td>
                        <td><strong><%= HtmlUtil.escape(item.getBusinessId()) %></strong></td>
                        <td><%= HtmlUtil.escape(item.getExpectedValue()) %></td>
                        <td><%= HtmlUtil.escape(item.getActualValue()) %></td>
                        <td><%= HtmlUtil.escape(item.getDescription()) %></td>
                        <td><span class="tag"><%= statusName(item.getStatus()) %></span></td>
                        <td><%= timeText(item.getCreatedAt()) %></td>
                        <td>
                            <% if (adminUser.hasPermission("RECONCILIATION_ITEM_VIEW")) { %><a class="button secondary compact"
                               href="<%= request.getContextPath() %>/admin/reconciliation/item/detail?itemId=<%= item.getItemId() %>">处理</a><% } %>
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

