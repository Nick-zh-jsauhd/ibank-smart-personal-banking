<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.RegisterRequest,com.bank.util.HtmlUtil" %>
<%
    RegisterRequest form = (RegisterRequest) request.getAttribute("form");
    String error = (String) request.getAttribute("error");
    String username = form == null ? "" : form.getUsername();
    String phone = form == null ? "" : form.getPhone();
    String fullName = form == null ? "" : form.getFullName();
    String email = form == null ? "" : form.getEmail();
    String address = form == null ? "" : form.getAddress();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>注册 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="public-page warm-finance-page">
<main class="auth-shell wide">
    <section class="auth-layout">
    <aside class="auth-visual">
        <div>
            <p class="eyebrow">客户开户</p>
            <h1>开通后，我们会先帮你准备账户、账单和安全保护。</h1>
            <p class="muted">注册链路保持简单，但后台会同步建立客户档案、默认储蓄账户和风险等级，方便后续转账、缴费、理财与服务跟进。</p>
        </div>
        <div class="auth-proof-grid">
            <div class="auth-proof"><strong>1</strong><span>创建登录账户</span></div>
            <div class="auth-proof"><strong>2</strong><span>建立客户档案</span></div>
            <div class="auth-proof"><strong>3</strong><span>生成默认账户</span></div>
        </div>
    </aside>
    <section class="auth-panel">
        <a class="brand" href="<%= request.getContextPath() %>/">iBank</a>
        <h1>注册客户</h1>
        <p class="muted">完成注册后，可先进入安全中心设置支付密码，再进行资金操作。</p>

        <% if (error != null) { %>
            <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
        <% } %>

        <form method="post" action="<%= request.getContextPath() %>/register" class="form grid-form">
            <label>
                <span>用户名</span>
                <input name="username" type="text" value="<%= HtmlUtil.escape(username) %>" required autocomplete="username">
            </label>
            <label>
                <span>手机号</span>
                <input name="phone" type="tel" value="<%= HtmlUtil.escape(phone) %>" required autocomplete="tel">
            </label>
            <label>
                <span>客户姓名</span>
                <input name="fullName" type="text" value="<%= HtmlUtil.escape(fullName) %>" required autocomplete="name">
            </label>
            <label>
                <span>邮箱</span>
                <input name="email" type="email" value="<%= HtmlUtil.escape(email) %>" autocomplete="email">
            </label>
            <label class="full-row">
                <span>联系地址</span>
                <input name="address" type="text" value="<%= HtmlUtil.escape(address) %>" autocomplete="street-address">
            </label>
            <label>
                <span>登录密码</span>
                <input name="password" type="password" required autocomplete="new-password">
            </label>
            <label>
                <span>确认密码</span>
                <input name="confirmPassword" type="password" required autocomplete="new-password">
            </label>
            <button class="button primary full-row" type="submit">创建客户与默认账户</button>
        </form>

        <p class="switch-link">已有账户？<a href="<%= request.getContextPath() %>/login">返回登录</a></p>
    </section>
    </section>
</main>
</body>
</html>
