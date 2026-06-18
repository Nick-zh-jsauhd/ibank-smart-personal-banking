<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRiskEventView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String txnTypeName(String txnType) {
        if ("WITHDRAW".equals(txnType)) return "取款";
        if ("TRANSFER_INNER".equals(txnType)) return "本行转账";
        if ("PAYMENT".equals(txnType)) return "生活缴费";
        if ("BUY_WEALTH".equals(txnType)) return "理财申购";
        return txnType == null ? "" : txnType;
    }

    private String handleStatusName(String status) {
        if ("PENDING".equals(status)) return "待处理";
        if ("FOLLOW_UP".equals(status)) return "继续跟进";
        if ("HANDLED".equals(status)) return "已处理";
        return status == null ? "" : status;
    }

    private String handleResultName(String result) {
        if ("FALSE_POSITIVE".equals(result)) return "误报";
        if ("CONFIRMED_RISK".equals(result)) return "确认风险";
        if ("CUSTOMER_VERIFIED".equals(result)) return "客户已核实";
        if ("FOLLOW_UP".equals(result)) return "继续跟进";
        return result == null ? "" : result;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminRiskEventView> events = (List<AdminRiskEventView>) request.getAttribute("events");
    String error = (String) request.getAttribute("error");
    String selectedDecision = (String) request.getAttribute("selectedDecision");
    String selectedHandleStatus = (String) request.getAttribute("selectedHandleStatus");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>后台风控事件 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">风控事件</p>
        <h1>后台风控事件</h1>
        <p class="muted">全局查看客户资金流出交易触发的 WARN 和 BLOCK 风险事件，并进入处置闭环。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/risk/events" class="filter-form">
            <label>
                <span>决策</span>
                <select name="decision">
                    <option value="">全部</option>
                    <option value="WARN" <%= "WARN".equals(selectedDecision) ? "selected" : "" %>>WARN</option>
                    <option value="BLOCK" <%= "BLOCK".equals(selectedDecision) ? "selected" : "" %>>BLOCK</option>
                </select>
            </label>
            <label>
                <span>处理状态</span>
                <select name="handleStatus">
                    <option value="">全部</option>
                    <option value="PENDING" <%= "PENDING".equals(selectedHandleStatus) ? "selected" : "" %>>待处理</option>
                    <option value="FOLLOW_UP" <%= "FOLLOW_UP".equals(selectedHandleStatus) ? "selected" : "" %>>继续跟进</option>
                    <option value="HANDLED" <%= "HANDLED".equals(selectedHandleStatus) ? "selected" : "" %>>已处理</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>时间</th>
                    <th>客户</th>
                    <th>手机号</th>
                    <th>账户</th>
                    <th>交易类型</th>
                    <th>金额</th>
                    <th>决策</th>
                    <th>风险等级</th>
                    <th>评分</th>
                    <th>处理状态</th>
                    <th>处置结论</th>
                    <th>命中规则</th>
                    <th>原因</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (events == null || events.isEmpty()) { %>
                    <tr><td colspan="14" class="empty">暂无风险事件</td></tr>
                <% } else {
                    for (AdminRiskEventView event : events) {
                        boolean blocked = "BLOCK".equals(event.getDecision());
                        boolean pending = "PENDING".equals(event.getHandleStatus());
                %>
                    <tr>
                        <td><%= event.getCreatedAt() == null ? "" : event.getCreatedAt().toString().substring(0, 19) %></td>
                        <td><%= HtmlUtil.escape(event.getFullName()) %></td>
                        <td><%= HtmlUtil.escape(event.getPhone()) %></td>
                        <td><%= HtmlUtil.escape(event.getAccountNo()) %></td>
                        <td><%= HtmlUtil.escape(txnTypeName(event.getTxnType())) %></td>
                        <td>¥ <%= moneyText(event.getAmount()) %></td>
                        <td><span class="direction <%= blocked ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(event.getDecision()) %></span></td>
                        <td><span class="tag"><%= HtmlUtil.escape(event.getRiskLevel()) %></span></td>
                        <td><%= event.getRiskScore() %></td>
                        <td><span class="direction <%= pending ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(handleStatusName(event.getHandleStatus())) %></span></td>
                        <td><%= HtmlUtil.escape(handleResultName(event.getHandleResult())) %></td>
                        <td><%= HtmlUtil.escape(event.getHitRules()) %></td>
                        <td><%= HtmlUtil.escape(event.getReason()) %></td>
                        <td>
                            <a class="button primary compact"
                               href="<%= request.getContextPath() %>/admin/risk/event/detail?eventId=<%= event.getEventId() %>">处置</a>
                            <a class="button secondary compact"
                               href="<%= request.getContextPath() %>/admin/customer/detail?customerId=<%= event.getCustomerId() %>">客户详情</a>
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

