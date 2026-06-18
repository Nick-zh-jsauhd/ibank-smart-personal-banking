<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.ReconciliationItem,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil" %>
<%!
    private String statusName(String status) {
        if ("CONFIRMED_EXCEPTION".equals(status)) return "确认异常";
        if ("OPEN".equals(status)) return "待处理";
        if ("INVESTIGATING".equals(status)) return "处理中";
        if ("FIXED".equals(status)) return "已修复";
        if ("CLOSED".equals(status)) return "已关闭";
        return status == null ? "" : status;
    }

    private String checkTypeName(String checkType) {
        if ("TRANSACTION_LEDGER".equals(checkType)) return "交易流水";
        if ("ACCOUNT_BALANCE".equals(checkType)) return "账户余额";
        if ("WEALTH_BUY".equals(checkType)) return "理财申购";
        if ("WEALTH_REDEEM".equals(checkType)) return "理财赎回";
        if ("RISK_BLOCK".equals(checkType)) return "风控拦截";
        return checkType == null ? "" : checkType;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    ReconciliationItem item = (ReconciliationItem) request.getAttribute("item");
    String error = (String) request.getAttribute("error");
    String accountNo = (String) request.getAttribute("accountNo");
    String suggestedAccountNo = (String) request.getAttribute("suggestedAccountNo");
    String direction = (String) request.getAttribute("direction");
    String amount = (String) request.getAttribute("amount");
    String reason = (String) request.getAttribute("reason");
    String evidence = (String) request.getAttribute("evidence");
    if ((accountNo == null || accountNo.length() == 0) && suggestedAccountNo != null) {
        accountNo = suggestedAccountNo;
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>创建调账申请 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">创建调账</p>
        <h1>创建调账申请</h1>
        <p class="muted">调账申请只记录意图，不会立即修改账户余额。资金变动必须经过复核和执行。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (item == null) { %>
        <section class="quick-actions">
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/reconciliation/items">返回异常处理中心</a>
        </section>
    <% } else { %>
        <section class="content-section filter-section">
            <div class="section-title">
                <div>
                    <h2>关联异常</h2>
                    <p class="section-note">只有“确认异常”的对账事项可以发起调账申请。</p>
                </div>
                <span class="tag"><%= statusName(item.getStatus()) %></span>
            </div>
            <dl class="detail-list">
                <div><dt>异常号</dt><dd>#<%= item.getItemId() %></dd></div>
                <div><dt>检查类型</dt><dd><%= HtmlUtil.escape(checkTypeName(item.getCheckType())) %></dd></div>
                <div><dt>业务标识</dt><dd><%= HtmlUtil.escape(item.getBusinessId()) %></dd></div>
                <div><dt>期望值</dt><dd><%= HtmlUtil.escape(item.getExpectedValue()) %></dd></div>
                <div><dt>实际值</dt><dd><%= HtmlUtil.escape(item.getActualValue()) %></dd></div>
                <div><dt>异常说明</dt><dd><%= HtmlUtil.escape(item.getDescription()) %></dd></div>
            </dl>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>调账信息</h2>
                    <p class="section-note">单笔调账金额上限为 100,000.00；调减会在执行时校验账户余额。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/admin/adjustment/create" class="form grid-form">
                <input type="hidden" name="itemId" value="<%= item.getItemId() %>">
                <label>
                    <span>调账账户号</span>
                    <input name="accountNo" value="<%= HtmlUtil.escape(accountNo) %>" placeholder="输入需要调账的账户号">
                </label>
                <label>
                    <span>调账方向</span>
                    <select name="direction">
                        <option value="INCREASE" <%= "INCREASE".equals(direction) ? "selected" : "" %>>调增余额</option>
                        <option value="DECREASE" <%= "DECREASE".equals(direction) ? "selected" : "" %>>调减余额</option>
                    </select>
                </label>
                <label>
                    <span>调账金额</span>
                    <input name="amount" value="<%= HtmlUtil.escape(amount) %>" placeholder="例如 100.00">
                </label>
                <label class="full-row">
                    <span>调账原因</span>
                    <textarea name="reason" rows="3" maxlength="500" placeholder="说明为什么需要调账，至少 5 个字"><%= HtmlUtil.escape(reason) %></textarea>
                </label>
                <label class="full-row">
                    <span>业务依据</span>
                    <textarea name="evidence" rows="3" maxlength="500" placeholder="记录核查依据、对账明细、审批依据等，至少 5 个字"><%= HtmlUtil.escape(evidence) %></textarea>
                </label>
                <div class="full-row action-row">
                    <a class="button secondary" href="<%= request.getContextPath() %>/admin/reconciliation/item/detail?itemId=<%= item.getItemId() %>">返回异常详情</a>
                    <% if (adminUser.hasPermission("ADJUSTMENT_CREATE")) { %>
                    <button class="button primary" type="submit">提交申请</button>
                    <% } %>
                </div>
            </form>
        </section>
    <% } %>
</main>
</body>
</html>

