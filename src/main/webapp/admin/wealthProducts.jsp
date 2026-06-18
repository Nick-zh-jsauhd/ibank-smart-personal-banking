<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.WealthProduct,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String rateText(BigDecimal rate) {
        if (rate == null) {
            return "0.0000";
        }
        return rate.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<WealthProduct> products = (List<WealthProduct>) request.getAttribute("products");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>理财产品管理 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">理财产品</p>
        <h1>理财产品管理</h1>
        <p class="muted">维护理财产品名称、风险等级、收益率、期限、购买金额范围和上下架状态。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/wealth/products" class="filter-form two-col">
            <label>
                <span>产品状态</span>
                <select name="status">
                    <option value="">全部</option>
                    <option value="ON_SALE" <%= "ON_SALE".equals(selectedStatus) ? "selected" : "" %>>在售</option>
                    <option value="OFF_SALE" <%= "OFF_SALE".equals(selectedStatus) ? "selected" : "" %>>已下架</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>产品参数</h2>
                <p class="section-note">在售产品会出现在客户侧理财列表并允许申购；下架产品会隐藏并拒绝新的申购。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table admin-edit-table admin-product-table">
                <thead>
                <tr>
                    <th>产品编码</th>
                    <th>产品名称</th>
                    <th>风险</th>
                    <th>年化</th>
                    <th>期限</th>
                    <th>起购金额</th>
                    <th>单人上限</th>
                    <th>状态</th>
                    <th>产品说明</th>
                    <th>更新时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (products == null || products.isEmpty()) { %>
                    <tr><td colspan="11" class="empty">还没有理财产品，新增产品后会显示在这里。</td></tr>
                <% } else {
                    for (WealthProduct product : products) {
                        String formId = "wealthProductForm" + product.getProductId();
                        boolean onSale = "ON_SALE".equals(product.getStatus());
                %>
                    <tr>
                        <td>
                            <form id="<%= formId %>" method="post" action="<%= request.getContextPath() %>/admin/wealth/products">
                                <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                                <input type="hidden" name="filterStatus" value="<%= HtmlUtil.escape(selectedStatus) %>">
                            </form>
                            <strong><%= HtmlUtil.escape(product.getProductCode()) %></strong>
                        </td>
                        <td><input form="<%= formId %>" class="admin-name-input" name="productName" value="<%= HtmlUtil.escape(product.getProductName()) %>"></td>
                        <td>
                            <select form="<%= formId %>" class="admin-risk-select" name="riskLevel">
                                <option value="R1" <%= "R1".equals(product.getRiskLevel()) ? "selected" : "" %>>R1</option>
                                <option value="R2" <%= "R2".equals(product.getRiskLevel()) ? "selected" : "" %>>R2</option>
                                <option value="R3" <%= "R3".equals(product.getRiskLevel()) ? "selected" : "" %>>R3</option>
                                <option value="R4" <%= "R4".equals(product.getRiskLevel()) ? "selected" : "" %>>R4</option>
                                <option value="R5" <%= "R5".equals(product.getRiskLevel()) ? "selected" : "" %>>R5</option>
                            </select>
                        </td>
                        <td><input form="<%= formId %>" class="admin-rate-input" name="expectedRate" value="<%= rateText(product.getExpectedRate()) %>"></td>
                        <td><input form="<%= formId %>" class="admin-count-input" name="periodDays" value="<%= product.getPeriodDays() %>"></td>
                        <td><input form="<%= formId %>" class="admin-money-input" name="minAmount" value="<%= moneyText(product.getMinAmount()) %>"></td>
                        <td><input form="<%= formId %>" class="admin-money-input" name="maxAmount" value="<%= moneyText(product.getMaxAmount()) %>"></td>
                        <td>
                            <select form="<%= formId %>" class="admin-status-select" name="status">
                                <option value="ON_SALE" <%= onSale ? "selected" : "" %>>在售</option>
                                <option value="OFF_SALE" <%= onSale ? "" : "selected" %>>已下架</option>
                            </select>
                        </td>
                        <td><input form="<%= formId %>" class="admin-description-input" name="description" value="<%= HtmlUtil.escape(product.getDescription()) %>"></td>
                        <td><%= product.getUpdatedAt() == null ? "" : product.getUpdatedAt().toString().substring(0, 19) %></td>
                        <td>
                            <% if (adminUser.hasPermission("WEALTH_PRODUCT_UPDATE")) { %>
                            <button form="<%= formId %>" class="button primary compact" type="submit">保存</button>
                            <% } %>
                            <span class="direction <%= onSale ? "direction-in" : "direction-out" %>"><%= onSale ? "在售" : "已下架" %></span>
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

