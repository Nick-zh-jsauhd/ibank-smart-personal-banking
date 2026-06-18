<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<Account> accounts = (List<Account>) request.getAttribute("accounts");
    String error = (String) request.getAttribute("error");
    Long selectedAccountId = (Long) request.getAttribute("selectedAccountId");
    String amount = (String) request.getAttribute("amount");
    String remark = (String) request.getAttribute("remark");
    request.setAttribute("activeNav", "transfer");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>模拟存款 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">存款入账</p>
            <h1>模拟存款</h1>
            <p class="muted">真实更新账户余额，并同步生成交易主记录和收入流水。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/transactions?txnType=DEPOSIT">查看存款流水</a>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>模拟存款用于演示入账后的资金变化。</strong>
            <p>提交成功后，工作台余额、近期流水和月账单收入会同步更新。</p>
        </div>
    </div>

    <section class="task-layout">
        <article class="task-panel with-warm-accent">
            <form method="post" action="<%= request.getContextPath() %>/deposit" class="form">
                <label>
                    <span>入账账户</span>
                    <select name="accountId" required>
                        <option value="">请选择账户</option>
                        <% if (accounts != null) {
                            for (Account account : accounts) {
                                boolean selected = selectedAccountId != null && selectedAccountId.equals(account.getAccountId());
                        %>
                            <option value="<%= account.getAccountId() %>" <%= selected ? "selected" : "" %>>
                                <%= HtmlUtil.escape(account.getAccountNo()) %>｜可用余额 ¥ <%= moneyText(account.getAvailableBalance()) %>
                            </option>
                        <%  }
                        } %>
                    </select>
                </label>
                <label>
                    <span>存款金额</span>
                    <input name="amount" type="number" min="0.01" max="1000000" step="0.01" value="<%= HtmlUtil.escape(amount) %>" required>
                </label>
                <label>
                    <span>备注</span>
                    <input name="remark" type="text" maxlength="255" value="<%= HtmlUtil.escape(remark) %>">
                </label>
                <button class="button primary full" type="submit">提交存款</button>
            </form>
        </article>

        <aside class="task-side">
            <section class="content-section">
                <h2>业务说明</h2>
                <ul class="task-checklist">
                    <li>模拟存款用于演示入账链路，不需要支付密码。</li>
                    <li>成功后账户可用余额增加，并产生 DEPOSIT 交易。</li>
                    <li>本月收入和工作台现金流会同步更新。</li>
                </ul>
            </section>
            <section class="content-section">
                <h2>可选账户</h2>
                <div class="movement-list">
                    <% if (accounts == null || accounts.isEmpty()) { %>
                        <div class="human-empty">
                            <strong>暂时没有可入账账户。</strong>
                            <p>开户完成后，可选择账户进行模拟存款并观察现金流变化。</p>
                        </div>
                    <% } else {
                        for (Account account : accounts) {
                    %>
                        <div class="movement-item">
                            <div class="movement-item-main">
                                <strong><%= HtmlUtil.escape(account.getAccountNo()) %></strong>
                                <p><%= HtmlUtil.escape(account.getBranchName()) %></p>
                            </div>
                            <strong>¥ <%= moneyText(account.getAvailableBalance()) %></strong>
                        </div>
                    <%  }
                    } %>
                </div>
            </section>
        </aside>
    </section>
</main>
</body>
</html>
