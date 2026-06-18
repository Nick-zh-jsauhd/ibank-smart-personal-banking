<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.AdjustmentRequest,com.bank.bean.ServiceTicket,com.bank.bean.TicketActionLog,com.bank.bean.TicketReply,com.bank.dto.AdminSessionUser,com.bank.dto.TicketDetail,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.sql.Timestamp,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String statusName(String status) {
        if ("SUBMITTED".equals(status)) return "待受理";
        if ("ACCEPTED".equals(status)) return "已受理";
        if ("INVESTIGATING".equals(status)) return "调查中";
        if ("WAITING_CUSTOMER".equals(status)) return "待客户补充";
        if ("RESOLVED".equals(status)) return "已处理";
        if ("CLOSED".equals(status)) return "已关闭";
        if ("REOPENED".equals(status)) return "客户重开";
        if ("REJECTED".equals(status)) return "不予受理";
        return status == null ? "" : status;
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

    private String roleName(String roleCode) {
        if ("CUSTOMER_OPERATOR".equals(roleCode)) return "客户运营";
        if ("RISK_OPERATOR".equals(roleCode)) return "风控运营";
        if ("RISK_MANAGER".equals(roleCode)) return "风控管理";
        if ("ACCOUNTING_OPERATOR".equals(roleCode)) return "账务运营";
        if ("ACCOUNTING_REVIEWER".equals(roleCode)) return "账务复核";
        if ("PRODUCT_MANAGER".equals(roleCode)) return "理财产品";
        return roleCode == null ? "" : roleCode;
    }

    private String senderName(TicketReply reply) {
        if ("CUSTOMER".equals(reply.getSenderType())) return "客户";
        if ("ADMIN".equals(reply.getSenderType())) return "后台";
        return reply.getSenderType() == null ? "" : reply.getSenderType();
    }

    private String adjustmentStatusName(String status) {
        if ("PENDING_REVIEW".equals(status)) return "待复核";
        if ("APPROVED".equals(status)) return "复核通过";
        if ("REJECTED".equals(status)) return "已驳回";
        if ("EXECUTED".equals(status)) return "已执行";
        if ("FAILED".equals(status)) return "执行失败";
        return status == null ? "" : status;
    }

    private String directionName(String direction) {
        if ("INCREASE".equals(direction)) return "调增";
        if ("DECREASE".equals(direction)) return "调减";
        return direction == null ? "" : direction;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    TicketDetail detail = (TicketDetail) request.getAttribute("detail");
    ServiceTicket ticket = detail == null ? null : detail.getTicket();
    List<AdjustmentRequest> adjustmentRequests = detail == null ? null : detail.getAdjustmentRequests();
    List<TicketReply> replies = detail == null ? null : detail.getReplies();
    List<TicketActionLog> actionLogs = detail == null ? null : detail.getActionLogs();
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    boolean terminal = ticket != null && ("CLOSED".equals(ticket.getStatus()) || "REJECTED".equals(ticket.getStatus()));
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>工单详情 - iBank Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout layout-wide">
    <section class="page-heading">
        <p class="eyebrow">工单详情</p>
        <h1>工单处理</h1>
        <p class="muted">后台处理动作会同步给客户通知，并自动更新后台待办状态。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (ticket != null) { %>
        <section class="summary-strip">
            <article class="summary-item">
                <span>工单状态</span>
                <strong><%= HtmlUtil.escape(statusName(ticket.getStatus())) %></strong>
            </article>
            <article class="summary-item">
                <span>优先级</span>
                <strong><%= HtmlUtil.escape(ticket.getPriority()) %></strong>
            </article>
            <article class="summary-item">
                <span>责任角色</span>
                <strong><%= HtmlUtil.escape(roleName(ticket.getAssignedRoleCode())) %></strong>
            </article>
            <article class="summary-item">
                <span>客户</span>
                <strong><%= HtmlUtil.escape(ticket.getCustomerName()) %></strong>
            </article>
        </section>

        <div class="detail-workspace">
        <div class="detail-main">
        <section class="content-section emphasis">
            <div class="section-title">
                <div>
                    <h2><%= HtmlUtil.escape(ticket.getTitle()) %></h2>
                    <p class="section-note"><%= HtmlUtil.escape(ticket.getTicketNo()) %> · <%= HtmlUtil.escape(typeName(ticket.getTicketType())) %></p>
                </div>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/tickets">返回列表</a>
            </div>
            <dl class="detail-list">
                <div><dt>状态</dt><dd><span class="status"><%= HtmlUtil.escape(statusName(ticket.getStatus())) %></span></dd></div>
                <div><dt>客户</dt><dd><%= HtmlUtil.escape(ticket.getCustomerName()) %> / <%= HtmlUtil.escape(ticket.getPhone()) %></dd></div>
                <div><dt>优先级</dt><dd><%= HtmlUtil.escape(ticket.getPriority()) %></dd></div>
                <div><dt>责任角色</dt><dd><%= HtmlUtil.escape(roleName(ticket.getAssignedRoleCode())) %></dd></div>
                <div><dt>处理人</dt><dd><%= HtmlUtil.escape(ticket.getAssignedAdminUsername()) %></dd></div>
                <div><dt>关联业务</dt><dd><%= HtmlUtil.escape(ticket.getRelatedBusinessType()) %> <%= HtmlUtil.escape(ticket.getRelatedBusinessId()) %></dd></div>
                <div><dt>创建时间</dt><dd><%= timeText(ticket.getCreatedAt()) %></dd></div>
                <div><dt>更新时间</dt><dd><%= timeText(ticket.getUpdatedAt()) %></dd></div>
                <div><dt>问题描述</dt><dd><%= HtmlUtil.escape(ticket.getDescription()) %></dd></div>
            </dl>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>关联调账申请</h2>
                    <p class="section-note">交易争议工单可以直接发起调账，复核和执行结果会自动回写工单。</p>
                </div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>申请编号</th>
                        <th>方向</th>
                        <th>金额</th>
                        <th>状态</th>
                        <th>申请人</th>
                        <th>操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (adjustmentRequests == null || adjustmentRequests.isEmpty()) { %>
                        <tr><td colspan="6" class="empty">暂无关联调账申请</td></tr>
                    <% } else {
                        for (AdjustmentRequest adjustment : adjustmentRequests) {
                    %>
                        <tr>
                            <td><%= HtmlUtil.escape(adjustment.getAdjustmentNo()) %></td>
                            <td><%= HtmlUtil.escape(directionName(adjustment.getDirection())) %></td>
                            <td>¥ <%= moneyText(adjustment.getAmount()) %></td>
                            <td><span class="status"><%= HtmlUtil.escape(adjustmentStatusName(adjustment.getStatus())) %></span></td>
                            <td><%= HtmlUtil.escape(adjustment.getApplicantUsername()) %></td>
                            <td><a class="button secondary compact" href="<%= request.getContextPath() %>/admin/adjustment/detail?adjustmentId=<%= adjustment.getAdjustmentId() %>">查看</a></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>

        </div>
        <aside class="detail-side">
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>处理提示</h2>
                        <p class="section-note">先确认工单诉求，再选择受理、调查、调账或转派；关键动作会同步通知客户并写入审计。</p>
                    </div>
                </div>
                <div class="section-actions">
                    <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/tickets">返回工单</a>
                    <% if (ticket.getRelatedBusinessId() != null && ticket.getRelatedBusinessId().length() > 0) { %>
                        <span class="tag"><%= HtmlUtil.escape(ticket.getRelatedBusinessId()) %></span>
                    <% } %>
                </div>
            </section>

        <% if (adminUser.hasPermission("ADJUSTMENT_CREATE") && adminUser.hasPermission("TICKET_HANDLE")
                && !terminal && ("TRANSACTION_DISPUTE".equals(ticket.getTicketType())
                || "ADJUSTMENT_INQUIRY".equals(ticket.getTicketType()))) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>从工单发起调账</h2>
                        <p class="section-note">用于交易争议确认需要补账或扣回时，申请会进入账务复核流程。</p>
                    </div>
                </div>
                <form class="form grid-form" method="post" action="<%= request.getContextPath() %>/admin/ticket/adjustment">
                    <input type="hidden" name="ticketId" value="<%= ticket.getTicketId() %>">
                    <label>
                        <span>调账账号</span>
                        <input name="accountNo" maxlength="32" placeholder="客户账号" required>
                    </label>
                    <label>
                        <span>方向</span>
                        <select name="direction">
                            <option value="INCREASE">调增</option>
                            <option value="DECREASE">调减</option>
                        </select>
                    </label>
                    <label>
                        <span>金额</span>
                        <input name="amount" maxlength="18" placeholder="0.00" required>
                    </label>
                    <label class="full-row">
                        <span>调账原因</span>
                        <textarea name="reason" rows="3" maxlength="500" required>由服务工单 <%= HtmlUtil.escape(ticket.getTicketNo()) %> 发起交易争议处置</textarea>
                    </label>
                    <label class="full-row">
                        <span>业务依据</span>
                        <textarea name="evidence" rows="3" maxlength="500" required>客户工单描述：<%= HtmlUtil.escape(ticket.getDescription()) %></textarea>
                    </label>
                    <div class="form-actions full-row">
                        <button class="button primary" type="submit">提交调账申请</button>
                    </div>
                </form>
            </section>
        <% } %>

        <% if (adminUser.hasPermission("TICKET_HANDLE") && !terminal) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>处理动作</h2>
                        <p class="section-note">要求客户补充、解决和驳回会把回复内容同步给客户。</p>
                    </div>
                </div>
                <form class="form grid-form" method="post" action="<%= request.getContextPath() %>/admin/ticket/detail">
                    <input type="hidden" name="ticketId" value="<%= ticket.getTicketId() %>">
                    <label>
                        <span>动作</span>
                        <select name="action">
                            <option value="ACCEPT">受理</option>
                            <option value="INVESTIGATE">进入调查</option>
                            <option value="WAIT_CUSTOMER">要求客户补充</option>
                            <option value="RESOLVE">给出处理结果</option>
                            <option value="REJECT">不予受理</option>
                            <option value="CLOSE">后台关闭</option>
                        </select>
                    </label>
                    <label>
                        <span>内部说明</span>
                        <input name="note" maxlength="500" placeholder="审计留痕说明">
                    </label>
                    <label class="full-row">
                        <span>给客户的回复</span>
                        <textarea name="replyContent" rows="5" maxlength="1000" placeholder="要求补充、解决、驳回时必须填写"></textarea>
                    </label>
                    <div class="form-actions full-row">
                        <button class="button primary" type="submit">提交处理</button>
                    </div>
                </form>
            </section>
        <% } %>

        <% if (adminUser.hasPermission("TICKET_ASSIGN")) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>转派责任角色</h2>
                        <p class="section-note">当工单需要其他团队处理时，转派会生成新的后台待办。</p>
                    </div>
                </div>
                <form class="filter-form two-col" method="post" action="<%= request.getContextPath() %>/admin/ticket/detail">
                    <input type="hidden" name="ticketId" value="<%= ticket.getTicketId() %>">
                    <input type="hidden" name="action" value="ASSIGN">
                    <label>
                        <span>目标角色</span>
                        <select name="assignedRoleCode">
                            <option value="CUSTOMER_OPERATOR">客户运营</option>
                            <option value="RISK_OPERATOR">风控运营</option>
                            <option value="ACCOUNTING_OPERATOR">账务运营</option>
                            <option value="ACCOUNTING_REVIEWER">账务复核</option>
                            <option value="PRODUCT_MANAGER">理财产品</option>
                        </select>
                    </label>
                    <div class="filter-actions">
                        <button class="button secondary" type="submit">转派</button>
                    </div>
                </form>
            </section>
        <% } %>

        </aside>
        </div>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>沟通记录</h2>
                    <p class="section-note">客户原始诉求、补充材料和后台回复。</p>
                </div>
            </div>
            <div class="conversation-list">
                <% if (replies == null || replies.isEmpty()) { %>
                    <p class="empty">暂无沟通记录</p>
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
                    <p class="section-note">用于客服质检、审计追踪和争议复盘。</p>
                </div>
            </div>
            <div class="timeline">
                <% if (actionLogs == null || actionLogs.isEmpty()) { %>
                    <p class="empty">暂无处理轨迹</p>
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
    <% } %>
</main>
</body>
</html>
