<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.RiskLimitRule,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
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
        return txnType == null ? "" : txnType;
    }

    private String ruleDisplayName(RiskLimitRule rule) {
        if (rule == null) {
            return "";
        }
        return txnTypeName(rule.getTxnType()) + " · " + rule.getCustomerRiskLevel() + " 客户";
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<RiskLimitRule> rules = (List<RiskLimitRule>) request.getAttribute("rules");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    String selectedTxnType = (String) request.getAttribute("selectedTxnType");
    String selectedRiskLevel = (String) request.getAttribute("selectedRiskLevel");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>风控规则管理 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">限额规则</p>
        <h1>风控规则管理</h1>
        <p class="muted">管理资金流出类交易的单笔限额、日累计限额、日笔数上限和启用状态。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/risk/rules" class="filter-form">
            <label>
                <span>交易类型</span>
                <select name="txnType">
                    <option value="">全部</option>
                    <option value="WITHDRAW" <%= "WITHDRAW".equals(selectedTxnType) ? "selected" : "" %>>取款</option>
                    <option value="TRANSFER_INNER" <%= "TRANSFER_INNER".equals(selectedTxnType) ? "selected" : "" %>>本行转账</option>
                    <option value="PAYMENT" <%= "PAYMENT".equals(selectedTxnType) ? "selected" : "" %>>生活缴费</option>
                    <option value="BUY_WEALTH" <%= "BUY_WEALTH".equals(selectedTxnType) ? "selected" : "" %>>理财申购</option>
                </select>
            </label>
            <label>
                <span>客户风险等级</span>
                <select name="riskLevel">
                    <option value="">全部</option>
                    <option value="C1" <%= "C1".equals(selectedRiskLevel) ? "selected" : "" %>>C1</option>
                    <option value="C2" <%= "C2".equals(selectedRiskLevel) ? "selected" : "" %>>C2</option>
                    <option value="C3" <%= "C3".equals(selectedRiskLevel) ? "selected" : "" %>>C3</option>
                    <option value="C4" <%= "C4".equals(selectedRiskLevel) ? "selected" : "" %>>C4</option>
                    <option value="C5" <%= "C5".equals(selectedRiskLevel) ? "selected" : "" %>>C5</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>限额规则</h2>
                <p class="section-note">启用规则会参与交易风控校验；停用规则会让该等级回退到 C3 规则，C3 也关闭时交易将无法通过限额校验。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table admin-edit-table">
                <thead>
                <tr>
                    <th>规则名称</th>
                    <th>交易类型</th>
                    <th>客户等级</th>
                    <th>单笔限额</th>
                    <th>日累计限额</th>
                    <th>日笔数</th>
                    <th>状态</th>
                    <th>更新时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (rules == null || rules.isEmpty()) { %>
                    <tr><td colspan="9" class="empty">暂无风控规则</td></tr>
                <% } else {
                    for (RiskLimitRule rule : rules) {
                        String formId = "riskRuleForm" + rule.getRuleId();
                        boolean active = "ACTIVE".equals(rule.getStatus());
                %>
                    <tr>
                        <td>
                            <form id="<%= formId %>" method="post" action="<%= request.getContextPath() %>/admin/risk/rules">
                                <input type="hidden" name="ruleId" value="<%= rule.getRuleId() %>">
                                <input type="hidden" name="filterTxnType" value="<%= HtmlUtil.escape(selectedTxnType) %>">
                                <input type="hidden" name="filterRiskLevel" value="<%= HtmlUtil.escape(selectedRiskLevel) %>">
                            </form>
                            <strong><%= HtmlUtil.escape(ruleDisplayName(rule)) %></strong>
                            <p class="cell-note">规则编号 #<%= rule.getRuleId() %></p>
                        </td>
                        <td><%= HtmlUtil.escape(txnTypeName(rule.getTxnType())) %></td>
                        <td><span class="tag"><%= HtmlUtil.escape(rule.getCustomerRiskLevel()) %></span></td>
                        <td><input form="<%= formId %>" class="admin-money-input" name="singleLimit" value="<%= moneyText(rule.getSingleLimit()) %>"></td>
                        <td><input form="<%= formId %>" class="admin-money-input" name="dailyAmountLimit" value="<%= moneyText(rule.getDailyAmountLimit()) %>"></td>
                        <td><input form="<%= formId %>" class="admin-count-input" name="dailyCountLimit" value="<%= rule.getDailyCountLimit() %>"></td>
                        <td>
                            <select form="<%= formId %>" class="admin-status-select" name="status">
                                <option value="ACTIVE" <%= active ? "selected" : "" %>>启用</option>
                                <option value="DISABLED" <%= active ? "" : "selected" %>>停用</option>
                            </select>
                        </td>
                        <td><%= rule.getUpdatedAt() == null ? "" : rule.getUpdatedAt().toString().substring(0, 19) %></td>
                        <td>
                            <% if (adminUser.hasPermission("RISK_RULE_UPDATE")) { %>
                            <button form="<%= formId %>" class="button primary compact" type="submit">保存</button>
                            <% } %>
                            <span class="direction <%= active ? "direction-in" : "direction-out" %>"><%= active ? "启用" : "停用" %></span>
                        </td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>
</main>
</body>
</html>

