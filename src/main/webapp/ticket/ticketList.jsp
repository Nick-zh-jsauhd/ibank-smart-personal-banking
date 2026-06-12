<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ServiceTicket,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String typeName(String type) {
        if ("TRANSACTION_DISPUTE".equals(type)) return "交易争议";
        if ("RISK_APPEAL".equals(type)) return "风控申诉";
        if ("ACCOUNT_SERVICE".equals(type)) return "账户服务";
        if ("WEALTH_SERVICE".equals(type)) return "理财服务";
        if ("ADJUSTMENT_INQUIRY".equals(type)) return "调账咨询";
        if ("GENERAL".equals(type)) return "其他问题";
        return type == null ? "" : type;
    }

    private String statusName(String status) {
        if ("SUBMITTED".equals(status)) return "已提交";
        if ("ACCEPTED".equals(status)) return "已受理";
        if ("INVESTIGATING".equals(status)) return "调查中";
        if ("WAITING_CUSTOMER".equals(status)) return "待补充";
        if ("RESOLVED".equals(status)) return "已处理";
        if ("CLOSED".equals(status)) return "已关闭";
        if ("REOPENED".equals(status)) return "已重开";
        if ("REJECTED".equals(status)) return "不予受理";
        return status == null ? "" : status;
    }

    private String priorityName(String priority) {
        if ("URGENT".equals(priority)) return "紧急";
        if ("HIGH".equals(priority)) return "高";
        if ("NORMAL".equals(priority)) return "普通";
        if ("LOW".equals(priority)) return "低";
        return priority == null ? "" : priority;
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<ServiceTicket> tickets = (List<ServiceTicket>) request.getAttribute("tickets");
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    int activeCount = 0;
    int waitingCount = 0;
    int resolvedCount = 0;
    if (tickets != null) {
        for (ServiceTicket ticket : tickets) {
            if ("WAITING_CUSTOMER".equals(ticket.getStatus())) {
                waitingCount++;
            } else if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
                resolvedCount++;
            } else {
                activeCount++;
            }
        }
    }
    request.setAttribute("activeNav", "service");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>服务工单 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Service Center</p>
            <h1>服务中心</h1>
            <p class="muted">交易争议、风控申诉、账户或理财问题都在这里提交和跟进。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/notifications?type=SERVICE">服务通知</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/ticket/create">发起工单</a>
        </div>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="photo-band service">
        <div>
            <p class="eyebrow">Service Care</p>
            <h2>问题提交后，会有人接手并留下每一步处理记录。</h2>
            <p class="section-note">交易争议、风控申诉和账户问题都会进入后台待办，处理结果会同步回通知中心。</p>
        </div>
    </section>

    <section class="metric-grid">
        <div class="metric-card"><span>处理中</span><strong><%= activeCount %></strong></div>
        <div class="metric-card"><span>待我补充</span><strong><%= waitingCount %></strong></div>
        <div class="metric-card"><span>已处理/关闭</span><strong><%= resolvedCount %></strong></div>
        <div class="metric-card"><span>总工单</span><strong><%= tickets == null ? 0 : tickets.size() %></strong></div>
    </section>

    <section class="dashboard-columns">
        <article class="content-section">
            <div class="section-title">
                <div>
                    <h2>服务事件流</h2>
                    <p class="section-note">按更新时间排列，优先看状态和下一步动作。</p>
                </div>
            </div>
            <div class="event-feed">
                <% if (tickets == null || tickets.isEmpty()) { %>
                    <div class="human-empty">
                        <strong>现在没有服务工单需要跟进。</strong>
                        <p>遇到交易争议、风控申诉或账户问题时，可以发起工单。提交后这里会显示受理、调查、待补充和处理完成的全过程。</p>
                    </div>
                <% } else {
                    for (ServiceTicket ticket : tickets) {
                %>
                    <a class="event-card warm-event" href="<%= request.getContextPath() %>/ticket/detail?ticketId=<%= ticket.getTicketId() %>">
                        <div class="event-card-main">
                            <div class="notification-meta">
                                <span class="tag"><%= HtmlUtil.escape(typeName(ticket.getTicketType())) %></span>
                                <span class="tag severity-<%= "URGENT".equals(ticket.getPriority()) ? "critical" : ("HIGH".equals(ticket.getPriority()) ? "high" : "info") %>"><%= HtmlUtil.escape(priorityName(ticket.getPriority())) %></span>
                                <span class="muted"><%= timeText(ticket.getUpdatedAt()) %></span>
                            </div>
                            <strong><%= HtmlUtil.escape(ticket.getTitle()) %></strong>
                            <p><%= HtmlUtil.escape(ticket.getDescription()) %></p>
                            <p class="cell-note"><%= HtmlUtil.escape(ticket.getTicketNo()) %> · <%= HtmlUtil.escape(ticket.getRelatedBusinessType()) %> <%= HtmlUtil.escape(ticket.getRelatedBusinessId()) %></p>
                        </div>
                        <span class="status"><%= HtmlUtil.escape(statusName(ticket.getStatus())) %></span>
                    </a>
                <%  }
                } %>
            </div>
        </article>

        <aside class="content-section">
            <div class="section-title">
                <div>
                    <h2>服务路径</h2>
                    <p class="section-note">工单会自动同步后台待办，处理结果再回到客户通知中心。</p>
                </div>
            </div>
            <ul class="task-checklist">
                <li>交易争议：关联交易编号，后台可继续生成调账申请。</li>
                <li>风控申诉：关联被拦截或预警通过的风险事件。</li>
                <li>待我补充：需要客户追加说明后后台继续处理。</li>
                <li>已处理：客户确认后可关闭，仍可在详情中查看轨迹。</li>
            </ul>
            <div class="care-note">
                <div>
                    <strong>下一步怎么看？</strong>
                    <p>优先处理“待我补充”的工单；如果是交易争议，请准备交易编号、金额和你的诉求，后台处理会更快。</p>
                </div>
            </div>
        </aside>
    </section>
</main>
</body>
</html>
