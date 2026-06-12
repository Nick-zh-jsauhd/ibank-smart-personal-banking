<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRoleView,com.bank.dto.AdminSessionUser,com.bank.dto.AdminUserView,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String statusName(String status) {
        if ("NORMAL".equals(status)) return "启用";
        if ("DISABLED".equals(status)) return "停用";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("NORMAL".equals(status)) return "status";
        return "direction direction-out";
    }

    private boolean selectedRole(String roleCode, String[] selectedRoleCodes) {
        if (roleCode == null || selectedRoleCodes == null) {
            return false;
        }
        for (String selectedRoleCode : selectedRoleCodes) {
            if (roleCode.equals(selectedRoleCode)) {
                return true;
            }
        }
        return false;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminUserView> admins = (List<AdminUserView>) request.getAttribute("admins");
    List<AdminRoleView> roles = (List<AdminRoleView>) request.getAttribute("roles");
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    String selectedKeyword = (String) request.getAttribute("selectedKeyword");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    String formUsername = (String) request.getAttribute("formUsername");
    String formPhone = (String) request.getAttribute("formPhone");
    String[] formRoleCodes = (String[]) request.getAttribute("formRoleCodes");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>管理员账号 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Admin Users</p>
        <h1>管理员账号</h1>
        <p class="muted">维护后台人员账号、启停状态和角色分配。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/security/admins" class="filter-form">
            <label>
                <span>关键词</span>
                <input name="keyword" value="<%= HtmlUtil.escape(selectedKeyword) %>" placeholder="账号或手机号">
            </label>
            <label>
                <span>状态</span>
                <select name="status">
                    <option value="">全部</option>
                    <option value="NORMAL" <%= "NORMAL".equals(selectedStatus) ? "selected" : "" %>>启用</option>
                    <option value="DISABLED" <%= "DISABLED".equals(selectedStatus) ? "selected" : "" %>>停用</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <% if (adminUser.hasPermission("ADMIN_USER_MANAGE")) { %>
        <section class="content-section filter-section">
            <div class="section-title">
                <div>
                    <h2>创建管理员</h2>
                    <p class="section-note">新账号默认启用，只能用于后台登录。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/admin/security/admins" class="form grid-form">
                <label>
                    <span>管理员账号</span>
                    <input name="username" value="<%= HtmlUtil.escape(formUsername) %>" maxlength="50" required>
                </label>
                <label>
                    <span>手机号</span>
                    <input name="phone" value="<%= HtmlUtil.escape(formPhone) %>" maxlength="20" required>
                </label>
                <label>
                    <span>初始密码</span>
                    <input name="password" type="password" minlength="6" required>
                </label>
                <div class="full-row">
                    <span class="field-title">初始角色</span>
                    <div class="security-role-grid">
                        <% if (roles != null) {
                            for (AdminRoleView role : roles) {
                                if (!"ACTIVE".equals(role.getStatus())) {
                                    continue;
                                }
                        %>
                            <label class="checkbox-line">
                                <input type="checkbox" name="roleCode" value="<%= HtmlUtil.escape(role.getRoleCode()) %>"
                                    <%= selectedRole(role.getRoleCode(), formRoleCodes) ? "checked" : "" %>>
                                <span><strong><%= HtmlUtil.escape(role.getRoleName()) %></strong><br><%= HtmlUtil.escape(role.getRoleCode()) %></span>
                            </label>
                        <%  }
                        } %>
                    </div>
                </div>
                <div class="full-row action-row">
                    <button class="button primary" type="submit">创建管理员</button>
                </div>
            </form>
        </section>
    <% } %>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>账号列表</h2>
                <p class="section-note">默认最多展示最近 100 个后台管理员账号。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>账号</th>
                    <th>手机号</th>
                    <th>状态</th>
                    <th>角色</th>
                    <th>失败次数</th>
                    <th>最后登录</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (admins == null || admins.isEmpty()) { %>
                    <tr><td colspan="8" class="empty">暂无管理员账号</td></tr>
                <% } else {
                    for (AdminUserView admin : admins) {
                %>
                    <tr>
                        <td><strong><%= HtmlUtil.escape(admin.getUsername()) %></strong></td>
                        <td><%= HtmlUtil.escape(admin.getPhone()) %></td>
                        <td><span class="<%= statusClass(admin.getStatus()) %>"><%= statusName(admin.getStatus()) %></span></td>
                        <td><%= HtmlUtil.escape(admin.roleNameSummary()) %></td>
                        <td><%= admin.getFailedLoginCount() %></td>
                        <td><%= timeText(admin.getLastLoginAt()) %></td>
                        <td><%= timeText(admin.getCreatedAt()) %></td>
                        <td>
                            <a class="button secondary compact"
                               href="<%= request.getContextPath() %>/admin/security/admin/detail?userId=<%= admin.getUserId() %>">详情</a>
                        </td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>
</main>
</body>
</html>

