<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.dto.LedgerEntryView,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.sql.Timestamp,java.util.List" %>
<%!
    private String txnTypeName(String txnType) {
        if ("DEPOSIT".equals(txnType)) return "存款";
        if ("WITHDRAW".equals(txnType)) return "取款";
        if ("TRANSFER_INNER".equals(txnType)) return "本行转账";
        if ("PAYMENT".equals(txnType)) return "生活缴费";
        if ("BUY_WEALTH".equals(txnType)) return "理财申购";
        if ("REDEEM_WEALTH".equals(txnType)) return "理财赎回";
        return txnType == null ? "其他交易" : txnType;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private int percent(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int value = amount.multiply(new BigDecimal("100")).divide(total, 0, RoundingMode.HALF_UP).intValue();
        if (value > 100) return 100;
        return value < 4 ? 4 : value;
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<Account> accounts = (List<Account>) request.getAttribute("accounts");
    List<LedgerEntryView> entries = (List<LedgerEntryView>) request.getAttribute("entries");
    String error = (String) request.getAttribute("error");
    Long selectedAccountId = (Long) request.getAttribute("selectedAccountId");
    String selectedDirection = (String) request.getAttribute("selectedDirection");
    String selectedTxnType = (String) request.getAttribute("selectedTxnType");
    BigDecimal totalIncome = BigDecimal.ZERO.setScale(2);
    BigDecimal totalExpense = BigDecimal.ZERO.setScale(2);
    int incomeCount = 0;
    int expenseCount = 0;
    LedgerEntryView largestMovement = null;
    if (entries != null) {
        for (LedgerEntryView entry : entries) {
            if ("IN".equals(entry.getDirection())) {
                totalIncome = totalIncome.add(entry.getAmount());
                incomeCount++;
            } else if ("OUT".equals(entry.getDirection())) {
                totalExpense = totalExpense.add(entry.getAmount());
                expenseCount++;
            }
            if (largestMovement == null || entry.getAmount().compareTo(largestMovement.getAmount()) > 0) {
                largestMovement = entry;
            }
        }
    }
    BigDecimal maxFlow = totalIncome.max(totalExpense);
    request.setAttribute("activeNav", "transactions");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>交易流水 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css?v=transaction-layout-20260615a">
</head>
<body class="customer-page transaction-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">交易流水</p>
            <h1>交易流水</h1>
            <p class="muted">按账户、方向和业务类型查看最近 100 条资金动向。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/bill/report">收支报表</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/transfer">发起转账</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note transaction-care-note">
        <div>
            <strong>流水用于核对每一笔资金变化。</strong>
            <p>如果某笔交易看起来不符合预期，可以复制交易编号，从服务中心发起交易争议工单。</p>
        </div>
    </div>

    <section class="transaction-workbench">
        <article class="cashflow-card transaction-cashflow-card">
            <div class="section-title">
                <div>
                    <h2>筛选结果现金流</h2>
                    <p class="section-note">当前列表范围内收入 <%= incomeCount %> 笔，支出 <%= expenseCount %> 笔。</p>
                </div>
            </div>
            <div class="cashflow-summary">
                <div><span class="small-label">收入</span><strong class="money-in">¥ <%= moneyText(totalIncome) %></strong></div>
                <div><span class="small-label">支出</span><strong class="money-out">¥ <%= moneyText(totalExpense) %></strong></div>
                <div><span class="small-label">最大单笔</span><strong>¥ <%= largestMovement == null ? "0.00" : moneyText(largestMovement.getAmount()) %></strong></div>
            </div>
            <div class="cashflow-row">
                <div class="bar-line">
                    <span>收入</span>
                    <div class="cashflow-track"><div class="cashflow-fill" style="width: <%= percent(totalIncome, maxFlow) %>%"></div></div>
                    <strong>¥ <%= moneyText(totalIncome) %></strong>
                </div>
                <div class="bar-line">
                    <span>支出</span>
                    <div class="cashflow-track"><div class="cashflow-fill" style="width: <%= percent(totalExpense, maxFlow) %>%"></div></div>
                    <strong>¥ <%= moneyText(totalExpense) %></strong>
                </div>
            </div>
        </article>

        <aside class="content-section filter-section transaction-filter-card">
            <div class="section-title">
                <div>
                    <h2>筛选交易</h2>
                    <p class="section-note">先缩小账户、方向和类型，再核对下方资金流动。</p>
                </div>
            </div>
            <form method="get" action="<%= request.getContextPath() %>/transactions" class="form transaction-filter-form">
                <label>
                    <span>账户</span>
                    <select name="accountId">
                        <option value="">全部账户</option>
                        <% if (accounts != null) {
                            for (Account account : accounts) {
                                boolean selected = selectedAccountId != null && selectedAccountId.equals(account.getAccountId());
                        %>
                            <option value="<%= account.getAccountId() %>" <%= selected ? "selected" : "" %>><%= HtmlUtil.escape(account.getAccountNo()) %></option>
                        <%  }
                        } %>
                    </select>
                </label>
                <label>
                    <span>方向</span>
                    <select name="direction">
                        <option value="">全部</option>
                        <option value="IN" <%= "IN".equals(selectedDirection) ? "selected" : "" %>>收入</option>
                        <option value="OUT" <%= "OUT".equals(selectedDirection) ? "selected" : "" %>>支出</option>
                    </select>
                </label>
                <label>
                    <span>交易类型</span>
                    <select name="txnType">
                        <option value="">全部</option>
                        <option value="DEPOSIT" <%= "DEPOSIT".equals(selectedTxnType) ? "selected" : "" %>>存款</option>
                        <option value="WITHDRAW" <%= "WITHDRAW".equals(selectedTxnType) ? "selected" : "" %>>取款</option>
                        <option value="TRANSFER_INNER" <%= "TRANSFER_INNER".equals(selectedTxnType) ? "selected" : "" %>>本行转账</option>
                        <option value="PAYMENT" <%= "PAYMENT".equals(selectedTxnType) ? "selected" : "" %>>缴费</option>
                        <option value="BUY_WEALTH" <%= "BUY_WEALTH".equals(selectedTxnType) ? "selected" : "" %>>理财申购</option>
                        <option value="REDEEM_WEALTH" <%= "REDEEM_WEALTH".equals(selectedTxnType) ? "selected" : "" %>>理财赎回</option>
                    </select>
                </label>
                <div class="transaction-filter-actions">
                    <button class="button primary" type="submit">查询交易</button>
                    <a class="button secondary" href="<%= request.getContextPath() %>/transactions">重置</a>
                </div>
            </form>
        </aside>
    </section>

    <section class="transaction-analysis-grid">
        <article class="content-section transaction-movement-card">
            <div class="section-title">
                <div>
                    <h2>关键资金流动</h2>
                    <p class="section-note">优先展示最近资金动向，表格用于完整核对。</p>
                </div>
            </div>
            <div class="movement-list">
                <% if (entries == null || entries.isEmpty()) { %>
                    <div class="human-empty">
                        <strong>当前筛选条件下没有流水。</strong>
                        <p>可以放宽账户、方向或交易类型筛选；新完成的转账、缴费和理财操作会自动进入这里。</p>
                    </div>
                <% } else {
                    int movementCount = 0;
                    for (LedgerEntryView entry : entries) {
                        if (movementCount >= 6) {
                            break;
                        }
                        movementCount++;
                        boolean income = "IN".equals(entry.getDirection());
                %>
                    <div class="movement-item">
                        <div class="movement-item-main">
                            <strong><%= HtmlUtil.escape(txnTypeName(entry.getTxnType())) %></strong>
                            <p><%= HtmlUtil.escape(entry.getSummary()) %> · <%= HtmlUtil.escape(entry.getAccountNo()) %></p>
                        </div>
                        <strong class="<%= income ? "money-in" : "money-out" %>"><%= income ? "+" : "-" %> ¥ <%= moneyText(entry.getAmount()) %></strong>
                    </div>
                <%  }
                } %>
            </div>
        </article>

        <aside class="content-section transaction-guide-card">
            <div class="section-title">
                <div>
                    <h2>流水说明</h2>
                    <p class="section-note">交易流水来自业务事务落账，转账会产生付款方和收款方两条流水。</p>
                </div>
            </div>
            <ul class="task-checklist">
                <li>收入和支出按流水方向统计，不等同于账户余额。</li>
                <li>如发现异常扣款，可从服务中心发起交易争议工单。</li>
                <li>月账单会按自然月聚合同一套流水数据。</li>
            </ul>
            <div class="transaction-guide-actions">
                <span>常用后续处理</span>
                <a class="button secondary" href="<%= request.getContextPath() %>/bill/report">查看收支报表</a>
                <a class="button secondary" href="<%= request.getContextPath() %>/ticket/create">发起服务工单</a>
                <a class="button secondary" href="<%= request.getContextPath() %>/risk/events">查看风控事件</a>
            </div>
        </aside>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>交易明细</h2>
                <p class="section-note">完整展示交易编号、余额、状态和摘要。</p>
            </div>
        </div>
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
                    <th>交易后余额</th>
                    <th>状态</th>
                    <th>摘要</th>
                </tr>
                </thead>
                <tbody>
                <% if (entries == null || entries.isEmpty()) { %>
                    <tr><td colspan="9" class="empty">当前筛选条件下没有流水，放宽筛选后再试。</td></tr>
                <% } else {
                    for (LedgerEntryView entry : entries) {
                        boolean income = "IN".equals(entry.getDirection());
                %>
                    <tr>
                        <td><%= timeText(entry.getCreatedAt()) %></td>
                        <td><%= HtmlUtil.escape(entry.getTransactionNo()) %></td>
                        <td><%= HtmlUtil.escape(entry.getAccountNo()) %></td>
                        <td><%= HtmlUtil.escape(txnTypeName(entry.getTxnType())) %></td>
                        <td><span class="direction <%= income ? "direction-in" : "direction-out" %>"><%= income ? "收入" : "支出" %></span></td>
                        <td>¥ <%= moneyText(entry.getAmount()) %></td>
                        <td>¥ <%= moneyText(entry.getBalanceAfter()) %></td>
                        <td><span class="status"><%= HtmlUtil.escape(entry.getStatus()) %></span></td>
                        <td><%= HtmlUtil.escape(entry.getSummary()) %></td>
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
