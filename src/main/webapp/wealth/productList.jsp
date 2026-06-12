<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.WealthProduct,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String rateText(BigDecimal rate) {
        if (rate == null) {
            return "";
        }
        return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String statusName(String status) {
        if ("ON_SALE".equals(status)) return "可申购";
        if ("OFF_SALE".equals(status)) return "已下架";
        return status == null ? "" : status;
    }

    private String productTypeName(String productType) {
        if ("FIXED_TERM".equals(productType)) return "封闭式";
        if ("OPEN".equals(productType)) return "开放式";
        if ("CASH".equals(productType)) return "现金管理";
        return productType == null ? "封闭式" : productType;
    }

    private String riskExplain(String riskLevel) {
        if ("R1".equals(riskLevel)) return "适合偏保守资金，优先关注流动性和本金波动控制。";
        if ("R2".equals(riskLevel)) return "适合稳健配置，收益目标和持有期限都要提前确认。";
        if ("R3".equals(riskLevel)) return "适合能接受一定净值波动的中长期资金。";
        return "申购前会按你的风险测评结果做匹配校验。";
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<WealthProduct> products = (List<WealthProduct>) request.getAttribute("products");
    String error = (String) request.getAttribute("error");
    request.setAttribute("activeNav", "wealth");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>理财产品 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Wealth</p>
            <h1>理财产品</h1>
            <p class="muted">按风险等级、期限、确认到账规则和参考年化选择适合的产品。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/risk/assessment">风险测评</a>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/wealth/holdings">我的持仓</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/transactions?txnType=BUY_WEALTH">申购流水</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="photo-band wealth">
        <div>
            <p class="eyebrow">Wealth Companion</p>
            <h2>理财不是只看收益，也要看期限、风险和这笔钱的用途。</h2>
            <p class="section-note">申购前系统会核对你的风险等级、账户余额、起购金额和支付密码。产品参考年化不代表保证收益。</p>
        </div>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>产品货架</h2>
                <p class="section-note">系统会在申购时校验客户风险等级、起购金额、余额和支付密码；确认后才形成持仓。</p>
            </div>
        </div>
        <% if (products == null || products.isEmpty()) { %>
            <div class="human-empty">
                <strong>当前没有可申购的理财产品。</strong>
                <p>产品上架后会显示适合人群、风险说明和购买前确认项；你也可以先完成风险测评，方便后续匹配。</p>
            </div>
        <% } else { %>
            <div class="product-card-grid">
                <% for (WealthProduct product : products) { %>
                    <article class="product-card warm-product">
                        <div>
                            <div class="section-title">
                                <div>
                                    <h2><%= HtmlUtil.escape(product.getProductName()) %></h2>
                                    <p class="section-note"><%= HtmlUtil.escape(product.getProductCode()) %></p>
                                </div>
                                <span class="tag"><%= HtmlUtil.escape(product.getRiskLevel()) %></span>
                            </div>
                            <p class="muted"><%= HtmlUtil.escape(product.getDescription()) %></p>
                            <div class="product-rate">
                                <span class="small-label">参考年化</span>
                                <strong><%= HtmlUtil.escape(rateText(product.getExpectedRate())) %></strong>
                            </div>
                            <div class="product-meta-grid">
                                <div><span class="small-label">类型</span><strong><%= HtmlUtil.escape(productTypeName(product.getProductType())) %></strong></div>
                                <div><span class="small-label">期限</span><strong><%= product.getPeriodDays() %> 天</strong></div>
                                <div><span class="small-label">起购</span><strong>¥ <%= moneyText(product.getMinAmount()) %></strong></div>
                                <div><span class="small-label">上限</span><strong>¥ <%= moneyText(product.getMaxAmount()) %></strong></div>
                                <div><span class="small-label">确认</span><strong>T+<%= product.getConfirmDays() %></strong></div>
                                <div><span class="small-label">到账</span><strong>T+<%= product.getArrivalDays() %></strong></div>
                            </div>
                            <ul class="product-fit-list">
                                <li><strong>适合谁：</strong><%= HtmlUtil.escape(riskExplain(product.getRiskLevel())) %></li>
                                <li><strong>赎回规则：</strong><%= product.isAllowEarlyRedeem() ? "允许提前赎回，到账仍需清算。" : "封闭期内不可赎回，到期后可提交赎回。" %></li>
                                <li><strong>购买前确认：</strong>请确认这笔资金不是短期必用资金，并已理解产品期限和到账规则。</li>
                            </ul>
                        </div>
                        <a class="button wealth full" href="<%= request.getContextPath() %>/wealth/buy?productId=<%= product.getProductId() %>">申购产品</a>
                    </article>
                <% } %>
            </div>
        <% } %>
    </section>
</main>
</body>
</html>
