<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.RiskEventView,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
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

    private String decisionName(String decision) {
        if ("WARN".equals(decision)) return "预警通过";
        if ("BLOCK".equals(decision)) return "拦截";
        return decision == null ? "" : decision;
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<RiskEventView> events = (List<RiskEventView>) request.getAttribute("events");
    String error = (String) request.getAttribute("error");
    String selectedDecision = (String) request.getAttribute("selectedDecision");
    int warnCount = 0;
    int blockCount = 0;
    if (events != null) {
        for (RiskEventView event : events) {
            if ("BLOCK".equals(event.getDecision())) {
                blockCount++;
            } else if ("WARN".equals(event.getDecision())) {
                warnCount++;
            }
        }
    }
    request.setAttribute("activeNav", "security");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>风险事件 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">Risk Control</p>
            <h1>风险事件</h1>
            <p class="muted">资金流出交易命中风控时，会保留命中规则、原因和处理轨迹。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/risk/assessment">更新风险测评</a>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="photo-band risk">
        <div>
            <p class="eyebrow">Transaction Check</p>
            <h2>异常交易先说明原因，再给出确认或申诉入口。</h2>
            <p class="section-note">预警通过代表交易已完成但建议关注；拦截代表交易未执行，可查看命中规则后发起风控申诉。</p>
        </div>
    </section>

    <div class="care-note">
        <div>
            <strong>我们发现异常时，会先说明原因，再给出下一步。</strong>
            <p>预警通过代表交易已完成但建议关注；拦截代表交易未执行，可查看命中规则后发起风控申诉。</p>
        </div>
    </div>

    <section class="insight-grid">
        <article class="cashflow-card">
            <div class="section-title">
                <div>
                    <h2>风险概览</h2>
                    <p class="section-note">PASS 交易不落事件表，WARN 和 BLOCK 会进入这里。</p>
                </div>
            </div>
            <div class="cashflow-summary">
                <div><span class="small-label">预警通过</span><strong><%= warnCount %></strong></div>
                <div><span class="small-label">拦截</span><strong><%= blockCount %></strong></div>
                <div><span class="small-label">当前列表</span><strong><%= events == null ? 0 : events.size() %></strong></div>
            </div>
        </article>

        <aside class="content-section filter-section">
            <div class="section-title">
                <div>
                    <h2>筛选事件</h2>
                    <p class="section-note">按决策结果查看需要关注的风险。</p>
                </div>
            </div>
            <form method="get" action="<%= request.getContextPath() %>/risk/events" class="form">
                <label>
                    <span>决策结果</span>
                    <select name="decision">
                        <option value="">全部风险事件</option>
                        <option value="WARN" <%= "WARN".equals(selectedDecision) ? "selected" : "" %>>预警通过</option>
                        <option value="BLOCK" <%= "BLOCK".equals(selectedDecision) ? "selected" : "" %>>拦截</option>
                    </select>
                </label>
                <button class="button primary full" type="submit">查询</button>
            </form>
        </aside>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>风险事件流</h2>
                <p class="section-note">客户侧突出“为什么触发”和“是否被拦截”。</p>
            </div>
        </div>
        <% if (events == null || events.isEmpty()) { %>
            <div class="human-empty">
                <strong>目前没有需要确认的异常交易。</strong>
                <p>当资金流出触发限额、频次或异常规则时，这里会显示原因、交易编号和可处理动作。</p>
            </div>
        <% } else { %>
            <div class="event-feed">
                <% for (RiskEventView event : events) {
                    boolean blocked = "BLOCK".equals(event.getDecision());
                %>
                    <article class="event-card warm-event">
                        <div class="event-card-main">
                            <div class="notification-meta">
                                <span class="direction <%= blocked ? "direction-out" : "direction-in" %>"><%= HtmlUtil.escape(decisionName(event.getDecision())) %></span>
                                <span class="tag"><%= HtmlUtil.escape(event.getRiskLevel()) %></span>
                                <span class="muted"><%= event.getCreatedAt() == null ? "" : event.getCreatedAt().toString().substring(0, 19) %></span>
                            </div>
                            <strong><%= HtmlUtil.escape(txnTypeName(event.getTxnType())) %> · ¥ <%= moneyText(event.getAmount()) %></strong>
                            <p><%= HtmlUtil.escape(event.getReason()) %></p>
                            <p class="cell-note">交易：<%= HtmlUtil.escape(event.getTransactionNo()) %> · 账户：<%= HtmlUtil.escape(event.getAccountNo()) %> · 命中规则：<%= HtmlUtil.escape(event.getHitRules()) %></p>
                        </div>
                        <strong>评分 <%= event.getRiskScore() %></strong>
                    </article>
                <% } %>
            </div>
        <% } %>
    </section>
</main>
</body>
</html>
