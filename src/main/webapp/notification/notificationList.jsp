<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Notification,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.util.List" %>
<%!
    private String typeName(String type) {
        if ("TRANSACTION".equals(type)) return "交易提醒";
        if ("RISK".equals(type)) return "风控提醒";
        if ("WEALTH".equals(type)) return "理财提醒";
        if ("SECURITY".equals(type)) return "安全提醒";
        if ("SERVICE".equals(type)) return "服务工单";
        return type == null ? "系统通知" : type;
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
    String error = (String) request.getAttribute("error");
    String selectedType = (String) request.getAttribute("selectedType");
    Integer unreadCount = (Integer) request.getAttribute("unreadCount");
    if (unreadCount == null) {
        unreadCount = 0;
    }
    request.setAttribute("activeNav", "service");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>通知中心 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Notification Center</p>
            <h1>通知中心</h1>
            <p class="muted">交易、理财、风控、安全和服务事件会在这里形成站内提醒。</p>
        </div>
        <form method="post" action="<%= request.getContextPath() %>/notifications/read-all">
            <button class="button secondary compact" type="submit">全部已读</button>
        </form>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>通知不是简单消息，而是业务提醒入口。</strong>
            <p>交易、风控、安全和服务类通知会说明发生了什么、关联哪笔业务，以及你是否需要继续处理。</p>
        </div>
    </div>

    <section class="insight-grid">
        <article class="cashflow-card">
            <div class="section-title">
                <div>
                    <h2>消息概览</h2>
                    <p class="section-note">当前未读 <%= unreadCount %> 条，最多展示最近 100 条。</p>
                </div>
            </div>
            <div class="cashflow-summary">
                <div><span class="small-label">未读</span><strong><%= unreadCount %></strong></div>
                <div><span class="small-label">本页消息</span><strong><%= notifications == null ? 0 : notifications.size() %></strong></div>
                <div><span class="small-label">筛选类型</span><strong><%= selectedType == null || selectedType.length() == 0 ? "全部" : HtmlUtil.escape(typeName(selectedType)) %></strong></div>
            </div>
        </article>
        <aside class="content-section filter-section">
            <div class="section-title">
                <div>
                    <h2>消息分类</h2>
                    <p class="section-note">按业务来源收敛通知。</p>
                </div>
            </div>
            <form class="form" method="get" action="<%= request.getContextPath() %>/notifications">
                <label>
                    <span>类型</span>
                    <select name="type">
                        <option value="ALL">全部</option>
                        <option value="TRANSACTION" <%= "TRANSACTION".equals(selectedType) ? "selected" : "" %>>交易提醒</option>
                        <option value="RISK" <%= "RISK".equals(selectedType) ? "selected" : "" %>>风控提醒</option>
                        <option value="WEALTH" <%= "WEALTH".equals(selectedType) ? "selected" : "" %>>理财提醒</option>
                        <option value="SECURITY" <%= "SECURITY".equals(selectedType) ? "selected" : "" %>>安全提醒</option>
                        <option value="SERVICE" <%= "SERVICE".equals(selectedType) ? "selected" : "" %>>服务工单</option>
                        <option value="SYSTEM" <%= "SYSTEM".equals(selectedType) ? "selected" : "" %>>系统通知</option>
                    </select>
                </label>
                <button class="button primary full" type="submit">筛选</button>
            </form>
        </aside>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>站内事件流</h2>
                <p class="section-note">未读消息会以高亮状态展示，关联业务编号保留用于追踪。</p>
            </div>
        </div>
        <% if (notifications == null || notifications.isEmpty()) { %>
            <div class="human-empty">
                <strong>现在没有新的站内通知。</strong>
                <p>完成交易、缴费、理财申购或提交服务工单后，系统会把结果和需要关注的下一步放到这里。</p>
            </div>
        <% } else { %>
            <div class="event-feed">
                <% for (Notification notification : notifications) { %>
                    <article class="event-card <%= notification.isReadFlag() ? "" : "unread" %>">
                        <div class="event-card-main">
                            <div class="notification-meta">
                                <span class="tag"><%= HtmlUtil.escape(typeName(notification.getNotificationType())) %></span>
                                <% if (!notification.isReadFlag()) { %>
                                    <span class="direction direction-out">未读</span>
                                <% } else { %>
                                    <span class="status">已读</span>
                                <% } %>
                                <span class="muted"><%= notification.getCreatedAt() == null ? "" : notification.getCreatedAt().toString().substring(0, 19) %></span>
                            </div>
                            <strong><%= HtmlUtil.escape(notification.getTitle()) %></strong>
                            <p><%= HtmlUtil.escape(notification.getContent()) %></p>
                            <% if (notification.getBusinessId() != null && notification.getBusinessId().length() > 0) { %>
                                <p class="cell-note">关联业务：<%= HtmlUtil.escape(notification.getBusinessType()) %> / <%= HtmlUtil.escape(notification.getBusinessId()) %></p>
                            <% } %>
                        </div>
                        <% if (!notification.isReadFlag()) { %>
                            <form method="post" action="<%= request.getContextPath() %>/notifications/read">
                                <input type="hidden" name="notificationId" value="<%= notification.getNotificationId() %>">
                                <button class="button primary compact" type="submit">标记已读</button>
                            </form>
                        <% } %>
                    </article>
                <% } %>
            </div>
        <% } %>
    </section>
</main>
</body>
</html>
