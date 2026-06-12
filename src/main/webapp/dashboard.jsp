<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.bean.ServiceTicket,com.bank.dto.CategorySummary,com.bank.dto.CustomerDashboardView,com.bank.dto.CustomerInsightSummary,com.bank.dto.LedgerEntryView,com.bank.dto.MonthlyBillSummary,com.bank.dto.RiskEventView,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.sql.Timestamp,java.util.List" %>
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
        return txnType == null ? "其他交易" : txnType;
    }

    private String ticketStatusName(String status) {
        if ("SUBMITTED".equals(status)) return "已提交";
        if ("ACCEPTED".equals(status)) return "已受理";
        if ("INVESTIGATING".equals(status)) return "调查中";
        if ("WAITING_CUSTOMER".equals(status)) return "待补充";
        if ("RESOLVED".equals(status)) return "已处理";
        if ("CLOSED".equals(status)) return "已关闭";
        if ("REOPENED".equals(status)) return "已重开";
        if ("REJECTED".equals(status)) return "不予受理";
        return status == null ? "" : status;
    }

    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String greetingText() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 11) return "早上好";
        if (hour < 14) return "中午好";
        if (hour < 18) return "下午好";
        return "晚上好";
    }

    private int percent(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int value = amount.multiply(new BigDecimal("100")).divide(total, 0, RoundingMode.HALF_UP).intValue();
        if (value > 100) {
            return 100;
        }
        return value < 4 ? 4 : value;
    }

    private String riskSourceName(String source) {
        if ("ASSESSMENT".equals(source)) return "来自风险测评";
        if ("ADMIN".equals(source)) return "管理员调整";
        return "系统默认";
    }
%>
<%
    CustomerDashboardView view = (CustomerDashboardView) request.getAttribute("dashboardView");
    if (view == null) {
        view = new CustomerDashboardView();
    }
    CustomerInsightSummary insight = view.getInsight() == null ? new CustomerInsightSummary() : view.getInsight();
    MonthlyBillSummary bill = view.getMonthlyBill() == null ? new MonthlyBillSummary() : view.getMonthlyBill();
    List<Account> accounts = view.getAccounts();
    List<LedgerEntryView> recentLedgers = view.getRecentLedgers();
    List<ServiceTicket> recentTickets = view.getRecentTickets();
    List<RiskEventView> recentRiskEvents = view.getRecentRiskEvents();
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    String displayName = loginUser != null && loginUser.getFullName() != null && loginUser.getFullName().trim().length() > 0 ? loginUser.getFullName().trim() : "客户";
    String error = (String) request.getAttribute("error");
    request.setAttribute("activeNav", "dashboard");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>工作台 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>
    <% if (view.getLoadWarning() != null && view.getLoadWarning().length() > 0) { %>
        <div class="alert danger"><%= HtmlUtil.escape(view.getLoadWarning()) %></div>
    <% } %>

    <section class="insight-hero">
        <article class="insight-main-card warm-dashboard">
            <div>
                <p class="human-greeting"><%= greetingText() %>，<%= HtmlUtil.escape(displayName) %></p>
                <p class="eyebrow">iBank Warm Finance</p>
                <h1>今天先看账户、账单和安全提醒。</h1>
                <p class="muted">我们把资产、现金流、服务待办和异常交易放在一起，帮你判断下一步最值得关注的事。</p>
                <span class="balance-figure">¥ <%= moneyText(view.getTotalAvailableBalance()) %></span>
                <p class="section-note">当前账户 <%= accounts == null ? 0 : accounts.size() %> 个，本月账单 <%= HtmlUtil.escape(bill.getYearMonth()) %> 已同步汇总。</p>
            </div>
            <div class="insight-stat-grid">
                <div class="insight-stat">
                    <span>本月收入</span>
                    <strong>¥ <%= moneyText(insight.getMonthlyIncome()) %></strong>
                </div>
                <div class="insight-stat">
                    <span>本月支出</span>
                    <strong>¥ <%= moneyText(insight.getMonthlyExpense()) %></strong>
                </div>
                <div class="insight-stat">
                    <span>净流入</span>
                    <strong>¥ <%= moneyText(insight.getMonthlyNetIncome()) %></strong>
                </div>
                <div class="insight-stat">
                    <span>未读待办</span>
                    <strong><%= view.getUnreadNotificationCount() %></strong>
                </div>
            </div>
        </article>

        <aside class="insight-side-card">
            <div class="risk-meter">
                <div class="risk-score">
                    <div>
                        <span class="small-label">客户风险等级</span>
                        <strong><%= HtmlUtil.escape(view.getCustomerRiskLevel()) %></strong>
                    </div>
                    <span class="tag"><%= HtmlUtil.escape(riskSourceName(view.getRiskLevelSource())) %></span>
                </div>
                <div class="risk-meter-track"><div class="risk-meter-fill" style="width: <%= "C1".equals(view.getCustomerRiskLevel()) ? 28 : ("C2".equals(view.getCustomerRiskLevel()) ? 52 : ("C3".equals(view.getCustomerRiskLevel()) ? 72 : 92)) %>%"></div></div>
                <p class="section-note">风险等级会影响理财申购和资金流出限额，异常事件会进入通知与工单闭环。</p>
            </div>
            <div class="quick-link-grid">
                <a class="quick-link" href="<%= request.getContextPath() %>/deposit"><strong>存款</strong><span>模拟入账</span></a>
                <a class="quick-link" href="<%= request.getContextPath() %>/payment"><strong>缴费</strong><span>生活账单</span></a>
                <a class="quick-link" href="<%= request.getContextPath() %>/notifications"><strong>通知</strong><span><%= view.getUnreadNotificationCount() %> 条未读</span></a>
                <a class="quick-link" href="<%= request.getContextPath() %>/risk/events"><strong>风控</strong><span><%= recentRiskEvents == null ? 0 : recentRiskEvents.size() %> 条事件</span></a>
            </div>
        </aside>
    </section>

    <section class="today-focus-grid">
        <a class="today-focus-card" href="<%= request.getContextPath() %>/notifications">
            <span>今天建议关注</span>
            <strong><%= view.getUnreadNotificationCount() %> 条未读</strong>
            <p>交易、安全和服务消息都会先在这里提醒你。</p>
        </a>
        <a class="today-focus-card" href="<%= request.getContextPath() %>/tickets">
            <span>服务跟进</span>
            <strong><%= view.getWaitingTicketCount() %> 个待补充</strong>
            <p>如果后台需要材料，补充后会继续处理。</p>
        </a>
        <a class="today-focus-card" href="<%= request.getContextPath() %>/risk/events">
            <span>安全提醒</span>
            <strong><%= recentRiskEvents == null ? 0 : recentRiskEvents.size() %> 条事件</strong>
            <p>异常交易会说明原因，并给出确认或申诉入口。</p>
        </a>
        <a class="today-focus-card" href="<%= request.getContextPath() %>/risk/assessment">
            <span>财富匹配</span>
            <strong><%= HtmlUtil.escape(view.getCustomerRiskLevel()) %></strong>
            <p>更新风险测评后，理财产品会更容易匹配你的承受能力。</p>
        </a>
    </section>

    <section class="insight-grid">
        <article class="cashflow-card">
            <div class="section-title">
                <div>
                    <h2>本月现金流</h2>
                    <p class="section-note">按当前月流水聚合，不额外生成账单数据。</p>
                </div>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/bill/report">查看收支报表</a>
            </div>
            <div class="cashflow-summary">
                <div><span class="small-label">收入笔数</span><strong><%= insight.getIncomeCount() %></strong></div>
                <div><span class="small-label">支出笔数</span><strong><%= insight.getExpenseCount() %></strong></div>
                <div><span class="small-label">最大支出</span><strong>¥ <%= insight.getLargestOutflow() == null ? "0.00" : moneyText(insight.getLargestOutflow().getAmount()) %></strong></div>
            </div>
            <div class="cashflow-row">
                <div class="bar-line">
                    <span>收入</span>
                    <div class="cashflow-track"><div class="cashflow-fill" style="width: <%= percent(insight.getMonthlyIncome(), insight.getMonthlyIncome().max(insight.getMonthlyExpense())) %>%"></div></div>
                    <strong>¥ <%= moneyText(insight.getMonthlyIncome()) %></strong>
                </div>
                <div class="bar-line">
                    <span>支出</span>
                    <div class="cashflow-track"><div class="cashflow-fill" style="width: <%= percent(insight.getMonthlyExpense(), insight.getMonthlyIncome().max(insight.getMonthlyExpense())) %>%"></div></div>
                    <strong>¥ <%= moneyText(insight.getMonthlyExpense()) %></strong>
                </div>
            </div>
        </article>

        <article class="movement-card">
            <div class="section-title">
                <div>
                    <h2>待办与提醒</h2>
                    <p class="section-note">服务、风控和通知统一汇聚，优先处理待补充和未读事项。</p>
                </div>
            </div>
            <div class="movement-list">
                <a class="movement-item" href="<%= request.getContextPath() %>/tickets">
                    <div class="movement-item-main">
                        <strong>服务工单</strong>
                        <p>处理中 <%= view.getActiveTicketCount() %>，待补充 <%= view.getWaitingTicketCount() %>，已处理 <%= view.getResolvedTicketCount() %></p>
                    </div>
                    <span class="tag">服务</span>
                </a>
                <a class="movement-item" href="<%= request.getContextPath() %>/notifications">
                    <div class="movement-item-main">
                        <strong>站内通知</strong>
                        <p>交易、理财、风控和服务消息会在这里形成待办。</p>
                    </div>
                    <span class="tag"><%= view.getUnreadNotificationCount() %> 未读</span>
                </a>
                <a class="movement-item" href="<%= request.getContextPath() %>/risk/assessment">
                    <div class="movement-item-main">
                        <strong>风险测评</strong>
                        <p>当前等级 <%= HtmlUtil.escape(view.getCustomerRiskLevel()) %>，可更新测评以匹配理财产品。</p>
                    </div>
                    <span class="tag">测评</span>
                </a>
            </div>
        </article>
    </section>

    <section class="dashboard-columns">
        <article class="content-section">
            <div class="section-title">
                <div>
                    <h2>近期资金动向</h2>
                    <p class="section-note">最近交易以资金方向和业务摘要为主，方便快速判断现金流。</p>
                </div>
                <a class="button secondary compact" href="<%= request.getContextPath() %>/transactions">全部流水</a>
            </div>
            <div class="movement-list">
                <% if (recentLedgers == null || recentLedgers.isEmpty()) { %>
                    <div class="human-empty">
                        <strong>最近还没有新的资金动向。</strong>
                        <p>完成转账、缴费、存款或理财操作后，这里会自动显示最近流水，方便你核对每一笔变化。</p>
                    </div>
                <% } else {
                    for (LedgerEntryView entry : recentLedgers) {
                        boolean income = "IN".equals(entry.getDirection());
                %>
                    <div class="movement-item">
                        <div class="movement-item-main">
                            <strong><%= HtmlUtil.escape(txnTypeName(entry.getTxnType())) %></strong>
                            <p><%= HtmlUtil.escape(entry.getSummary()) %> · <%= timeText(entry.getCreatedAt()) %></p>
                        </div>
                        <strong class="<%= income ? "money-in" : "money-out" %>"><%= income ? "+" : "-" %> ¥ <%= moneyText(entry.getAmount()) %></strong>
                    </div>
                <%  }
                } %>
            </div>
        </article>

        <aside>
            <article class="content-section">
                <div class="section-title">
                    <div>
                        <h2>分类支出</h2>
                        <p class="section-note">以本月支出总额为基准展示占比。</p>
                    </div>
                </div>
                <div class="category-list">
                    <% if (bill.getCategories() == null || bill.getCategories().isEmpty()) { %>
                        <div class="human-empty">
                            <strong>这个月还没有形成支出分类。</strong>
                            <p>生活缴费、转账和取款完成后，月账单会把支出按类型汇总出来。</p>
                        </div>
                    <% } else {
                        for (CategorySummary category : bill.getCategories()) {
                            if (!"OUT".equals(category.getDirection())) {
                                continue;
                            }
                    %>
                        <div class="category-item">
                            <div class="category-head">
                                <span><%= HtmlUtil.escape(txnTypeName(category.getTxnType())) %></span>
                                <strong>¥ <%= moneyText(category.getTotalAmount()) %></strong>
                            </div>
                            <div class="cashflow-track"><div class="cashflow-fill" style="width: <%= percent(category.getTotalAmount(), bill.getTotalExpense()) %>%"></div></div>
                        </div>
                    <%  }
                    } %>
                </div>
            </article>

            <article class="content-section">
                <div class="section-title">
                    <div>
                        <h2>服务进展</h2>
                        <p class="section-note">展示最近三条服务请求。</p>
                    </div>
                    <a class="button secondary compact" href="<%= request.getContextPath() %>/ticket/create">发起工单</a>
                </div>
                <div class="event-feed">
                    <% if (recentTickets == null || recentTickets.isEmpty()) { %>
                        <div class="human-empty">
                            <strong>目前没有需要跟进的服务请求。</strong>
                            <p>如果遇到交易争议、风控申诉或账户问题，可以发起工单，处理轨迹会留在这里。</p>
                        </div>
                    <% } else {
                        for (ServiceTicket ticket : recentTickets) {
                    %>
                        <a class="event-card" href="<%= request.getContextPath() %>/ticket/detail?ticketId=<%= ticket.getTicketId() %>">
                            <div class="event-card-main">
                                <strong><%= HtmlUtil.escape(ticket.getTitle()) %></strong>
                                <p><%= HtmlUtil.escape(ticket.getTicketNo()) %> · <%= timeText(ticket.getUpdatedAt()) %></p>
                            </div>
                            <span class="status"><%= HtmlUtil.escape(ticketStatusName(ticket.getStatus())) %></span>
                        </a>
                    <%  }
                    } %>
                </div>
            </article>
        </aside>
    </section>
</main>
</body>
</html>
