<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.bean.Customer,com.bank.bean.Notification,com.bank.bean.RiskAssessment,com.bank.bean.User,com.bank.dto.AdminCustomerDetail,com.bank.dto.AdminSessionUser,com.bank.dto.LedgerEntryView,com.bank.dto.RiskEventView,com.bank.dto.WealthOrderConfirmView,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String txnTypeName(String txnType) {
        if ("DEPOSIT".equals(txnType)) return "存款";
        if ("WITHDRAW".equals(txnType)) return "取款";
        if ("TRANSFER_INNER".equals(txnType)) return "本行转账";
        if ("PAYMENT".equals(txnType)) return "生活缴费";
        if ("BUY_WEALTH".equals(txnType)) return "理财申购";
        if ("REDEEM_WEALTH".equals(txnType)) return "理财赎回";
        return txnType == null ? "" : txnType;
    }

    private String sourceName(String source) {
        if ("ASSESSMENT".equals(source)) return "风险测评";
        if ("ADMIN".equals(source)) return "管理员调整";
        if ("SYSTEM".equals(source)) return "系统默认";
        return source == null ? "" : source;
    }

    private String matchName(String matchResult) {
        if ("MATCH".equals(matchResult)) return "匹配";
        if ("EDGE_MATCH".equals(matchResult)) return "临界匹配";
        if ("MISMATCH".equals(matchResult)) return "不匹配";
        return matchResult == null ? "" : matchResult;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminCustomerDetail detail = (AdminCustomerDetail) request.getAttribute("detail");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    Customer customer = detail == null ? null : detail.getCustomer();
    User user = detail == null ? null : detail.getUser();
    List<Account> accounts = detail == null ? null : detail.getAccounts();
    List<LedgerEntryView> ledgers = detail == null ? null : detail.getRecentLedgers();
    List<RiskEventView> riskEvents = detail == null ? null : detail.getRiskEvents();
    List<Notification> notifications = detail == null ? null : detail.getNotifications();
    List<RiskAssessment> assessments = detail == null ? null : detail.getRiskAssessments();
    List<WealthOrderConfirmView> confirms = detail == null ? null : detail.getWealthOrderConfirms();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>客户详情 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Customer Detail</p>
        <h1>客户详情</h1>
        <p class="muted">客户档案、账户、近期流水、风控事件和站内通知的聚合视图。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (customer != null) { %>
        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2><%= HtmlUtil.escape(customer.getFullName()) %></h2>
                    <p class="section-note">客户编号：<%= customer.getCustomerId() %>，用户名：<%= user == null ? "" : HtmlUtil.escape(user.getUsername()) %></p>
                </div>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/customers">返回客户列表</a>
            </div>
            <dl class="detail-list">
                <div><dt>手机号</dt><dd><%= HtmlUtil.escape(customer.getPhone()) %></dd></div>
                <div><dt>邮箱</dt><dd><%= HtmlUtil.escape(customer.getEmail()) %></dd></div>
                <div><dt>地址</dt><dd><%= HtmlUtil.escape(customer.getAddress()) %></dd></div>
                <div><dt>风险等级</dt><dd><span class="tag"><%= HtmlUtil.escape(customer.getRiskLevel()) %></span></dd></div>
                <div><dt>等级来源</dt><dd><%= HtmlUtil.escape(sourceName(customer.getRiskLevelSource())) %></dd></div>
                <div><dt>等级更新时间</dt><dd><%= customer.getRiskLevelUpdatedAt() == null ? "未更新" : customer.getRiskLevelUpdatedAt().toString().substring(0, 19) %></dd></div>
                <div><dt>账号状态</dt><dd><span class="status"><%= user == null ? "" : HtmlUtil.escape(user.getStatus()) %></span></dd></div>
                <div><dt>创建时间</dt><dd><%= customer.getCreatedAt() == null ? "" : customer.getCreatedAt().toString().substring(0, 19) %></dd></div>
            </dl>
        </section>

        <section class="content-section">
            <div class="section-title">
                <div>
                    <h2>风险等级管理</h2>
                    <p class="section-note">人工调整会覆盖客户当前风险等级，并写入管理员审计日志。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/admin/customer/risk-level" class="form grid-form">
                <input type="hidden" name="customerId" value="<%= customer.getCustomerId() %>">
                <label>
                    <span>新风险等级</span>
                    <select name="riskLevel">
                        <option value="C1" <%= "C1".equals(customer.getRiskLevel()) ? "selected" : "" %>>C1 保守型</option>
                        <option value="C2" <%= "C2".equals(customer.getRiskLevel()) ? "selected" : "" %>>C2 稳健型</option>
                        <option value="C3" <%= "C3".equals(customer.getRiskLevel()) ? "selected" : "" %>>C3 平衡型</option>
                        <option value="C4" <%= "C4".equals(customer.getRiskLevel()) ? "selected" : "" %>>C4 成长型</option>
                        <option value="C5" <%= "C5".equals(customer.getRiskLevel()) ? "selected" : "" %>>C5 进取型</option>
                    </select>
                </label>
                <label>
                    <span>调整原因</span>
                    <input name="reason" maxlength="500" placeholder="例如：客户线下补充材料，经复核调整">
                </label>
                <div class="full-row action-row">
                    <% if (adminUser.hasPermission("CUSTOMER_RISK_ADJUST")) { %>
                    <button class="button primary" type="submit">保存调整</button>
                    <% } %>
                </div>
            </form>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>风险测评记录</h2></div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>提交时间</th>
                        <th>得分</th>
                        <th>等级</th>
                        <th>状态</th>
                        <th>有效期</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (assessments == null || assessments.isEmpty()) { %>
                        <tr><td colspan="5" class="empty">暂无风险测评记录</td></tr>
                    <% } else {
                        for (RiskAssessment assessment : assessments) {
                    %>
                        <tr>
                            <td><%= assessment.getCreatedAt() == null ? "" : assessment.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= assessment.getTotalScore() %></td>
                            <td><span class="tag"><%= HtmlUtil.escape(assessment.getRiskLevel()) %></span></td>
                            <td><span class="status"><%= HtmlUtil.escape(assessment.getStatus()) %></span></td>
                            <td><%= assessment.getEffectiveUntil() == null ? "" : assessment.getEffectiveUntil().toString().substring(0, 10) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>理财申购确认记录</h2></div>
            <div class="table-wrap">
                <table class="wide-table">
                    <thead>
                    <tr>
                        <th>确认时间</th>
                        <th>交易号</th>
                        <th>产品</th>
                        <th>账户</th>
                        <th>金额</th>
                        <th>客户/产品风险</th>
                        <th>匹配结果</th>
                        <th>揭示版本</th>
                        <th>IP</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (confirms == null || confirms.isEmpty()) { %>
                        <tr><td colspan="9" class="empty">暂无理财申购确认记录</td></tr>
                    <% } else {
                        for (WealthOrderConfirmView confirm : confirms) {
                    %>
                        <tr>
                            <td><%= confirm.getCreatedAt() == null ? "" : confirm.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= HtmlUtil.escape(confirm.getTransactionNo()) %></td>
                            <td>
                                <strong><%= HtmlUtil.escape(confirm.getProductName()) %></strong>
                                <p class="cell-note"><%= HtmlUtil.escape(confirm.getProductCode()) %></p>
                            </td>
                            <td><%= HtmlUtil.escape(confirm.getAccountNo()) %></td>
                            <td>¥ <%= moneyText(confirm.getAmount()) %></td>
                            <td><%= HtmlUtil.escape(confirm.getCustomerRiskLevel()) %> / <%= HtmlUtil.escape(confirm.getProductRiskLevel()) %></td>
                            <td><span class="tag"><%= HtmlUtil.escape(matchName(confirm.getMatchResult())) %></span></td>
                            <td><%= HtmlUtil.escape(confirm.getDisclosureVersion()) %></td>
                            <td><%= HtmlUtil.escape(confirm.getIpAddress()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>账户</h2></div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>账号</th>
                        <th>类型</th>
                        <th>开户行</th>
                        <th>可用余额</th>
                        <th>冻结余额</th>
                        <th>状态</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (accounts == null || accounts.isEmpty()) { %>
                        <tr><td colspan="6" class="empty">暂无账户</td></tr>
                    <% } else {
                        for (Account account : accounts) {
                    %>
                        <tr>
                            <td><%= HtmlUtil.escape(account.getAccountNo()) %><%= account.isDefaultFlag() ? " <span class=\"tag\">默认</span>" : "" %></td>
                            <td><%= HtmlUtil.escape(account.getAccountType()) %></td>
                            <td><%= HtmlUtil.escape(account.getBranchName()) %></td>
                            <td>¥ <%= moneyText(account.getAvailableBalance()) %></td>
                            <td>¥ <%= moneyText(account.getFrozenBalance()) %></td>
                            <td><span class="status"><%= HtmlUtil.escape(account.getStatus()) %></span></td>
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
                <table>
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>交易编号</th>
                        <th>账户</th>
                        <th>类型</th>
                        <th>方向</th>
                        <th>金额</th>
                        <th>余额</th>
                        <th>摘要</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (ledgers == null || ledgers.isEmpty()) { %>
                        <tr><td colspan="8" class="empty">暂无流水</td></tr>
                    <% } else {
                        for (LedgerEntryView ledger : ledgers) {
                            boolean income = "IN".equals(ledger.getDirection());
                    %>
                        <tr>
                            <td><%= ledger.getCreatedAt() == null ? "" : ledger.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= HtmlUtil.escape(ledger.getTransactionNo()) %></td>
                            <td><%= HtmlUtil.escape(ledger.getAccountNo()) %></td>
                            <td><%= HtmlUtil.escape(txnTypeName(ledger.getTxnType())) %></td>
                            <td><span class="direction <%= income ? "direction-in" : "direction-out" %>"><%= income ? "收入" : "支出" %></span></td>
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

        <section class="content-section">
            <div class="section-title"><h2>风险事件</h2></div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>类型</th>
                        <th>金额</th>
                        <th>决策</th>
                        <th>评分</th>
                        <th>原因</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (riskEvents == null || riskEvents.isEmpty()) { %>
                        <tr><td colspan="6" class="empty">暂无风险事件</td></tr>
                    <% } else {
                        for (RiskEventView event : riskEvents) {
                            boolean blocked = "BLOCK".equals(event.getDecision());
                    %>
                        <tr>
                            <td><%= event.getCreatedAt() == null ? "" : event.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= HtmlUtil.escape(txnTypeName(event.getTxnType())) %></td>
                            <td>¥ <%= moneyText(event.getAmount()) %></td>
                            <td><span class="direction <%= blocked ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(event.getDecision()) %></span></td>
                            <td><%= event.getRiskScore() %></td>
                            <td><%= HtmlUtil.escape(event.getReason()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="content-section">
            <div class="section-title"><h2>近期通知</h2></div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>类型</th>
                        <th>标题</th>
                        <th>状态</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (notifications == null || notifications.isEmpty()) { %>
                        <tr><td colspan="4" class="empty">暂无通知</td></tr>
                    <% } else {
                        int shown = 0;
                        for (Notification notification : notifications) {
                            if (shown++ >= 10) {
                                break;
                            }
                    %>
                        <tr>
                            <td><%= notification.getCreatedAt() == null ? "" : notification.getCreatedAt().toString().substring(0, 19) %></td>
                            <td><%= HtmlUtil.escape(notification.getNotificationType()) %></td>
                            <td><%= HtmlUtil.escape(notification.getTitle()) %></td>
                            <td><span class="<%= notification.isReadFlag() ? "status" : "direction direction-out" %>"><%= notification.isReadFlag() ? "已读" : "未读" %></span></td>
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

