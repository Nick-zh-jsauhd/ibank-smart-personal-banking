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
    BigDecimal totalAvailable = BigDecimal.ZERO.setScale(2);
    BigDecimal totalFrozen = BigDecimal.ZERO.setScale(2);
    if (accounts != null) {
        for (Account account : accounts) {
            if (account.getAvailableBalance() != null) {
                totalAvailable = totalAvailable.add(account.getAvailableBalance());
            }
            if (account.getFrozenBalance() != null) {
                totalFrozen = totalFrozen.add(account.getFrozenBalance());
            }
        }
    }
    request.setAttribute("activeNav", "accounts");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>账户列表 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Accounts</p>
            <h1>账户资产</h1>
            <p class="muted">按账户查看可用余额、冻结余额、开户行和状态。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/transfer">转账</a>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/withdraw">取款</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/deposit">存款</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>先看可用余额，再决定下一步资金安排。</strong>
            <p>冻结余额代表暂时不可用资金；如果对账户状态或余额有疑问，可以从服务中心发起账户服务工单。</p>
        </div>
    </div>

    <section class="metric-grid">
        <article class="metric-card">
            <span>可用总额</span>
            <strong>¥ <%= moneyText(totalAvailable) %></strong>
        </article>
        <article class="metric-card">
            <span>冻结总额</span>
            <strong>¥ <%= moneyText(totalFrozen) %></strong>
        </article>
        <article class="metric-card">
            <span>账户数量</span>
            <strong><%= accounts == null ? 0 : accounts.size() %></strong>
        </article>
        <article class="metric-card account-default-metric">
            <span>默认账户</span>
            <%
                String defaultNo = "未设置";
                String defaultDisplay = defaultNo;
                   if (accounts != null) {
                       for (Account account : accounts) {
                           if (account.isDefaultFlag()) {
                               defaultNo = account.getAccountNo();
                               if (defaultNo != null && defaultNo.length() > 4) {
                                   defaultDisplay = "尾号 " + defaultNo.substring(defaultNo.length() - 4);
                               } else {
                                   defaultDisplay = defaultNo;
                               }
                               break;
                           }
                       }
                   }
            %>
            <strong class="account-number-value" title="<%= HtmlUtil.escape(defaultNo) %>"><%= HtmlUtil.escape(defaultDisplay) %></strong>
            <% if (!"未设置".equals(defaultNo)) { %>
                <small class="account-number-full"><%= HtmlUtil.escape(defaultNo) %></small>
            <% } %>
        </article>
    </section>

    <section class="section-block">
        <div class="section-title">
            <div>
                <h2>账户卡片</h2>
                <p class="section-note">先看关键资金状态，再进入下方完整明细。</p>
            </div>
        </div>
        <% if (accounts == null || accounts.isEmpty()) { %>
            <div class="content-section">
                <div class="human-empty">
                    <strong>暂时还没有账户数据。</strong>
                    <p>开户成功后，默认账户、可用余额和交易入口会显示在这里。</p>
                </div>
            </div>
        <% } else { %>
            <div class="account-card-grid">
                <% for (Account account : accounts) { %>
                    <article class="account-card">
                        <div class="account-card-head">
                            <div>
                                <h3><%= HtmlUtil.escape(account.getAccountType()) %></h3>
                                <span class="account-number"><%= HtmlUtil.escape(account.getAccountNo()) %></span>
                            </div>
                            <span class="<%= account.isDefaultFlag() ? "tag" : "status" %>"><%= account.isDefaultFlag() ? "默认" : HtmlUtil.escape(account.getStatus()) %></span>
                        </div>
                        <strong>¥ <%= moneyText(account.getAvailableBalance()) %></strong>
                        <p><%= HtmlUtil.escape(account.getBranchName()) %> · 冻结 ¥ <%= moneyText(account.getFrozenBalance()) %></p>
                    </article>
                <% } %>
            </div>
        <% } %>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>完整账户明细</h2>
                <p class="section-note">保留表格视图，方便核对开户时间、状态和余额字段。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>账号</th>
                    <th>账户类型</th>
                    <th>开户行</th>
                    <th>可用余额</th>
                    <th>冻结余额</th>
                    <th>状态</th>
                    <th>开户时间</th>
                </tr>
                </thead>
                <tbody>
                <% if (accounts == null || accounts.isEmpty()) { %>
                    <tr><td colspan="7" class="empty">暂时还没有账户数据，开户后会在这里显示完整明细。</td></tr>
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
                        <td><%= account.getOpenedAt() == null ? "" : account.getOpenedAt().toString().substring(0, 19) %></td>
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
