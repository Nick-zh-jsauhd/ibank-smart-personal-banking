<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.bean.WealthProduct,com.bank.dto.WealthPurchasePreview,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode" %>
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

    private String matchText(String matchResult) {
        if ("EDGE_MATCH".equals(matchResult)) {
            return "临界匹配";
        }
        if ("MATCH".equals(matchResult)) {
            return "匹配";
        }
        return matchResult == null ? "" : matchResult;
    }
%>
<%
    WealthPurchasePreview preview = (WealthPurchasePreview) request.getAttribute("preview");
    String error = (String) request.getAttribute("error");
    WealthProduct product = preview == null ? null : preview.getProduct();
    Account account = preview == null ? null : preview.getAccount();
    request.setAttribute("activeNav", "wealth");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>申购确认 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">申购确认</p>
            <h1>申购确认与风险揭示</h1>
            <p class="muted">确认产品风险、付款账户和申购金额，理财产品不同于存款，参考收益不代表实际收益。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/wealth/products">返回产品列表</a>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (preview == null || product == null || account == null) { %>
        <section class="content-section">
            <p class="empty">申购确认信息无效，请返回产品列表重新发起。</p>
            <div class="action-row">
                <a class="button primary" href="<%= request.getContextPath() %>/wealth/products">返回产品列表</a>
            </div>
        </section>
    <% } else { %>
        <section class="task-layout">
            <article class="task-panel">
                <div class="section-title">
                    <div>
                        <h2>风险揭示确认</h2>
                        <p class="section-note">勾选确认项并输入支付密码后，系统会冻结申购资金并生成待确认订单。</p>
                    </div>
                    <span class="tag"><%= HtmlUtil.escape(matchText(preview.getMatchResult())) %></span>
                </div>

                <% if ("EDGE_MATCH".equals(preview.getMatchResult())) { %>
                    <div class="alert danger">该产品风险等级已达到您当前可承受等级上限，请确认您已充分理解产品风险。</div>
                <% } %>

                <form method="post" action="<%= request.getContextPath() %>/wealth/submit" class="form">
                    <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                    <input type="hidden" name="accountId" value="<%= account.getAccountId() %>">
                    <input type="hidden" name="amount" value="<%= moneyText(preview.getAmount()) %>">
                    <label class="checkbox-line">
                        <input name="productDisclosure" type="checkbox" value="1">
                        <span>我已阅读并理解产品说明、风险等级、期限和申购金额限制。</span>
                    </label>
                    <label class="checkbox-line">
                        <input name="nonDepositDisclosure" type="checkbox" value="1">
                        <span>我了解理财产品不等同于银行存款，产品存在本金和收益波动风险。</span>
                    </label>
                    <label class="checkbox-line">
                        <input name="yieldDisclosure" type="checkbox" value="1">
                        <span>我了解参考年化收益率不代表实际收益，实际收益以产品运作为准。</span>
                    </label>
                    <label class="checkbox-line">
                        <input name="accountConfirm" type="checkbox" value="1">
                        <span>我确认使用账户 <%= HtmlUtil.escape(account.getAccountNo()) %> 申购，金额 ¥ <%= moneyText(preview.getAmount()) %>。</span>
                    </label>
                    <label>
                        <span>支付密码</span>
                        <input name="payPassword" type="password" required maxlength="6" pattern="\d{6}" inputmode="numeric">
                    </label>
                    <div class="action-row">
                        <a class="button secondary" href="<%= request.getContextPath() %>/wealth/buy?productId=<%= product.getProductId() %>">返回修改</a>
                        <button class="button primary" type="submit">确认申购并冻结资金</button>
                    </div>
                </form>
            </article>

            <aside class="task-side">
                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2><%= HtmlUtil.escape(product.getProductName()) %></h2>
                            <p class="section-note"><%= HtmlUtil.escape(product.getProductCode()) %>，风险揭示版本 <%= HtmlUtil.escape(preview.getDisclosureVersion()) %></p>
                        </div>
                        <span class="tag"><%= HtmlUtil.escape(product.getRiskLevel()) %></span>
                    </div>
                    <div class="product-rate">
                        <span class="small-label">参考年化</span>
                        <strong><%= rateText(product.getExpectedRate()) %></strong>
                    </div>
                    <div class="product-meta-grid">
                        <div><span class="small-label">申购金额</span><strong>¥ <%= moneyText(preview.getAmount()) %></strong></div>
                        <div><span class="small-label">产品期限</span><strong><%= product.getPeriodDays() %> 天</strong></div>
                        <div><span class="small-label">客户风险</span><strong><%= HtmlUtil.escape(preview.getCustomerRiskLevel()) %></strong></div>
                        <div><span class="small-label">产品风险</span><strong><%= HtmlUtil.escape(preview.getProductRiskLevel()) %></strong></div>
                        <div><span class="small-label">申购确认</span><strong>T+<%= product.getConfirmDays() %></strong></div>
                        <div><span class="small-label">赎回到账</span><strong>T+<%= product.getArrivalDays() %></strong></div>
                    </div>
                </section>
                <section class="content-section">
                    <h2>付款账户</h2>
                    <div class="movement-item">
                        <div class="movement-item-main">
                            <strong><%= HtmlUtil.escape(account.getAccountNo()) %></strong>
                            <p>可用余额</p>
                        </div>
                        <strong>¥ <%= moneyText(account.getAvailableBalance()) %></strong>
                    </div>
                </section>
                <section class="content-section">
                    <h2>提交后系统动作</h2>
                    <ul class="task-checklist">
                        <li>再次校验风险等级、金额范围、余额和支付密码。</li>
                        <li>付款账户冻结申购金额，生成待确认的理财申购交易。</li>
                        <li>后台清算确认后创建持仓，并生成正式出账流水。</li>
                        <li>确认前资金不会进入理财持仓，确认后可在“我的持仓”追踪。</li>
                    </ul>
                </section>
            </aside>
        </section>
    <% } %>
</main>
</body>
</html>
