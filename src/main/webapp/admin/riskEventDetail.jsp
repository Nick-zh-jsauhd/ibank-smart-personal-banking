<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.bean.Customer,com.bank.bean.RiskActionLog,com.bank.dto.AdminRiskEventDetail,com.bank.dto.AdminRiskEventView,com.bank.dto.AdminSessionUser,com.bank.dto.LedgerEntryView,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String txnTypeName(String txnType) {
        if ("WITHDRAW".equals(txnType)) return "取款";
        if ("TRANSFER_INNER".equals(txnType)) return "本行转账";
        if ("PAYMENT".equals(txnType)) return "生活缴费";
        if ("BUY_WEALTH".equals(txnType)) return "理财申购";
        if ("DEPOSIT".equals(txnType)) return "存款";
        if ("REDEEM_WEALTH".equals(txnType)) return "理财赎回";
        return txnType == null ? "" : txnType;
    }

    private String handleStatusName(String status) {
        if ("PENDING".equals(status)) return "待处理";
        if ("FOLLOW_UP".equals(status)) return "继续跟进";
        if ("HANDLED".equals(status)) return "已处理";
        return status == null ? "" : status;
    }

    private String handleResultName(String result) {
        if ("FALSE_POSITIVE".equals(result)) return "误报";
        if ("CONFIRMED_RISK".equals(result)) return "确认风险";
        if ("CUSTOMER_VERIFIED".equals(result)) return "客户已核实";
        if ("FOLLOW_UP".equals(result)) return "继续跟进";
        return result == null ? "" : result;
    }

    private String actionTypeName(String actionType) {
        if ("HANDLE_EVENT".equals(actionType)) return "事件处置";
        if ("FREEZE_ACCOUNT".equals(actionType)) return "冻结账户";
        if ("UNFREEZE_ACCOUNT".equals(actionType)) return "解冻账户";
        return actionType == null ? "" : actionType;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminRiskEventDetail detail = (AdminRiskEventDetail) request.getAttribute("detail");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    AdminRiskEventView event = detail == null ? null : detail.getEvent();
    Customer customer = detail == null ? null : detail.getCustomer();
    Account account = detail == null ? null : detail.getAccount();
    List<LedgerEntryView> ledgers = detail == null ? null : detail.getRecentLedgers();
    List<RiskActionLog> actionLogs = detail == null ? null : detail.getActionLogs();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>风控事件处置 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Risk Case</p>
        <h1>风控事件处置</h1>
        <p class="muted">查看事件上下文，记录处置结论，并按需冻结或解冻关联账户。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (event == null) { %>
        <section class="quick-actions">
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/risk/events">返回风控事件</a>
        </section>
    <% } else { %>
        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>事件信息</h2>
                    <p class="section-note">交易号 <%= HtmlUtil.escape(event.getTransactionNo()) %></p>
                </div>
                <div class="section-actions">
                    <span class="direction <%= "BLOCK".equals(event.getDecision()) ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(event.getDecision()) %></span>
                    <span class="direction <%= "PENDING".equals(event.getHandleStatus()) ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(handleStatusName(event.getHandleStatus())) %></span>
                </div>
            </div>
            <dl class="detail-list">
                <div><dt>交易类型</dt><dd><%= HtmlUtil.escape(txnTypeName(event.getTxnType())) %></dd></div>
                <div><dt>交易金额</dt><dd>¥ <%= moneyText(event.getAmount()) %></dd></div>
                <div><dt>风险等级</dt><dd><%= HtmlUtil.escape(event.getRiskLevel()) %>，评分 <%= event.getRiskScore() %></dd></div>
                <div><dt>命中规则</dt><dd><%= HtmlUtil.escape(event.getHitRules()) %></dd></div>
                <div><dt>触发原因</dt><dd><%= HtmlUtil.escape(event.getReason()) %></dd></div>
                <div><dt>触发 IP</dt><dd><%= HtmlUtil.escape(event.getIpAddress()) %></dd></div>
                <div><dt>触发时间</dt><dd><%= event.getCreatedAt() == null ? "" : event.getCreatedAt().toString().substring(0, 19) %></dd></div>
                <div><dt>最近结论</dt><dd><%= HtmlUtil.escape(handleResultName(event.getHandleResult())) %></dd></div>
                <div><dt>最近处理</dt><dd><%= event.getHandledAt() == null ? "未处理" : event.getHandledAt().toString().substring(0, 19) %> <%= HtmlUtil.escape(event.getHandlerUsername()) %></dd></div>
                <div><dt>处理备注</dt><dd><%= HtmlUtil.escape(event.getHandleNote()) %></dd></div>
            </dl>
        </section>

        <section class="metric-grid risk-detail-grid">
            <article class="metric-card">
                <span>客户姓名</span>
                <strong><%= customer == null ? HtmlUtil.escape(event.getFullName()) : HtmlUtil.escape(customer.getFullName()) %></strong>
                <p class="section-note"><%= customer == null ? HtmlUtil.escape(event.getPhone()) : HtmlUtil.escape(customer.getPhone()) %></p>
            </article>
            <article class="metric-card">
                <span>客户风险等级</span>
                <strong><%= customer == null ? "" : HtmlUtil.escape(customer.getRiskLevel()) %></strong>
                <p class="section-note">
                    <a href="<%= request.getContextPath() %>/admin/customer/detail?customerId=<%= event.getCustomerId() %>">查看客户详情</a>
                </p>
            </article>
            <article class="metric-card">
                <span>关联账户</span>
                <strong><%= account == null ? HtmlUtil.escape(event.getAccountNo()) : HtmlUtil.escape(account.getAccountNo()) %></strong>
                <p class="section-note"><%= account == null ? "" : HtmlUtil.escape(account.getAccountType()) %></p>
            </article>
            <article class="metric-card">
                <span>账户状态</span>
                <strong><%= account == null ? HtmlUtil.escape(event.getAccountStatus()) : HtmlUtil.escape(account.getStatus()) %></strong>
                <p class="section-note">余额 ¥ <%= account == null ? "0.00" : moneyText(account.getAvailableBalance()) %></p>
            </article>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>处置操作</h2>
                    <p class="section-note">BLOCK 交易已经失败；如判定误报，关闭事件后由用户重新发起交易。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/admin/risk/event/detail" class="form grid-form">
                <input type="hidden" name="eventId" value="<%= event.getEventId() %>">
                <label>
                    <span>处置结论</span>
                    <select name="handleResult">
                        <option value="FALSE_POSITIVE">误报</option>
                        <option value="CONFIRMED_RISK">确认风险</option>
                        <option value="CUSTOMER_VERIFIED">客户已核实</option>
                        <option value="FOLLOW_UP">继续跟进</option>
                    </select>
                </label>
                <label>
                    <span>账户动作</span>
                    <select name="accountAction">
                        <option value="NONE">不变更账户</option>
                        <option value="FREEZE">冻结关联账户</option>
                        <option value="UNFREEZE">解冻关联账户</option>
                    </select>
                </label>
                <label class="full-row">
                    <span>处置备注</span>
                    <textarea name="note" rows="4" maxlength="500" placeholder="记录核实依据、客户沟通情况或后续跟进事项"></textarea>
                </label>
                <div class="full-row action-row">
                    <a class="button secondary" href="<%= request.getContextPath() %>/admin/risk/events">返回列表</a>
                    <% if (adminUser.hasPermission("RISK_EVENT_HANDLE")) { %>
                    <button class="button primary" type="submit">提交处置</button>
                    <% } %>
                </div>
            </form>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>处置记录</h2></div>
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
                        <tr><td colspan="6" class="empty">暂无处置记录</td></tr>
                    <% } else {
                        for (RiskActionLog log : actionLogs) {
                    %>
                        <tr>
                            <td><%= log.getCreatedAt() == null ? "" : log.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= HtmlUtil.escape(log.getAdminUsername()) %></td>
                            <td><%= HtmlUtil.escape(actionTypeName(log.getActionType())) %></td>
                            <td><%= HtmlUtil.escape(handleStatusName(log.getBeforeStatus())) %></td>
                            <td><%= HtmlUtil.escape(handleStatusName(log.getAfterStatus())) %></td>
                            <td><%= HtmlUtil.escape(log.getNote()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>近期流水</h2></div>
            <div class="table-wrap">
                <table class="wide-table">
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>交易号</th>
                        <th>类型</th>
                        <th>方向</th>
                        <th>金额</th>
                        <th>余额</th>
                        <th>摘要</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (ledgers == null || ledgers.isEmpty()) { %>
                        <tr><td colspan="7" class="empty">暂无近期流水</td></tr>
                    <% } else {
                        for (LedgerEntryView ledger : ledgers) {
                    %>
                        <tr>
                            <td><%= ledger.getCreatedAt() == null ? "" : ledger.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= HtmlUtil.escape(ledger.getTransactionNo()) %></td>
                            <td><%= HtmlUtil.escape(txnTypeName(ledger.getTxnType())) %></td>
                            <td><span class="direction <%= "OUT".equals(ledger.getDirection()) ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(ledger.getDirection()) %></span></td>
                            <td>¥ <%= moneyText(ledger.getAmount()) %></td>
                            <td>¥ <%= moneyText(ledger.getBalanceAfter()) %></td>
                            <td><%= HtmlUtil.escape(ledger.getSummary()) %></td>
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

