<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.util.HtmlUtil" %>
<%
    String identity = (String) request.getAttribute("identity");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>后台登录 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="access-page access-page-admin">
<main class="access-canvas">
    <header class="access-header" aria-label="后台登录页导航">
        <a class="access-brand" href="<%= request.getContextPath() %>/">
            <span></span>
            <strong>iBank Admin</strong>
        </a>
        <nav class="access-links" aria-label="角色切换">
            <a href="<%= request.getContextPath() %>/">门户首页</a>
            <a href="<%= request.getContextPath() %>/login">客户入口</a>
        </nav>
    </header>

    <section class="access-layout admin">
        <section class="access-console admin" aria-label="后台登录表单">
            <div class="access-console-head">
                <span>授权人员验证</span>
                <h2>进入运营控制台</h2>
                <p>后台会按角色加载客户、风控、清算、工单和审计功能。高风险操作会进入审计日志。</p>
            </div>

            <% if (error != null) { %>
                <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
            <% } %>

            <form method="post" action="<%= request.getContextPath() %>/admin/login" class="access-form">
                <label>
                    <span>管理员账号</span>
                    <input name="identity" type="text" value="<%= HtmlUtil.escape(identity) %>" required autocomplete="username" placeholder="输入管理员账号">
                </label>
                <label>
                    <span>登录密码</span>
                    <input name="password" type="password" required autocomplete="current-password" placeholder="输入登录密码">
                </label>
                <button class="access-submit" type="submit">进入后台</button>
            </form>

            <footer class="access-console-foot">
                <a href="<%= request.getContextPath() %>/login">返回客户登录</a>
            </footer>
        </section>

        <div class="access-copy" aria-label="运营后台说明">
            <p class="access-kicker">运营后台登录</p>
            <h1>进入后台前，确认职责边界。</h1>
            <p>风险复核、理财清算、调账审批、工单处理和审计追踪在这里分工协作。系统记录每一次授权动作和处理结果。</p>
            <div class="access-path" aria-label="后台处理路径">
                <span>权限</span>
                <i></i>
                <span>待办</span>
                <i></i>
                <span>复核</span>
                <i></i>
                <span>留痕</span>
            </div>
        </div>
    </section>
</main>
</body>
</html>
