<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.util.HtmlUtil" %>
<%
    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
    request.setAttribute("activeNav", "security");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>支付密码 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Security</p>
            <h1>支付密码</h1>
            <p class="muted">支付密码用于取款、转账、缴费、理财申购和赎回等资金操作。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/risk/events">查看风控事件</a>
    </section>

    <% if (message != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(message) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="photo-band security">
        <div>
            <p class="eyebrow">Security Care</p>
            <h2>支付前多一步确认，就是给账户多一层保护。</h2>
            <p class="section-note">支付密码只用于资金操作。遇到陌生来电、屏幕共享、代办转账等情况，请先暂停交易并查看风控提醒。</p>
        </div>
    </section>

    <section class="task-layout">
        <article class="task-panel with-warm-accent">
            <div class="section-title">
                <div>
                    <h2>设置或更新支付密码</h2>
                    <p class="section-note">需要先验证登录密码，再设置 6 位数字支付密码。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/security/pay-password" class="form">
                <label>
                    <span>登录密码</span>
                    <input name="loginPassword" type="password" required autocomplete="current-password">
                </label>
                <label>
                    <span>支付密码</span>
                    <input name="payPassword" type="password" required maxlength="6" pattern="\d{6}" inputmode="numeric" autocomplete="new-password">
                </label>
                <label>
                    <span>确认支付密码</span>
                    <input name="confirmPayPassword" type="password" required maxlength="6" pattern="\d{6}" inputmode="numeric" autocomplete="new-password">
                </label>
                <button class="button primary full" type="submit">保存支付密码</button>
            </form>
        </article>

        <aside class="task-side">
            <section class="content-section">
                <h2>资金操作保护</h2>
                <ul class="task-checklist">
                    <li>支付密码必须为 6 位数字，和登录密码分离。</li>
                    <li>取款、转账、缴费、理财申购和赎回都会校验支付密码。</li>
                    <li>密码错误不会改变余额，也不会生成成功流水。</li>
                    <li>大额或异常交易仍会经过风控规则和限额规则校验。</li>
                </ul>
            </section>
            <section class="security-advice">
                <div class="security-advice-item">
                    <strong>防骗提醒</strong>
                    <p>银行不会索要你的支付密码，也不会要求开启屏幕共享完成转账。</p>
                </div>
                <div class="security-advice-item">
                    <strong>大额确认</strong>
                    <p>金额、收款人或频次异常时，系统会触发限额和风控规则，请按页面提示确认。</p>
                </div>
                <div class="security-advice-item">
                    <strong>家人协助</strong>
                    <p>给长辈使用时，建议先说明“只在本人发起交易时输入密码”，不要通过聊天工具转发验证码或密码。</p>
                </div>
            </section>
            <section class="content-section">
                <h2>相关入口</h2>
                <div class="quick-link-grid">
                    <a class="quick-link" href="<%= request.getContextPath() %>/transfer"><strong>转账</strong><span>本行资金划转</span></a>
                    <a class="quick-link" href="<%= request.getContextPath() %>/payment"><strong>缴费</strong><span>水电燃气话费</span></a>
                    <a class="quick-link" href="<%= request.getContextPath() %>/wealth/products"><strong>理财</strong><span>申购和持仓</span></a>
                    <a class="quick-link" href="<%= request.getContextPath() %>/risk/events"><strong>风控</strong><span>异常事件追踪</span></a>
                </div>
            </section>
        </aside>
    </section>
</main>
</body>
</html>
