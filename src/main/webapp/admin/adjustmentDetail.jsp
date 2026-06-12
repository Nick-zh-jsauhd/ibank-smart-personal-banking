<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.AdjustmentActionLog,com.bank.bean.AdjustmentRequest,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String statusName(String status) {
        if ("PENDING_REVIEW".equals(status)) return "待复核";
        if ("APPROVED".equals(status)) return "复核通过";
        if ("REJECTED".equals(status)) return "已驳回";
        if ("EXECUTED".equals(status)) return "已执行";
        if ("FAILED".equals(status)) return "执行失败";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("REJECTED".equals(status) || "FAILED".equals(status)) return "direction direction-out";
        if ("EXECUTED".equals(status)) return "status";
        return "tag";
    }

    private String directionName(String direction) {
        if ("INCREASE".equals(direction)) return "调增";
        if ("DECREASE".equals(direction)) return "调减";
        return direction == null ? "" : direction;
    }

    private boolean fromServiceTicket(AdjustmentRequest adjustment) {
        return adjustment != null
                && "SERVICE_TICKET".equals(adjustment.getSourceType())
                && adjustment.getSourceTicketId() != null;
    }

    private String sourceName(AdjustmentRequest adjustment) {
        if (fromServiceTicket(adjustment)) {
            return "服务工单 #" + adjustment.getSourceTicketId();
        }
        if (adjustment != null && adjustment.getReconciliationItemId() != null) {
            String businessId = adjustment.getReconciliationBusinessId() == null ? ""
                    : " " + adjustment.getReconciliationBusinessId();
            return "对账异常 #" + adjustment.getReconciliationItemId() + businessId;
        }
        return "未记录";
    }

    private String actionTypeName(String actionType) {
        if ("CREATE".equals(actionType)) return "创建申请";
        if ("REVIEW".equals(actionType)) return "复核";
        if ("EXECUTE".equals(actionType)) return "执行调账";
        return actionType == null ? "" : actionType;
    }

    private String timeText(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdjustmentRequest adjustment = (AdjustmentRequest) request.getAttribute("adjustment");
    List<AdjustmentActionLog> actionLogs = (List<AdjustmentActionLog>) request.getAttribute("actionLogs");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>调账申请详情 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout layout-wide">
    <section class="page-heading">
        <p class="eyebrow">Adjustment Detail</p>
        <h1>调账申请详情</h1>
        <p class="muted">复核与执行分离，申请人不能复核或执行自己的申请。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (adjustment == null) { %>
        <section class="quick-actions">
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/adjustments">返回调账申请</a>
        </section>
    <% } else { %>
        <section class="summary-strip">
            <article class="summary-item">
                <span>状态</span>
                <strong><%= statusName(adjustment.getStatus()) %></strong>
            </article>
            <article class="summary-item">
                <span>调账方向</span>
                <strong><%= directionName(adjustment.getDirection()) %></strong>
            </article>
            <article class="summary-item">
                <span>调账金额</span>
                <strong>¥ <%= moneyText(adjustment.getAmount()) %></strong>
            </article>
            <article class="summary-item">
                <span>来源</span>
                <strong><%= HtmlUtil.escape(sourceName(adjustment)) %></strong>
            </article>
        </section>

        <div class="detail-workspace">
        <div class="detail-main">
        <section class="content-section emphasis">
            <div class="section-title">
                <div>
                    <h2>申请信息</h2>
                    <p class="section-note"><%= HtmlUtil.escape(adjustment.getAdjustmentNo()) %></p>
                </div>
                <span class="<%= statusClass(adjustment.getStatus()) %>"><%= statusName(adjustment.getStatus()) %></span>
            </div>
            <dl class="detail-list">
                <div><dt>客户</dt><dd><%= HtmlUtil.escape(adjustment.getCustomerName()) %></dd></div>
                <div><dt>账户</dt><dd><%= HtmlUtil.escape(adjustment.getAccountNo()) %></dd></div>
                <div><dt>来源</dt><dd><%= HtmlUtil.escape(sourceName(adjustment)) %></dd></div>
                <div><dt>申请人</dt><dd><%= HtmlUtil.escape(adjustment.getApplicantUsername()) %></dd></div>
                <div><dt>复核人</dt><dd><%= adjustment.getReviewerUsername() == null ? "未复核" : HtmlUtil.escape(adjustment.getReviewerUsername()) %></dd></div>
                <div><dt>复核意见</dt><dd><%= HtmlUtil.escape(adjustment.getReviewNote()) %></dd></div>
                <div><dt>调账原因</dt><dd><%= HtmlUtil.escape(adjustment.getReason()) %></dd></div>
                <div><dt>业务依据</dt><dd><%= HtmlUtil.escape(adjustment.getEvidence()) %></dd></div>
                <div><dt>执行交易</dt><dd><%= adjustment.getExecutedTransactionId() == null ? "未执行" : "#" + adjustment.getExecutedTransactionId() %></dd></div>
                <div><dt>执行流水</dt><dd><%= adjustment.getExecutedLedgerId() == null ? "未执行" : "#" + adjustment.getExecutedLedgerId() %></dd></div>
                <div><dt>创建时间</dt><dd><%= timeText(adjustment.getCreatedAt()) %></dd></div>
                <div><dt>执行时间</dt><dd><%= timeText(adjustment.getExecutedAt()) %></dd></div>
            </dl>
        </section>

        </div>
        <aside class="detail-side">
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>流程控制</h2>
                        <p class="section-note">调账必须经过申请、复核、执行三步；申请人不能复核或执行自己的申请。</p>
                    </div>
                </div>
                <div class="section-actions">
                    <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/adjustments">返回列表</a>
                    <% if (fromServiceTicket(adjustment)) { %>
                        <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/ticket/detail?ticketId=<%= adjustment.getSourceTicketId() %>">来源工单</a>
                    <% } %>
                </div>
            </section>

        <% if ("PENDING_REVIEW".equals(adjustment.getStatus()) && adminUser.hasPermission("ADJUSTMENT_REVIEW")) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>复核操作</h2>
                        <p class="section-note">复核人不能是申请人；驳回后不能执行调账。</p>
                    </div>
                </div>
                <form method="post" action="<%= request.getContextPath() %>/admin/adjustment/detail" class="form grid-form">
                    <input type="hidden" name="adjustmentId" value="<%= adjustment.getAdjustmentId() %>">
                    <input type="hidden" name="action" value="review">
                    <label>
                        <span>复核结论</span>
                        <select name="decision">
                            <option value="APPROVE">复核通过</option>
                            <option value="REJECT">驳回申请</option>
                        </select>
                    </label>
                    <label class="full-row">
                        <span>复核意见</span>
                        <textarea name="note" rows="3" maxlength="500" placeholder="记录复核依据，至少 5 个字"></textarea>
                    </label>
                    <div class="full-row action-row">
                        <button class="button primary" type="submit">提交复核</button>
                    </div>
                </form>
            </section>
        <% } %>

        <% if ("APPROVED".equals(adjustment.getStatus()) && adminUser.hasPermission("ADJUSTMENT_EXECUTE")) { %>
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>执行调账</h2>
                        <p class="section-note">执行后系统会更新账户余额，写入交易表和流水表，并同步回写来源业务状态。</p>
                    </div>
                </div>
                <form method="post" action="<%= request.getContextPath() %>/admin/adjustment/detail" class="action-row">
                    <input type="hidden" name="adjustmentId" value="<%= adjustment.getAdjustmentId() %>">
                    <input type="hidden" name="action" value="execute">
                    <button class="button primary" type="submit">执行调账</button>
                </form>
            </section>
        <% } %>

        </aside>
        </div>

        <section class="content-section">
            <div class="section-title">
                <h2>操作记录</h2>
                <div class="section-actions">
                    <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/adjustments">返回列表</a>
                    <% if (fromServiceTicket(adjustment)) { %>
                        <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/ticket/detail?ticketId=<%= adjustment.getSourceTicketId() %>">查看来源工单</a>
                    <% } else if (adjustment.getReconciliationItemId() != null) { %>
                        <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/reconciliation/item/detail?itemId=<%= adjustment.getReconciliationItemId() %>">查看来源异常</a>
                    <% } %>
                </div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>管理员</th>
                        <th>动作</th>
                        <th>前状态</th>
                        <th>后状态</th>
                        <th>备注</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (actionLogs == null || actionLogs.isEmpty()) { %>
                        <tr><td colspan="6" class="empty">暂无操作记录</td></tr>
                    <% } else {
                        for (AdjustmentActionLog log : actionLogs) {
                    %>
                        <tr>
                            <td><%= timeText(log.getCreatedAt()) %></td>
                            <td><%= HtmlUtil.escape(log.getAdminUsername()) %></td>
                            <td><%= actionTypeName(log.getActionType()) %></td>
                            <td><%= statusName(log.getBeforeStatus()) %></td>
                            <td><%= statusName(log.getAfterStatus()) %></td>
                            <td><%= HtmlUtil.escape(log.getNote()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>
    <% } %>
</main>
</body>
</html>

