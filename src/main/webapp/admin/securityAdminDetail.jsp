<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRoleView,com.bank.dto.AdminSessionUser,com.bank.dto.AdminUserDetail,com.bank.dto.AdminUserView,com.bank.util.HtmlUtil,java.sql.Timestamp" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String statusName(String status) {
        if ("NORMAL".equals(status)) return "启用";
        if ("DISABLED".equals(status)) return "停用";
        return status == null ? "" : status;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminUserDetail detail = (AdminUserDetail) request.getAttribute("detail");
    AdminUserView target = detail == null ? null : detail.getAdminUser();
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>管理员详情 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Admin User Detail</p>
        <h1>管理员详情</h1>
        <p class="muted">查看账号状态、登录信息并维护后台角色。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (target == null) { %>
        <section class="content-section">
            <p class="empty">管理员账号不存在。</p>
            <div class="action-row">
                <a class="button secondary" href="<%= request.getContextPath() %>/admin/security/admins">返回列表</a>
            </div>
        </section>
    <% } else { %>
        <section class="content-section">
            <div class="section-title">
                <h2>账号信息</h2>
                <div class="section-actions">
                    <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/security/admins">返回列表</a>
                </div>
            </div>
            <dl class="detail-list">
                <div><dt>账号</dt><dd><%= HtmlUtil.escape(target.getUsername()) %></dd></div>
                <div><dt>手机号</dt><dd><%= HtmlUtil.escape(target.getPhone()) %></dd></div>
                <div><dt>状态</dt><dd><%= statusName(target.getStatus()) %></dd></div>
                <div><dt>当前角色</dt><dd><%= HtmlUtil.escape(target.roleNameSummary()) %></dd></div>
                <div><dt>登录失败次数</dt><dd><%= target.getFailedLoginCount() %></dd></div>
                <div><dt>锁定到</dt><dd><%= timeText(target.getLockedUntil()) %></dd></div>
                <div><dt>最后登录</dt><dd><%= timeText(target.getLastLoginAt()) %></dd></div>
                <div><dt>创建时间</dt><dd><%= timeText(target.getCreatedAt()) %></dd></div>
                <div><dt>更新时间</dt><dd><%= timeText(target.getUpdatedAt()) %></dd></div>
            </dl>
        </section>

        <% if (adminUser.hasPermission("ADMIN_USER_MANAGE")) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>账号和角色</h2>
                        <p class="section-note">系统会阻止停用自己、修改自己的角色，或移除最后一个启用的超级管理员。</p>
                    </div>
                </div>
                <form method="post" action="<%= request.getContextPath() %>/admin/security/admin/detail" class="form grid-form">
                    <input type="hidden" name="userId" value="<%= target.getUserId() %>">
                    <label>
                        <span>账号状态</span>
                        <select name="status">
                            <option value="NORMAL" <%= "NORMAL".equals(target.getStatus()) ? "selected" : "" %>>启用</option>
                            <option value="DISABLED" <%= "DISABLED".equals(target.getStatus()) ? "selected" : "" %>>停用</option>
                        </select>
                    </label>
                    <label>
                        <span>重置密码</span>
                        <input name="resetPassword" type="password" minlength="6" placeholder="不填写则不修改">
                    </label>
                    <div class="full-row">
                        <span class="field-title">角色分配</span>
                        <div class="security-role-grid">
                            <% for (AdminRoleView role : detail.getAvailableRoles()) { %>
                                <label class="checkbox-line">
                                    <input type="checkbox" name="roleCode" value="<%= HtmlUtil.escape(role.getRoleCode()) %>"
                                        <%= target.hasRole(role.getRoleCode()) ? "checked" : "" %>
                                        <%= "ACTIVE".equals(role.getStatus()) ? "" : "disabled" %>>
                                    <span><strong><%= HtmlUtil.escape(role.getRoleName()) %></strong><br><%= HtmlUtil.escape(role.getRoleCode()) %></span>
                                </label>
                            <% } %>
                        </div>
                    </div>
                    <div class="full-row action-row">
                        <button class="button primary" type="submit">保存变更</button>
                    </div>
                </form>
            </section>
        <% } %>
    <% } %>
</main>
</body>
</html>

