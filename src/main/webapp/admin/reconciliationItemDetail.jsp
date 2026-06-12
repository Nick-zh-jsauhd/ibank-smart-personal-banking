<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ReconciliationActionLog,com.bank.bean.ReconciliationItem,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.util.List" %>
<%!
    private String statusName(String status) {
        if ("OPEN".equals(status)) return "待处理";
        if ("INVESTIGATING".equals(status)) return "处理中";
        if ("CONFIRMED_EXCEPTION".equals(status)) return "确认异常";
        if ("ACCEPTED".equals(status)) return "可接受差异";
        if ("FIXED".equals(status)) return "已修复";
        if ("CLOSED".equals(status)) return "已关闭";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("OPEN".equals(status) || "CONFIRMED_EXCEPTION".equals(status) || "CRITICAL".equals(status)) {
            return "direction direction-out";
        }
        if ("FIXED".equals(status) || "ACCEPTED".equals(status) || "CLOSED".equals(status)) {
            return "status";
        }
        return "tag";
    }

    private String checkTypeName(String checkType) {
        if ("TRANSACTION_LEDGER".equals(checkType)) return "交易流水";
        if ("ACCOUNT_BALANCE".equals(checkType)) return "账户余额";
        if ("WEALTH_BUY".equals(checkType)) return "理财申购";
        if ("WEALTH_REDEEM".equals(checkType)) return "理财赎回";
        if ("RISK_BLOCK".equals(checkType)) return "风控拦截";
        return checkType == null ? "" : checkType;
    }

    private String severityName(String severity) {
        if ("CRITICAL".equals(severity)) return "严重";
        if ("WARN".equals(severity)) return "警告";
        return severity == null ? "" : severity;
    }

    private String actionTypeName(String actionType) {
        if ("STATUS_CHANGE".equals(actionType)) return "状态变更";
        return actionType == null ? "" : actionType;
    }

    private String timeText(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    ReconciliationItem item = (ReconciliationItem) request.getAttribute("item");
    List<ReconciliationActionLog> actionLogs = (List<ReconciliationActionLog>) request.getAttribute("actionLogs");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>对账异常详情 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Exception Handling</p>
        <h1>对账异常详情</h1>
        <p class="muted">记录异常核实过程和处理结论，为后续调账申请或复核流程保留依据。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (item == null) { %>
        <section class="quick-actions">
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/reconciliation/items">返回异常处理中心</a>
        </section>
    <% } else { %>
        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>异常信息</h2>
                    <p class="section-note">批次 #<%= item.getBatchId() %>，异常 #<%= item.getItemId() %></p>
                </div>
                <div class="section-actions">
                    <span class="<%= statusClass(item.getSeverity()) %>"><%= severityName(item.getSeverity()) %></span>
                    <span class="<%= statusClass(item.getStatus()) %>"><%= statusName(item.getStatus()) %></span>
                    <% if ("CONFIRMED_EXCEPTION".equals(item.getStatus()) && adminUser.hasPermission("ADJUSTMENT_CREATE")) { %>
                        <a class="button primary compact"
                           href="<%= request.getContextPath() %>/admin/adjustment/create?itemId=<%= item.getItemId() %>">发起调账申请</a>
                    <% } %>
                </div>
            </div>
            <dl class="detail-list">
                <div><dt>对账日期</dt><dd><%= item.getReconDate() == null ? "" : item.getReconDate().toString() %></dd></div>
                <div><dt>检查类型</dt><dd><%= HtmlUtil.escape(checkTypeName(item.getCheckType())) %></dd></div>
                <div><dt>业务类型</dt><dd><%= HtmlUtil.escape(item.getBusinessType()) %></dd></div>
                <div><dt>业务标识</dt><dd><strong><%= HtmlUtil.escape(item.getBusinessId()) %></strong></dd></div>
                <div><dt>期望值</dt><dd><%= HtmlUtil.escape(item.getExpectedValue()) %></dd></div>
                <div><dt>实际值</dt><dd><%= HtmlUtil.escape(item.getActualValue()) %></dd></div>
                <div><dt>异常说明</dt><dd><%= HtmlUtil.escape(item.getDescription()) %></dd></div>
                <div><dt>当前处理人</dt><dd><%= item.getHandlerUsername() == null ? "未分配" : HtmlUtil.escape(item.getHandlerUsername()) %></dd></div>
                <div><dt>最近结论</dt><dd><%= HtmlUtil.escape(statusName(item.getHandleResult())) %></dd></div>
                <div><dt>最近处理时间</dt><dd><%= timeText(item.getHandledAt()) %></dd></div>
                <div><dt>最近处理说明</dt><dd><%= HtmlUtil.escape(item.getHandleNote()) %></dd></div>
            </dl>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>处理操作</h2>
                    <p class="section-note">这里仅记录人工处置结论；涉及账户余额、流水或持仓修复时，后续应走调账申请和复核。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/admin/reconciliation/item/detail" class="form grid-form">
                <input type="hidden" name="itemId" value="<%= item.getItemId() %>">
                <label>
                    <span>处理动作</span>
                    <select name="targetStatus">
                        <option value="INVESTIGATING">接手处理</option>
                        <option value="CONFIRMED_EXCEPTION">确认异常</option>
                        <option value="ACCEPTED">标记为可接受差异</option>
                        <option value="FIXED">标记为已修复</option>
                        <option value="CLOSED">关闭异常</option>
                    </select>
                </label>
                <label class="full-row">
                    <span>处理说明</span>
                    <textarea name="note" rows="4" maxlength="500" placeholder="记录核查依据、业务判断、修复方式或关闭原因。除接手处理外，其他动作至少填写 5 个字。"></textarea>
                </label>
                <div class="full-row action-row">
                    <a class="button secondary" href="<%= request.getContextPath() %>/admin/reconciliation/items">返回异常处理中心</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/admin/reconciliation/detail?batchId=<%= item.getBatchId() %>">返回批次详情</a>
                    <% if (adminUser.hasPermission("RECONCILIATION_ITEM_HANDLE")) { %>
                    <button class="button primary" type="submit">提交处理</button>
                    <% } %>
                </div>
            </form>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>处理记录</h2></div>
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
                        <tr><td colspan="6" class="empty">暂无处理记录</td></tr>
                    <% } else {
                        for (ReconciliationActionLog log : actionLogs) {
                    %>
                        <tr>
                            <td><%= timeText(log.getCreatedAt()) %></td>
                            <td><%= HtmlUtil.escape(log.getAdminUsername()) %></td>
                            <td><%= HtmlUtil.escape(actionTypeName(log.getActionType())) %></td>
                            <td><%= HtmlUtil.escape(statusName(log.getBeforeStatus())) %></td>
                            <td><%= HtmlUtil.escape(statusName(log.getAfterStatus())) %></td>
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

