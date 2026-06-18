<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminCustomerView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminCustomerView> customers = (List<AdminCustomerView>) request.getAttribute("customers");
    String error = (String) request.getAttribute("error");
    String keyword = (String) request.getAttribute("keyword");
    String riskLevel = (String) request.getAttribute("riskLevel");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>客户管理 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">客户运营</p>
        <h1>客户管理</h1>
        <p class="muted">按姓名、手机号、用户名或风险等级查询客户，进入详情可查看账户、流水、风控和通知。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/customers" class="filter-form">
            <label>
                <span>关键词</span>
                <input name="keyword" type="text" maxlength="80" value="<%= HtmlUtil.escape(keyword) %>">
            </label>
            <label>
                <span>风险等级</span>
                <select name="riskLevel">
                    <option value="">全部</option>
                    <% for (int i = 1; i <= 5; i++) {
                        String level = "C" + i;
                    %>
                        <option value="<%= level %>" <%= level.equals(riskLevel) ? "selected" : "" %>><%= level %></option>
                    <% } %>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>客户</th>
                    <th>用户名</th>
                    <th>手机号</th>
                    <th>风险等级</th>
                    <th>账户数</th>
                    <th>可用余额合计</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (customers == null || customers.isEmpty()) { %>
                    <tr><td colspan="8" class="empty">暂无客户数据</td></tr>
                <% } else {
                    for (AdminCustomerView customer : customers) {
                %>
                    <tr>
                        <td><%= HtmlUtil.escape(customer.getFullName()) %></td>
                        <td><%= HtmlUtil.escape(customer.getUsername()) %></td>
                        <td><%= HtmlUtil.escape(customer.getPhone()) %></td>
                        <td><span class="tag"><%= HtmlUtil.escape(customer.getRiskLevel()) %></span></td>
                        <td><%= customer.getAccountCount() %></td>
                        <td>¥ <%= moneyText(customer.getTotalAvailableBalance()) %></td>
                        <td><%= customer.getCreatedAt() == null ? "" : customer.getCreatedAt().toString().substring(0, 19) %></td>
                        <td>
                            <a class="button primary compact"
                               href="<%= request.getContextPath() %>/admin/customer/detail?customerId=<%= customer.getCustomerId() %>">详情</a>
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

