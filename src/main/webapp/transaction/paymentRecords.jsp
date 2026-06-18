<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.BillPaymentView,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String typeName(String paymentType) {
        if ("WATER".equals(paymentType)) return "水费";
        if ("ELECTRICITY".equals(paymentType)) return "电费";
        if ("GAS".equals(paymentType)) return "燃气费";
        if ("MOBILE".equals(paymentType)) return "话费";
        return paymentType == null ? "" : paymentType;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    List<BillPaymentView> records = (List<BillPaymentView>) request.getAttribute("records");
    String error = (String) request.getAttribute("error");
    String selectedPaymentType = (String) request.getAttribute("selectedPaymentType");
    BigDecimal totalAmount = BigDecimal.ZERO;
    String latestTime = "--";
    if (records != null) {
        for (BillPaymentView record : records) {
            if (record.getAmount() != null) {
                totalAmount = totalAmount.add(record.getAmount());
            }
        }
        if (!records.isEmpty() && records.get(0).getCreatedAt() != null) {
            latestTime = records.get(0).getCreatedAt().toString().substring(0, 19);
        }
    }
    request.setAttribute("activeNav", "transfer");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>缴费记录 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">缴费记录</p>
            <h1>缴费记录</h1>
            <p class="muted">展示生活缴费扩展业务详情；资金流水仍可在流水页统一查询。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/payment">办理缴费</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/transactions?txnType=PAYMENT">缴费流水</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>缴费记录帮你核对生活账单是否已经完成。</strong>
            <p>如果需要核对资金出账，请进入缴费流水；如果对账单机构或户号有疑问，可以发起服务工单。</p>
        </div>
    </div>

    <section class="metric-grid">
        <article class="metric-card">
            <span>记录数</span>
            <strong><%= records == null ? 0 : records.size() %></strong>
        </article>
        <article class="metric-card">
            <span>筛选类型</span>
            <strong><%= selectedPaymentType == null || selectedPaymentType.length() == 0 ? "全部" : HtmlUtil.escape(typeName(selectedPaymentType)) %></strong>
        </article>
        <article class="metric-card">
            <span>合计金额</span>
            <strong>¥ <%= moneyText(totalAmount) %></strong>
        </article>
        <article class="metric-card">
            <span>最近缴费</span>
            <strong><%= latestTime %></strong>
        </article>
    </section>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/payment-records" class="filter-form two-col">
            <label>
                <span>缴费类型</span>
                <select name="paymentType">
                    <option value="">全部</option>
                    <option value="WATER" <%= "WATER".equals(selectedPaymentType) ? "selected" : "" %>>水费</option>
                    <option value="ELECTRICITY" <%= "ELECTRICITY".equals(selectedPaymentType) ? "selected" : "" %>>电费</option>
                    <option value="GAS" <%= "GAS".equals(selectedPaymentType) ? "selected" : "" %>>燃气费</option>
                    <option value="MOBILE" <%= "MOBILE".equals(selectedPaymentType) ? "selected" : "" %>>话费</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>最近缴费</h2>
                <p class="section-note">缴费记录包含机构、户号、账期和业务状态。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>时间</th>
                    <th>交易编号</th>
                    <th>付款账户</th>
                    <th>类型</th>
                    <th>机构</th>
                    <th>户号/手机号</th>
                    <th>账期</th>
                    <th>金额</th>
                    <th>状态</th>
                </tr>
                </thead>
                <tbody>
                <% if (records == null || records.isEmpty()) { %>
                    <tr><td colspan="9" class="empty">当前还没有缴费记录，完成水电燃气或话费缴费后会在这里展示。</td></tr>
                <% } else {
                    for (BillPaymentView record : records) {
                %>
                    <tr>
                        <td><%= record.getCreatedAt() == null ? "" : record.getCreatedAt().toString().substring(0, 19) %></td>
                        <td><%= HtmlUtil.escape(record.getTransactionNo()) %></td>
                        <td><%= HtmlUtil.escape(record.getAccountNo()) %></td>
                        <td><%= HtmlUtil.escape(typeName(record.getPaymentType())) %></td>
                        <td><%= HtmlUtil.escape(record.getInstitutionName()) %></td>
                        <td><%= HtmlUtil.escape(record.getPayerNo()) %></td>
                        <td><%= HtmlUtil.escape(record.getBillingMonth()) %></td>
                        <td>¥ <%= moneyText(record.getAmount()) %></td>
                        <td><span class="status"><%= HtmlUtil.escape(record.getStatus()) %></span></td>
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
