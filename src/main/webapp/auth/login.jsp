<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.util.HtmlUtil" %>
<%
    String identity = (String) request.getAttribute("identity");
    String error = (String) request.getAttribute("error");
    Object flash = session.getAttribute("flash");
    if (flash != null) {
        session.removeAttribute("flash");
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>客户登录 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="access-page access-page-client">
<main class="access-canvas">
    <header class="access-header" aria-label="登录页导航">
        <a class="access-brand" href="<%= request.getContextPath() %>/">
            <span></span>
            <strong>iBank</strong>
        </a>
        <nav class="access-links" aria-label="角色切换">
            <a href="<%= request.getContextPath() %>/">门户首页</a>
            <a href="<%= request.getContextPath() %>/admin/login">管理入口</a>
        </nav>
    </header>

    <section class="access-layout client">
        <div class="access-copy" aria-label="个人银行说明">
            <p class="access-kicker">个人银行登录</p>
            <h1>进入前，先知道今天会看到什么。</h1>
            <p>登录后，系统会把账户余额、月账单、安全提醒、理财持仓和服务进度合并到工作台，先给你一张清楚的资金地图。</p>
            <div class="access-path" aria-label="登录后路径">
                <span>资产</span>
                <i></i>
                <span>账单</span>
                <i></i>
                <span>安全</span>
                <i></i>
                <span>服务</span>
            </div>
        </div>

        <section class="access-console" aria-label="客户登录表单">
            <div class="access-console-head">
                <span>客户验证</span>
                <h2>登录个人银行</h2>
                <p>使用用户名或手机号继续。涉及转账、缴费和理财操作时，仍会进入支付密码与风控校验。</p>
            </div>

            <% if (flash != null) { %>
                <div class="alert success"><%= HtmlUtil.escape(String.valueOf(flash)) %></div>
            <% } %>
            <% if (error != null) { %>
                <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
            <% } %>

            <form method="post" action="<%= request.getContextPath() %>/login" class="access-form">
                <label>
                    <span>用户名 / 手机号</span>
                    <input name="identity" type="text" value="<%= HtmlUtil.escape(identity) %>" required autocomplete="username" placeholder="输入用户名或手机号">
                </label>
                <label>
                    <span>登录密码</span>
                    <input name="password" type="password" required autocomplete="current-password" placeholder="输入登录密码">
                </label>
                <button class="access-submit" type="submit">进入工作台</button>
            </form>

            <footer class="access-console-foot">
                <span>还没有账户？</span>
                <a href="<%= request.getContextPath() %>/register">开通客户账户</a>
            </footer>
        </section>
    </section>
</main>
</body>
</html>
