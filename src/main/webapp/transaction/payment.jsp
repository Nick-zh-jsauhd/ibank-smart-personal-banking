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
    String selectedPaymentType = (String) request.getAttribute("selectedPaymentType");
    String payerNo = (String) request.getAttribute("payerNo");
    String billingMonth = (String) request.getAttribute("billingMonth");
    String amount = (String) request.getAttribute("amount");
    String remark = (String) request.getAttribute("remark");
    request.setAttribute("activeNav", "transfer");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>生活缴费 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Payment</p>
            <h1>生活缴费</h1>
            <p class="muted">扣减账户余额，并生成 PAYMENT 交易、出账流水和缴费业务记录。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/payment-records">缴费记录</a>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/transactions?txnType=PAYMENT">缴费流水</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="photo-band payment">
        <div>
            <p class="eyebrow">Life Payment</p>
            <h2>把生活账单放进月度现金流里一起看。</h2>
            <p class="section-note">缴费成功后会同步扣减余额、生成缴费记录，并在月账单中归入生活支出。</p>
        </div>
    </section>

    <section class="task-layout">
        <article class="task-panel with-warm-accent">
            <form method="post" action="<%= request.getContextPath() %>/payment" class="form">
                <label>
                    <span>付款账户</span>
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
                    <span>缴费类型</span>
                    <select name="paymentType" required>
                        <option value="">请选择类型</option>
                        <option value="WATER" <%= "WATER".equals(selectedPaymentType) ? "selected" : "" %>>水费</option>
                        <option value="ELECTRICITY" <%= "ELECTRICITY".equals(selectedPaymentType) ? "selected" : "" %>>电费</option>
                        <option value="GAS" <%= "GAS".equals(selectedPaymentType) ? "selected" : "" %>>燃气费</option>
                        <option value="MOBILE" <%= "MOBILE".equals(selectedPaymentType) ? "selected" : "" %>>话费</option>
                    </select>
                </label>
                <label>
                    <span>户号 / 手机号</span>
                    <input name="payerNo" type="text" maxlength="40" value="<%= HtmlUtil.escape(payerNo) %>" required>
                </label>
                <label>
                    <span>账期</span>
                    <input name="billingMonth" type="month" value="<%= HtmlUtil.escape(billingMonth) %>" required>
                </label>
                <label>
                    <span>缴费金额</span>
                    <input name="amount" type="number" min="0.01" max="20000" step="0.01" value="<%= HtmlUtil.escape(amount) %>" required>
                </label>
                <label>
                    <span>支付密码</span>
                    <input name="payPassword" type="password" required maxlength="6" pattern="\d{6}" inputmode="numeric">
                </label>
                <label>
                    <span>备注</span>
                    <input name="remark" type="text" maxlength="255" value="<%= HtmlUtil.escape(remark) %>">
                </label>
                <button class="button primary full" type="submit">提交缴费</button>
            </form>
        </article>

        <aside class="task-side">
            <section class="content-section">
                <h2>缴费检查</h2>
                <ul class="task-checklist">
                    <li>支持水费、电费、燃气费和话费四类模拟缴费。</li>
                    <li>缴费金额最高 20000 元，资金流出会触发风控评估。</li>
                    <li>成功后月账单分类会出现生活缴费支出。</li>
                    <li>缴费完成后可在缴费记录和交易流水中核对。</li>
                </ul>
            </section>
            <section class="content-section">
                <h2>付款账户</h2>
                <div class="movement-list">
                    <% if (accounts == null || accounts.isEmpty()) { %>
                        <div class="human-empty">
                            <strong>暂时没有可用于缴费的账户。</strong>
                            <p>账户开通或入账后，可用余额会在这里展示，再选择水电燃气或话费账单完成支付。</p>
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
