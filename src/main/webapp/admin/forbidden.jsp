<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil" %>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    String requiredPermission = (String) request.getAttribute("requiredPermission");
    String requestPath = (String) request.getAttribute("requestPath");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>无权限 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout narrow">
    <section class="content-section">
        <div class="section-title">
            <div>
                <p class="eyebrow">访问受限</p>
                <h1>无权限访问</h1>
                <p class="section-note">当前管理员账号没有执行该后台操作所需的权限。</p>
            </div>
        </div>
        <dl class="detail-list">
            <div><dt>请求路径</dt><dd><%= HtmlUtil.escape(requestPath) %></dd></div>
            <div><dt>所需权限</dt><dd><%= HtmlUtil.escape(requiredPermission) %></dd></div>
        </dl>
        <div class="action-row">
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/dashboard">返回后台首页</a>
        </div>
    </section>
</main>
</body>
</html>
