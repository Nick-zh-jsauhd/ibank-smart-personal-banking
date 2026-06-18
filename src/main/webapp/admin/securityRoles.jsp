<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminPermissionView,com.bank.dto.AdminRoleView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.util.List" %>
<%!
    private String statusName(String status) {
        if ("ACTIVE".equals(status)) return "启用";
        if ("DISABLED".equals(status)) return "停用";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("ACTIVE".equals(status)) return "status";
        return "direction direction-out";
    }

    private String roleFocus(String roleCode) {
        if ("SUPER_ADMIN".equals(roleCode)) return "管理系统级账号、角色和关键治理能力。";
        if ("RISK_OPERATOR".equals(roleCode)) return "处理风控事件、GNN 复核和异常交易线索。";
        if ("RISK_MANAGER".equals(roleCode)) return "维护限额规则和风险控制策略。";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "处理对账异常、发起调账和跟进账务问题。";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "复核调账申请并控制资金修正风险。";
        if ("CUSTOMER_OPERATOR".equals(roleCode)) return "查看客户档案、跟进服务请求和客户风险等级。";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "维护理财产品、上下架状态和产品参数。";
        if ("AUDITOR".equals(roleCode)) return "查看审计日志、越权访问和高风险后台操作。";
        return "按权限清单执行对应后台工作。";
    }

    private String roleBoundary(String roleCode) {
        if ("SUPER_ADMIN".equals(roleCode)) return "不能绕过审计日志，不能移除最后一个启用的超级管理员。";
        if ("RISK_OPERATOR".equals(roleCode)) return "模型高分不等于最终裁决，关键风险仍需留痕说明。";
        if ("RISK_MANAGER".equals(roleCode)) return "规则调整会影响交易体验，修改前要确认限额和误伤成本。";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "不能直接完成复核，资金修正需要进入复核链路。";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "复核结论要说明依据，避免无理由通过或驳回。";
        if ("CUSTOMER_OPERATOR".equals(roleCode)) return "客户信息只用于服务和运营处理，不承担系统级权限维护。";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "理财产品变更应与客户风险等级和销售状态保持一致。";
        if ("AUDITOR".equals(roleCode)) return "审计角色主要观察和追踪，不参与业务执行。";
        return "权限边界以表格中的权限编码为准。";
    }

    private String moduleSummary(AdminRoleView role) {
        StringBuilder builder = new StringBuilder();
        for (AdminPermissionView permission : role.getPermissions()) {
            String module = permission.getModule();
            if (module == null || module.length() == 0) {
                continue;
            }
            if (builder.indexOf(module) < 0) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append(module);
            }
        }
        return builder.length() == 0 ? "无模块" : builder.toString();
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdminRoleView> roles = (List<AdminRoleView>) request.getAttribute("roles");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>角色权限 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">角色权限</p>
        <h1>角色权限</h1>
        <p class="muted">查看后台角色与权限清单，确认岗位边界和可操作范围。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (roles == null || roles.isEmpty()) { %>
        <section class="content-section">
            <p class="empty">暂无角色权限配置</p>
        </section>
    <% } else {
        for (AdminRoleView role : roles) {
    %>
        <section class="content-section filter-section">
            <div class="section-title">
                <div>
                    <h2><%= HtmlUtil.escape(role.getRoleName()) %></h2>
                    <p class="section-note"><%= HtmlUtil.escape(role.getDescription()) %></p>
                </div>
                <div class="section-actions">
                    <span class="<%= statusClass(role.getStatus()) %>"><%= statusName(role.getStatus()) %></span>
                    <span class="tag">系统角色</span>
                </div>
            </div>
            <div class="admin-role-brief">
                <article>
                    <span>岗位重点</span>
                    <strong><%= HtmlUtil.escape(roleFocus(role.getRoleCode())) %></strong>
                </article>
                <article>
                    <span>权限边界</span>
                    <strong><%= HtmlUtil.escape(roleBoundary(role.getRoleCode())) %></strong>
                </article>
                <article>
                    <span>覆盖模块</span>
                    <strong><%= HtmlUtil.escape(moduleSummary(role)) %></strong>
                </article>
                <article>
                    <span>权限数量</span>
                    <strong><%= role.getPermissions().size() %> 项</strong>
                </article>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>权限编码</th>
                        <th>权限名称</th>
                        <th>模块</th>
                        <th>说明</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (role.getPermissions().isEmpty()) { %>
                        <tr><td colspan="4" class="empty">该角色暂无权限</td></tr>
                    <% } else {
                        for (AdminPermissionView permission : role.getPermissions()) {
                    %>
                        <tr>
                            <td><strong><%= HtmlUtil.escape(permission.getPermissionCode()) %></strong></td>
                            <td><%= HtmlUtil.escape(permission.getPermissionName()) %></td>
                            <td><span class="tag"><%= HtmlUtil.escape(permission.getModule()) %></span></td>
                            <td><%= HtmlUtil.escape(permission.getDescription()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>
    <%  }
    } %>
</main>
</body>
</html>

