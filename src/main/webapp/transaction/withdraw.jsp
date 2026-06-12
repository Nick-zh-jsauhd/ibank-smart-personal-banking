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
    <title>模拟取款 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Withdraw</p>
            <h1>模拟取款</h1>
            <p class="muted">校验支付密码、账户余额和风控限额后生成出账流水。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/transactions?txnType=WITHDRAW">查看取款流水</a>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>取款会经过支付密码、余额和风控限额校验。</strong>
            <p>如果金额、频次或账户状态异常，系统会先拦截或预警，并说明触发原因。</p>
        </div>
    </div>

    <section class="task-layout">
        <article class="task-panel with-warm-accent">
            <form method="post" action="<%= request.getContextPath() %>/withdraw" class="form">
                <label>
                    <span>取款账户</span>
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
                    <span>取款金额</span>
                    <input name="amount" type="number" min="0.01" max="50000" step="0.01" value="<%= HtmlUtil.escape(amount) %>" required>
                </label>
                <label>
                    <span>支付密码</span>
                    <input name="payPassword" type="password" required maxlength="6" pattern="\d{6}" inputmode="numeric">
                </label>
                <label>
                    <span>备注</span>
                    <input name="remark" type="text" maxlength="255" value="<%= HtmlUtil.escape(remark) %>">
                </label>
                <button class="button primary full" type="submit">提交取款</button>
            </form>
        </article>

        <aside class="task-side">
            <section class="content-section">
                <h2>风控与安全</h2>
                <ul class="task-checklist">
                    <li>单笔最高 50000 元，实际仍受客户风险等级和规则限制。</li>
                    <li>必须输入 6 位支付密码。</li>
                    <li>异常取款会进入风险事件，并同步通知客户。</li>
                    <li><a href="<%= request.getContextPath() %>/security/pay-password">设置或修改支付密码</a></li>
                </ul>
            </section>
            <section class="content-section">
                <h2>账户余额</h2>
                <div class="movement-list">
                    <% if (accounts == null || accounts.isEmpty()) { %>
                        <div class="human-empty">
                            <strong>暂时没有可取款账户。</strong>
                            <p>账户开通并有可用余额后，再选择账户提交取款。</p>
                        </div>
                    <% } else {
                        for (Account account : accounts) {
                    %>
                        <div class="movement-item">
                            <div class="movement-item-main">
                                <strong><%= HtmlUtil.escape(account.getAccountNo()) %></strong>
                                <p><%= HtmlUtil.escape(account.getAccountType()) %></p>
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
