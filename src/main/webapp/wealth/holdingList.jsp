<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.WealthHoldingView,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
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
        if ("HOLDING".equals(status)) {
            return "持有中";
        }
        if ("REDEEMED".equals(status)) {
            return "已赎回";
        }
        if ("REDEEMING".equals(status)) {
            return "赎回处理中";
        }
        return status == null ? "" : status;
    }

    private BigDecimal safeAdd(BigDecimal left, BigDecimal right) {
        return left.add(right == null ? BigDecimal.ZERO : right);
    }
%>
<%
    List<WealthHoldingView> holdings = (List<WealthHoldingView>) request.getAttribute("holdings");
    String error = (String) request.getAttribute("error");
    int activeCount = 0;
    int redeemedCount = 0;
    BigDecimal totalPrincipal = BigDecimal.ZERO;
    BigDecimal totalIncome = BigDecimal.ZERO;
    BigDecimal totalRedeem = BigDecimal.ZERO;
    if (holdings != null) {
        for (WealthHoldingView holding : holdings) {
            if ("HOLDING".equals(holding.getStatus()) || "REDEEMING".equals(holding.getStatus())) {
                activeCount++;
                totalPrincipal = safeAdd(totalPrincipal, holding.getPrincipal());
                totalIncome = safeAdd(totalIncome, holding.getCurrentIncome());
                totalRedeem = safeAdd(totalRedeem, holding.getRedeemAmount());
            } else if ("REDEEMED".equals(holding.getStatus())) {
                redeemedCount++;
            }
        }
    }
    request.setAttribute("activeNav", "wealth");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>我的持仓 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Wealth Holdings</p>
            <h1>我的理财持仓</h1>
            <p class="muted">集中查看持有本金、估算收益、可赎回金额和赎回入口。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/wealth/products">继续申购</a>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/risk/assessment">风险测评</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/transactions?txnType=REDEEM_WEALTH">赎回流水</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>持仓页用于看清“本金、估算收益和赎回动作”。</strong>
            <p>持仓收益只体现在理财资产估值里，不会每天进入账户余额；赎回清算到账后才生成入账流水。</p>
        </div>
    </div>

    <section class="metric-grid">
        <article class="metric-card">
            <span>持有中产品</span>
            <strong><%= activeCount %></strong>
            <p class="section-note">已赎回 <%= redeemedCount %> 笔</p>
        </article>
        <article class="metric-card">
            <span>持有本金</span>
            <strong>¥ <%= moneyText(totalPrincipal) %></strong>
        </article>
        <article class="metric-card">
            <span>估算收益</span>
            <strong>¥ <%= moneyText(totalIncome) %></strong>
        </article>
        <article class="metric-card">
            <span>可赎回合计</span>
            <strong>¥ <%= moneyText(totalRedeem) %></strong>
        </article>
    </section>

    <% if (holdings == null || holdings.isEmpty()) { %>
        <section class="content-section">
            <div class="human-empty">
                <strong>你还没有理财持仓。</strong>
                <p>可以先完成风险测评，再从产品列表选择适合期限和风险等级的产品。</p>
            </div>
            <div class="action-row">
                <a class="button wealth" href="<%= request.getContextPath() %>/wealth/products">浏览理财产品</a>
            </div>
        </section>
    <% } else { %>
        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>持仓卡片</h2>
                    <p class="section-note">赎回会生成 REDEEM_WEALTH 入账流水，并更新持仓状态。</p>
                </div>
            </div>
            <div class="product-card-grid">
                <% for (WealthHoldingView holding : holdings) {
                    boolean active = "HOLDING".equals(holding.getStatus());
                    boolean redeeming = "REDEEMING".equals(holding.getStatus());
                    boolean canRedeem = active && (holding.isAllowEarlyRedeem() || holding.getRemainingDays() == 0);
                %>
                    <article class="product-card holding-card">
                        <div>
                            <div class="section-title">
                                <div>
                                    <h2><%= HtmlUtil.escape(holding.getProductName()) %></h2>
                                    <p class="section-note"><%= HtmlUtil.escape(holding.getProductCode()) %>，<%= HtmlUtil.escape(holding.getAccountNo()) %></p>
                                </div>
                                <span class="<%= active ? "status" : "tag" %>"><%= HtmlUtil.escape(statusName(holding.getStatus())) %></span>
                            </div>
                            <div class="product-rate">
                                <span class="small-label">可赎回金额</span>
                                <strong>¥ <%= moneyText(holding.getRedeemAmount()) %></strong>
                            </div>
                            <div class="product-meta-grid">
                                <div><span class="small-label">本金</span><strong>¥ <%= moneyText(holding.getPrincipal()) %></strong></div>
                                <div><span class="small-label">估算收益</span><strong>¥ <%= moneyText(holding.getCurrentIncome()) %></strong></div>
                                <div><span class="small-label">参考年化</span><strong><%= rateText(holding.getExpectedRate()) %></strong></div>
                                <div><span class="small-label">持有天数</span><strong><%= holding.getHoldingDays() %> 天</strong></div>
                                <div><span class="small-label">到期日</span><strong><%= holding.getMaturityDate() == null ? "-" : holding.getMaturityDate().toString() %></strong></div>
                                <div><span class="small-label">剩余封闭期</span><strong><%= holding.getRemainingDays() %> 天</strong></div>
                            </div>
                        </div>
                        <% if (canRedeem) { %>
                            <form class="inline-action holding-action" method="post" action="<%= request.getContextPath() %>/wealth/redeem">
                                <input type="hidden" name="holdingId" value="<%= holding.getHoldingId() %>">
                                <input name="payPassword" type="password" maxlength="6" pattern="\d{6}" inputmode="numeric" placeholder="支付密码" required>
                                <button class="button primary compact" type="submit">提交赎回</button>
                            </form>
                        <% } else if (active) { %>
                            <p class="section-note">封闭期未结束，剩余 <%= holding.getRemainingDays() %> 天后可提交赎回。</p>
                        <% } else if (redeeming) { %>
                            <p class="section-note">赎回已提交，预计到账日：<%= holding.getExpectedArrivalDate() == null ? "-" : holding.getExpectedArrivalDate().toString() %></p>
                        <% } else { %>
                            <p class="section-note">赎回时间：<%= holding.getRedeemTime() == null ? "" : holding.getRedeemTime().toString().substring(0, 19) %></p>
                        <% } %>
                    </article>
                <% } %>
            </div>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>持仓明细</h2>
                    <p class="section-note">表格保留完整字段，便于核对账户、期限和状态。</p>
                </div>
            </div>
            <div class="table-wrap">
                <table class="wide-table">
                    <thead>
                    <tr>
                        <th>产品</th>
                        <th>账户</th>
                        <th>风险</th>
                        <th>参考年化</th>
                        <th>本金</th>
                        <th>持有天数</th>
                        <th>到期日</th>
                        <th>剩余天数</th>
                        <th>估算收益</th>
                        <th>可赎回金额</th>
                        <th>状态</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (WealthHoldingView holding : holdings) { %>
                        <tr>
                            <td>
                                <strong><%= HtmlUtil.escape(holding.getProductName()) %></strong>
                                <p class="cell-note"><%= HtmlUtil.escape(holding.getProductCode()) %>，期限 <%= holding.getPeriodDays() %> 天</p>
                            </td>
                            <td><%= HtmlUtil.escape(holding.getAccountNo()) %></td>
                            <td><span class="tag"><%= HtmlUtil.escape(holding.getRiskLevel()) %></span></td>
                            <td><%= rateText(holding.getExpectedRate()) %></td>
                            <td>¥ <%= moneyText(holding.getPrincipal()) %></td>
                            <td><%= holding.getHoldingDays() %> 天</td>
                            <td><%= holding.getMaturityDate() == null ? "-" : holding.getMaturityDate().toString() %></td>
                            <td><%= holding.getRemainingDays() %> 天</td>
                            <td>¥ <%= moneyText(holding.getCurrentIncome()) %></td>
                            <td>¥ <%= moneyText(holding.getRedeemAmount()) %></td>
                            <td><span class="status"><%= HtmlUtil.escape(statusName(holding.getStatus())) %></span></td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </section>
    <% } %>
</main>
</body>
</html>
