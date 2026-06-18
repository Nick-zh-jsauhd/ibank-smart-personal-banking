<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.WealthOrderView,com.bank.dto.WealthSettlementSummary,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String statusName(String status) {
        if ("BUY_PENDING_CONFIRM".equals(status)) return "申购待确认";
        if ("BUY_CONFIRMED".equals(status)) return "申购已确认";
        if ("REDEEM_PENDING_ARRIVAL".equals(status)) return "赎回待到账";
        if ("REDEEM_COMPLETED".equals(status)) return "赎回已到账";
        if ("FAILED".equals(status)) return "处理失败";
        return status == null ? "" : status;
    }

    private String orderTypeName(String type) {
        if ("BUY".equals(type)) return "申购";
        if ("REDEEM".equals(type)) return "赎回";
        return type == null ? "" : type;
    }

    private String selected(String left, String right) {
        return right.equals(left == null ? "" : left) ? "selected" : "";
    }
%>
<%
    WealthSettlementSummary summary = (WealthSettlementSummary) request.getAttribute("summary");
    List<WealthOrderView> orders = (List<WealthOrderView>) request.getAttribute("orders");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    request.setAttribute("activeNav", "wealth-settlement");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>理财清算 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout layout-wide">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">理财清算</p>
            <h1>理财清算中心</h1>
            <p class="muted">把客户申购、资金冻结、持仓确认、赎回申请和到账落账拆成可追踪的清算流程。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/wealth/products">产品管理</a>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation">账务对账</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>
    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>

    <section class="metric-grid">
        <article class="metric-card warning">
            <span>申购待确认</span>
            <strong><%= summary == null ? 0 : summary.getPendingBuyOrders() %></strong>
            <p class="section-note">确认后解冻资金并生成持仓</p>
        </article>
        <article class="metric-card warning">
            <span>赎回待到账</span>
            <strong><%= summary == null ? 0 : summary.getRedeemingOrders() %></strong>
            <p class="section-note">到账后生成入账流水</p>
        </article>
        <article class="metric-card">
            <span>本次确认申购</span>
            <strong><%= summary == null ? 0 : summary.getConfirmedBuyOrders() %></strong>
        </article>
        <article class="metric-card">
            <span>本次赎回到账</span>
            <strong><%= summary == null ? 0 : summary.getSettledRedeemOrders() %></strong>
        </article>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>清算动作</h2>
                <p class="section-note">演示版由管理员手动触发；真实系统通常由日终批处理或清算任务自动执行。</p>
            </div>
        </div>
        <div class="action-grid">
            <form method="post" action="<%= request.getContextPath() %>/admin/wealth/settlement" class="action-card">
                <input type="hidden" name="action" value="confirmBuy">
                <strong>确认申购</strong>
                <span>把待确认申购单转为持仓，并生成理财申购出账流水。</span>
                <button class="button primary compact" type="submit">执行确认</button>
            </form>
            <form method="post" action="<%= request.getContextPath() %>/admin/wealth/settlement" class="action-card">
                <input type="hidden" name="action" value="settleRedeem">
                <strong>赎回到账</strong>
                <span>把赎回待到账订单清算入账，并生成理财赎回入账流水。</span>
                <button class="button primary compact" type="submit">执行到账</button>
            </form>
            <form method="post" action="<%= request.getContextPath() %>/admin/wealth/settlement" class="action-card">
                <input type="hidden" name="action" value="all">
                <strong>一键清算</strong>
                <span>连续处理申购确认和赎回到账，适合演示完整业务闭环。</span>
                <button class="button danger compact" type="submit">执行全部</button>
            </form>
        </div>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>理财订单队列</h2>
                <p class="section-note">订单状态解释资金处于哪里：冻结、持仓、赎回中或已到账。</p>
            </div>
            <form method="get" action="<%= request.getContextPath() %>/admin/wealth/settlement" class="filter-inline">
                <select name="status">
                    <option value="">全部状态</option>
                    <option value="BUY_PENDING_CONFIRM" <%= selected(selectedStatus, "BUY_PENDING_CONFIRM") %>>申购待确认</option>
                    <option value="BUY_CONFIRMED" <%= selected(selectedStatus, "BUY_CONFIRMED") %>>申购已确认</option>
                    <option value="REDEEM_PENDING_ARRIVAL" <%= selected(selectedStatus, "REDEEM_PENDING_ARRIVAL") %>>赎回待到账</option>
                    <option value="REDEEM_COMPLETED" <%= selected(selectedStatus, "REDEEM_COMPLETED") %>>赎回已到账</option>
                </select>
                <button class="button secondary compact" type="submit">筛选</button>
            </form>
        </div>
        <div class="table-wrap">
            <table class="wide-table admin-product-table">
                <thead>
                <tr>
                    <th>订单号</th>
                    <th>客户</th>
                    <th>产品</th>
                    <th>类型</th>
                    <th>金额</th>
                    <th>确认/到账金额</th>
                    <th>收益</th>
                    <th>状态</th>
                    <th>提交时间</th>
                    <th>起息/到期</th>
                    <th>预计到账</th>
                </tr>
                </thead>
                <tbody>
                <% if (orders == null || orders.isEmpty()) { %>
                    <tr><td colspan="11" class="empty">暂无理财清算订单</td></tr>
                <% } else {
                    for (WealthOrderView order : orders) {
                %>
                    <tr>
                        <td><strong><%= HtmlUtil.escape(order.getOrderNo()) %></strong></td>
                        <td><%= HtmlUtil.escape(order.getFullName()) %><p class="cell-note"><%= HtmlUtil.escape(order.getAccountNo()) %></p></td>
                        <td><%= HtmlUtil.escape(order.getProductName()) %><p class="cell-note"><%= HtmlUtil.escape(order.getProductCode()) %></p></td>
                        <td><%= HtmlUtil.escape(orderTypeName(order.getOrderType())) %></td>
                        <td>¥ <%= moneyText(order.getAmount()) %></td>
                        <td>¥ <%= moneyText(order.getConfirmedAmount()) %></td>
                        <td>¥ <%= moneyText(order.getIncomeAmount()) %></td>
                        <td><span class="status"><%= HtmlUtil.escape(statusName(order.getStatus())) %></span></td>
                        <td><%= order.getSubmitTime() == null ? "" : order.getSubmitTime().toString().substring(0, 19) %></td>
                        <td>
                            <%= order.getValueDate() == null ? "-" : order.getValueDate().toString() %>
                            /
                            <%= order.getMaturityDate() == null ? "-" : order.getMaturityDate().toString() %>
                        </td>
                        <td><%= order.getExpectedArrivalDate() == null ? "-" : order.getExpectedArrivalDate().toString() %></td>
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
