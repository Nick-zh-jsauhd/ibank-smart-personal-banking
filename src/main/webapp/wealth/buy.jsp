<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.bean.WealthProduct,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
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
%>
<%
    WealthProduct product = (WealthProduct) request.getAttribute("product");
    List<Account> accounts = (List<Account>) request.getAttribute("accounts");
    String error = (String) request.getAttribute("error");
    Long selectedAccountId = (Long) request.getAttribute("selectedAccountId");
    String amount = (String) request.getAttribute("amount");
    if (amount == null && product != null) {
        amount = product.getMinAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    request.setAttribute("activeNav", "wealth");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>理财申购 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Wealth Order</p>
            <h1>理财申购</h1>
            <p class="muted">选择付款账户和申购金额，下一步进入风险确认与支付密码校验。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/wealth/products">产品列表</a>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/wealth/holdings">我的持仓</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/risk/assessment">风险测评</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>申购前先确认这笔钱的用途和持有时间。</strong>
            <p>下一步会展示风险匹配与产品确认项，最终提交时会校验支付密码并冻结申购资金，清算确认后才生成持仓。</p>
        </div>
    </div>

    <% if (product == null) { %>
        <section class="content-section">
            <div class="human-empty">
                <strong>未找到可申购产品。</strong>
                <p>可能产品已下架或链接已失效，请返回产品列表重新选择。</p>
            </div>
            <div class="action-row">
                <a class="button primary" href="<%= request.getContextPath() %>/wealth/products">返回产品列表</a>
            </div>
        </section>
    <% } else { %>
        <section class="task-layout">
            <article class="task-panel with-warm-accent">
                <div class="section-title">
                    <div>
                        <h2>申购信息</h2>
                        <p class="section-note">申购提交后先冻结付款账户资金，后台清算确认后生成持仓和 OUT 流水。</p>
                    </div>
                </div>
                <form method="post" action="<%= request.getContextPath() %>/wealth/confirm" class="form">
                    <input type="hidden" name="productId" value="<%= product.getProductId() %>">
                    <label>
                        <span>付款账户</span>
                        <select name="accountId" required>
                            <option value="">请选择账户</option>
                            <% if (accounts != null) {
                                for (Account account : accounts) {
                                    boolean selected = selectedAccountId != null && selectedAccountId.equals(account.getAccountId());
                            %>
                                <option value="<%= account.getAccountId() %>" <%= selected ? "selected" : "" %>>
                                    <%= HtmlUtil.escape(account.getAccountNo()) %>｜可用余额 ¥ <%= moneyText(account.getAvailableBalance()) %>
                                </option>
                            <%  }
                            } %>
                        </select>
                    </label>
                    <label>
                        <span>申购金额</span>
                        <input name="amount" type="number"
                               min="<%= moneyText(product.getMinAmount()) %>"
                               max="<%= moneyText(product.getMaxAmount()) %>"
                               step="0.01"
                               value="<%= HtmlUtil.escape(amount) %>" required>
                    </label>
                    <button class="button wealth full" type="submit">进入风险确认</button>
                </form>
            </article>

            <aside class="task-side">
                <section class="content-section">
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
                        <strong><%= rateText(product.getExpectedRate()) %></strong>
                    </div>
                    <div class="product-meta-grid">
                        <div><span class="small-label">期限</span><strong><%= product.getPeriodDays() %> 天</strong></div>
                        <div><span class="small-label">起购</span><strong>¥ <%= moneyText(product.getMinAmount()) %></strong></div>
                        <div><span class="small-label">上限</span><strong>¥ <%= moneyText(product.getMaxAmount()) %></strong></div>
                        <div><span class="small-label">确认</span><strong>T+<%= product.getConfirmDays() %></strong></div>
                        <div><span class="small-label">到账</span><strong>T+<%= product.getArrivalDays() %></strong></div>
                        <div><span class="small-label">赎回</span><strong><%= product.isAllowEarlyRedeem() ? "可提前" : "到期后" %></strong></div>
                    </div>
                </section>
                <section class="content-section">
                    <h2>申购前校验</h2>
                    <ul class="task-checklist">
                        <li>客户风险等级需覆盖产品风险等级。</li>
                        <li>申购金额需在起购金额和购买上限之间。</li>
                        <li>付款账户可用余额需覆盖申购金额。</li>
                        <li>最终提交时需要 6 位数字支付密码。</li>
                        <li>提交后先冻结资金，清算确认后才形成持仓。</li>
                    </ul>
                </section>
                <section class="content-section">
                    <h2>可用账户</h2>
                    <div class="movement-list">
                        <% if (accounts == null || accounts.isEmpty()) { %>
                            <div class="human-empty">
                                <strong>暂时没有可付款账户。</strong>
                                <p>账户可用余额足够后，再继续完成理财申购确认。</p>
                            </div>
                        <% } else {
                            for (Account account : accounts) {
                        %>
                            <div class="movement-item">
                                <div class="movement-item-main">
                                    <strong><%= HtmlUtil.escape(account.getAccountNo()) %></strong>
                                    <p><%= HtmlUtil.escape(account.getAccountType()) %></p>
                                </div>
                                <strong>¥ <%= moneyText(account.getAvailableBalance()) %></strong>
                            </div>
                        <%  }
                        } %>
                    </div>
                </section>
            </aside>
        </section>
    <% } %>
</main>
</body>
</html>
