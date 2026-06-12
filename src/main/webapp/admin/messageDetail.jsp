<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.AdminAlert,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String statusName(String status) {
        if ("NEW".equals(status)) return "待确认";
        if ("ACKED".equals(status)) return "处理中";
        if ("RESOLVED".equals(status)) return "已解决";
        if ("CLOSED".equals(status)) return "已关闭";
        return status == null ? "" : status;
    }

    private String severityName(String severity) {
        if ("CRITICAL".equals(severity)) return "严重";
        if ("HIGH".equals(severity)) return "高";
        if ("WARNING".equals(severity)) return "预警";
        if ("INFO".equals(severity)) return "提示";
        return severity == null ? "" : severity;
    }

    private String roleName(String roleCode) {
        if ("RISK_OPERATOR".equals(roleCode)) return "风控运营";
        if ("RISK_MANAGER".equals(roleCode)) return "风控规则管理员";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "账务运营";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "账务复核员";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "理财产品管理员";
        if ("SUPER_ADMIN".equals(roleCode)) return "超级管理员";
        return roleCode == null || roleCode.length() == 0 ? "全局" : roleCode;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminAlert alert = (AdminAlert) request.getAttribute("alert");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>消息详情 - iBank Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Message Detail</p>
        <h1>消息详情</h1>
        <p class="muted">查看告警来源、责任角色和当前处理状态，并完成确认、解决或关闭。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (alert != null) { %>
        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2><%= HtmlUtil.escape(alert.getTitle()) %></h2>
                    <p class="section-note"><%= HtmlUtil.escape(alert.getContent()) %></p>
                </div>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/messages">返回列表</a>
            </div>

            <dl class="detail-list">
                <div>
                    <dt>级别</dt>
                    <dd><span class="tag severity-<%= HtmlUtil.escape(alert.getSeverity() == null ? "INFO" : alert.getSeverity().toLowerCase()) %>"><%= HtmlUtil.escape(severityName(alert.getSeverity())) %></span></dd>
                </div>
                <div>
                    <dt>状态</dt>
                    <dd><span class="status"><%= HtmlUtil.escape(statusName(alert.getStatus())) %></span></dd>
                </div>
                <div>
                    <dt>消息类型</dt>
                    <dd><%= HtmlUtil.escape(alert.getAlertType()) %></dd>
                </div>
                <div>
                    <dt>责任角色</dt>
                    <dd><%= HtmlUtil.escape(roleName(alert.getResponsibleRoleCode())) %></dd>
                </div>
                <div>
                    <dt>关联对象</dt>
                    <dd>
                        <%= HtmlUtil.escape(alert.getTargetType()) %> #<%= HtmlUtil.escape(alert.getTargetId()) %>
                        <% if ("ADJUSTMENT_REQUEST".equals(alert.getTargetType()) && alert.getTargetId() != null) { %>
                            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/adjustment/detail?adjustmentId=<%= HtmlUtil.escape(alert.getTargetId()) %>">查看调账</a>
                        <% } else if ("RECONCILIATION_BATCH".equals(alert.getTargetType()) && alert.getTargetId() != null) { %>
                            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation/detail?batchId=<%= HtmlUtil.escape(alert.getTargetId()) %>">查看对账</a>
                        <% } else if ("RECONCILIATION_ITEM".equals(alert.getTargetType()) && alert.getTargetId() != null) { %>
                            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation/item/detail?itemId=<%= HtmlUtil.escape(alert.getTargetId()) %>">查看异常</a>
                        <% } else if ("SERVICE_TICKET".equals(alert.getTargetType()) && alert.getTargetId() != null) { %>
                            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/ticket/detail?ticketId=<%= HtmlUtil.escape(alert.getTargetId()) %>">查看工单</a>
                        <% } %>
                    </dd>
                </div>
                <div>
                    <dt>创建时间</dt>
                    <dd><%= timeText(alert.getCreatedAt()) %></dd>
                </div>
                <div>
                    <dt>接收人</dt>
                    <dd><%= HtmlUtil.escape(alert.getAssignedAdminUsername()) %></dd>
                </div>
                <div>
                    <dt>处理人</dt>
                    <dd><%= HtmlUtil.escape(alert.getHandledByAdminUsername()) %></dd>
                </div>
                <div>
                    <dt>处理时间</dt>
                    <dd><%= timeText(alert.getHandledAt()) %></dd>
                </div>
                <div>
                    <dt>处理说明</dt>
                    <dd><%= HtmlUtil.escape(alert.getHandleNote()) %></dd>
                </div>
            </dl>
        </section>

        <% if (adminUser.hasPermission("ADMIN_ALERT_HANDLE") && !"CLOSED".equals(alert.getStatus())) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>处理动作</h2>
                        <p class="section-note">先确认是否承接，再根据关联业务选择解决或关闭。每次提交都会记录处理人和处理说明。</p>
                    </div>
                </div>
                <div class="admin-next-step-grid">
                    <article>
                        <span>确认接收</span>
                        <strong>把待办从“无人承接”变成“处理中”。</strong>
                        <p>适用于你已经开始查看调账、工单或对账异常，但还没有最终结果。</p>
                    </article>
                    <article>
                        <span>标记解决</span>
                        <strong>说明业务已经处理完毕。</strong>
                        <p>建议写清楚关联业务结果，例如“调账已复核通过”或“工单已回复客户”。</p>
                    </article>
                    <article>
                        <span>关闭消息</span>
                        <strong>终止这个告警的后台跟进。</strong>
                        <p>适用于重复告警、无需处理或已由其他流程覆盖的消息，必须写明原因。</p>
                    </article>
                </div>
                <form class="grid-form" method="post" action="<%= request.getContextPath() %>/admin/message/detail">
                    <input type="hidden" name="alertId" value="<%= alert.getAlertId() %>">
                    <label>
                        <span>动作</span>
                        <select name="action">
                            <option value="ACK">确认接收</option>
                            <option value="RESOLVE">标记解决</option>
                            <option value="CLOSE">关闭消息</option>
                        </select>
                    </label>
                    <label class="full-row">
                        <span>处理说明</span>
                        <textarea name="note" rows="4" maxlength="500" placeholder="解决或关闭时至少填写 5 个字"></textarea>
                    </label>
                    <div class="form-actions">
                        <button class="button primary" type="submit">提交处理</button>
                    </div>
                </form>
            </section>
        <% } %>
    <% } %>
</main>
</body>
</html>
