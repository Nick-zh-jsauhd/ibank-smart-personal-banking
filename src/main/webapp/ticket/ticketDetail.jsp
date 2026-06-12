<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ServiceTicket,com.bank.bean.TicketActionLog,com.bank.bean.TicketReply,com.bank.dto.TicketDetail,com.bank.util.HtmlUtil,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
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

    private String senderName(TicketReply reply) {
        if ("CUSTOMER".equals(reply.getSenderType())) return "客户";
        if ("ADMIN".equals(reply.getSenderType())) return "后台";
        return reply.getSenderType() == null ? "" : reply.getSenderType();
    }
%>
<%
    TicketDetail detail = (TicketDetail) request.getAttribute("detail");
    ServiceTicket ticket = detail == null ? null : detail.getTicket();
    List<TicketReply> replies = detail == null ? null : detail.getReplies();
    List<TicketActionLog> actionLogs = detail == null ? null : detail.getActionLogs();
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    request.setAttribute("activeNav", "service");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>工单详情 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Ticket Detail</p>
            <h1>工单详情</h1>
            <p class="muted">查看处理进展，并按后台要求补充资料或确认关闭。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/tickets">返回列表</a>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (ticket == null) { %>
        <section class="content-section">
            <div class="human-empty">
                <strong>未找到工单详情。</strong>
                <p>可能工单编号已失效或没有访问权限，请返回服务中心重新选择。</p>
            </div>
        </section>
    <% } else { %>
        <div class="care-note">
            <div>
                <strong>工单的关键是“当前状态”和“下一步动作”。</strong>
                <p>如果状态是待补充，请在右侧追加说明；如果已处理但仍有疑问，可以重新打开继续跟进。</p>
            </div>
        </div>

        <section class="summary-strip">
            <article class="summary-item">
                <span>当前状态</span>
                <strong><%= HtmlUtil.escape(statusName(ticket.getStatus())) %></strong>
            </article>
            <article class="summary-item">
                <span>优先级</span>
                <strong><%= HtmlUtil.escape(ticket.getPriority()) %></strong>
            </article>
            <article class="summary-item">
                <span>关联业务</span>
                <strong><%= HtmlUtil.escape(ticket.getRelatedBusinessType()) %> <%= HtmlUtil.escape(ticket.getRelatedBusinessId()) %></strong>
            </article>
            <article class="summary-item">
                <span>更新时间</span>
                <strong><%= timeText(ticket.getUpdatedAt()) %></strong>
            </article>
        </section>

        <section class="detail-workspace">
            <div class="detail-main">
                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2><%= HtmlUtil.escape(ticket.getTitle()) %></h2>
                            <p class="section-note"><%= HtmlUtil.escape(ticket.getTicketNo()) %></p>
                        </div>
                        <span class="status"><%= HtmlUtil.escape(statusName(ticket.getStatus())) %></span>
                    </div>
                    <dl class="detail-list">
                        <div><dt>创建时间</dt><dd><%= timeText(ticket.getCreatedAt()) %></dd></div>
                        <div><dt>更新时间</dt><dd><%= timeText(ticket.getUpdatedAt()) %></dd></div>
                        <div><dt>问题描述</dt><dd><%= HtmlUtil.escape(ticket.getDescription()) %></dd></div>
                    </dl>
                </section>

                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2>沟通记录</h2>
                            <p class="section-note">客户补充和后台回复都会保留在这里。</p>
                        </div>
                    </div>
                    <div class="conversation-list">
                        <% if (replies == null || replies.isEmpty()) { %>
                            <div class="human-empty">
                                <strong>还没有沟通记录。</strong>
                                <p>后台受理或你补充说明后，双方沟通内容会保留在这里。</p>
                            </div>
                        <% } else {
                            for (TicketReply reply : replies) {
                        %>
                            <article class="conversation-item <%= "ADMIN".equals(reply.getSenderType()) ? "admin-reply" : "customer-reply" %>">
                                <div class="conversation-meta">
                                    <strong><%= HtmlUtil.escape(senderName(reply)) %></strong>
                                    <span><%= timeText(reply.getCreatedAt()) %></span>
                                </div>
                                <p><%= HtmlUtil.escape(reply.getContent()) %></p>
                            </article>
                        <%  }
                        } %>
                    </div>
                </section>

                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2>处理轨迹</h2>
                            <p class="section-note">关键状态流转会形成不可变轨迹。</p>
                        </div>
                    </div>
                    <div class="timeline">
                        <% if (actionLogs == null || actionLogs.isEmpty()) { %>
                            <div class="human-empty">
                                <strong>还没有处理轨迹。</strong>
                                <p>受理、调查、待补充、处理完成等关键状态会形成时间线。</p>
                            </div>
                        <% } else {
                            for (TicketActionLog log : actionLogs) {
                        %>
                            <div class="timeline-item">
                                <strong><%= HtmlUtil.escape(log.getActionType()) %></strong>
                                <span><%= timeText(log.getCreatedAt()) %></span>
                                <p><%= HtmlUtil.escape(statusName(log.getBeforeStatus())) %> → <%= HtmlUtil.escape(statusName(log.getAfterStatus())) %>，<%= HtmlUtil.escape(log.getNote()) %></p>
                            </div>
                        <%  }
                        } %>
                    </div>
                </section>
            </div>

            <aside class="detail-side">
                <% if (!"CLOSED".equals(ticket.getStatus()) && !"REJECTED".equals(ticket.getStatus())) { %>
                    <section class="content-section">
                        <div class="section-title">
                            <div>
                                <h2>补充说明</h2>
                                <p class="section-note">补充资料会同步生成后台待办，便于处理人员继续跟进。</p>
                            </div>
                        </div>
                        <form class="form" method="post" action="<%= request.getContextPath() %>/ticket/detail">
                            <input type="hidden" name="ticketId" value="<%= ticket.getTicketId() %>">
                            <input type="hidden" name="action" value="reply">
                            <label>
                                <span>补充内容</span>
                                <textarea name="content" rows="5" maxlength="1000" required></textarea>
                            </label>
                            <div class="form-actions">
                                <button class="button primary" type="submit">提交补充</button>
                            </div>
                        </form>
                    </section>
                <% } %>

                <% if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) { %>
                    <section class="content-section">
                        <div class="section-title">
                            <div>
                                <h2>结果确认</h2>
                                <p class="section-note">已解决可以确认关闭；仍有问题可以重新打开并说明原因。</p>
                            </div>
                        </div>
                        <form class="form" method="post" action="<%= request.getContextPath() %>/ticket/detail">
                            <input type="hidden" name="ticketId" value="<%= ticket.getTicketId() %>">
                            <label>
                                <span>说明</span>
                                <textarea name="note" rows="4" maxlength="500"></textarea>
                            </label>
                            <div class="form-actions">
                                <% if ("RESOLVED".equals(ticket.getStatus())) { %>
                                    <button class="button primary" type="submit" name="action" value="close">确认关闭</button>
                                <% } %>
                                <button class="button secondary" type="submit" name="action" value="reopen">重新打开</button>
                            </div>
                        </form>
                    </section>
                <% } %>

                <section class="content-section">
                    <h2>处理说明</h2>
                    <ul class="task-checklist">
                        <li>后台回复后会同步进入通知中心。</li>
                        <li>需要客户补充材料时，工单会进入待补充状态。</li>
                        <li>确认关闭后仍保留全部沟通记录和状态轨迹。</li>
                    </ul>
                </section>
            </aside>
        </section>
    <% } %>
</main>
</body>
</html>
