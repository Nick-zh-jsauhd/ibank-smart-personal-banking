<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminDashboardMetrics,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminDashboardMetrics metrics = (AdminDashboardMetrics) request.getAttribute("metrics");
    String error = (String) request.getAttribute("error");
    int openAdminAlerts = metrics == null ? 0 : metrics.getOpenAdminAlertCount();
    int openServiceTickets = metrics == null ? 0 : metrics.getOpenServiceTicketCount();
    int openAdjustments = metrics == null ? 0 : metrics.getOpenAdjustmentReviewCount();
    int openReconciliationItems = metrics == null ? 0 : metrics.getOpenReconciliationItemCount();
    int openActionCount = openAdminAlerts + openServiceTickets + openAdjustments + openReconciliationItems;
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>后台首页 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout layout-wide">
    <section class="dashboard-hero admin-ops-hero">
        <div class="hero-panel">
            <p class="eyebrow">运营指挥台</p>
            <h1>运营控制台</h1>
            <p class="muted">把客户、交易、风控、工单、对账和调账收敛成一张运营看板，优先处理需要人工介入的业务风险。</p>
            <div class="admin-hero-actions">
                <% if (adminUser.hasPermission("ADMIN_ALERT_VIEW")) { %><a class="button primary" href="<%= request.getContextPath() %>/admin/messages">处理消息待办</a><% } %>
                <% if (adminUser.hasPermission("TICKET_VIEW")) { %><a class="button secondary" href="<%= request.getContextPath() %>/admin/tickets">查看服务工单</a><% } %>
                <% if (adminUser.hasPermission("RECONCILIATION_VIEW")) { %><a class="button secondary" href="<%= request.getContextPath() %>/admin/reconciliation">进入账务对账</a><% } %>
            </div>
        </div>
        <aside class="hero-summary admin-health-panel">
            <div class="admin-health-score">
                <span>今日运营脉搏</span>
                <strong><%= openActionCount %></strong>
                <p>需要人工跟进的任务</p>
            </div>
            <div class="hero-mini-grid">
                <div class="hero-mini">
                    <span>消息待办</span>
                    <strong><%= openAdminAlerts %></strong>
                </div>
                <div class="hero-mini">
                    <span>工单跟进</span>
                    <strong><%= openServiceTickets %></strong>
                </div>
                <div class="hero-mini">
                    <span>调账/对账</span>
                    <strong><%= openAdjustments + openReconciliationItems %></strong>
                </div>
                <div class="hero-mini">
                    <span>站内通知</span>
                    <strong><%= metrics == null ? 0 : metrics.getUnreadNotificationCount() %></strong>
                </div>
            </div>
        </aside>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (metrics != null) { %>
        <section class="metric-grid">
            <article class="metric-card">
                <span>客户总数</span>
                <strong><%= metrics.getCustomerCount() %></strong>
            </article>
            <article class="metric-card">
                <span>账户总数</span>
                <strong><%= metrics.getAccountCount() %></strong>
            </article>
            <article class="metric-card">
                <span>今日交易笔数</span>
                <strong><%= metrics.getTodayTransactionCount() %></strong>
            </article>
            <article class="metric-card">
                <span>今日交易金额</span>
                <strong>¥ <%= moneyText(metrics.getTodayTransactionAmount()) %></strong>
            </article>
            <article class="metric-card">
                <span>今日风控拦截</span>
                <strong><%= metrics.getTodayRiskBlockCount() %></strong>
            </article>
            <article class="metric-card">
                <span>全站未读通知</span>
                <strong><%= metrics.getUnreadNotificationCount() %></strong>
            </article>
            <article class="metric-card">
                <span>后台待处理消息</span>
                <strong><%= metrics.getOpenAdminAlertCount() %></strong>
            </article>
            <article class="metric-card warning">
                <span>服务工单跟进</span>
                <strong><%= metrics.getOpenServiceTicketCount() %></strong>
            </article>
            <article class="metric-card warning">
                <span>调账待处理</span>
                <strong><%= metrics.getOpenAdjustmentReviewCount() %></strong>
            </article>
            <article class="metric-card">
                <span>对账异常未闭环</span>
                <strong><%= metrics.getOpenReconciliationItemCount() %></strong>
            </article>
            <article class="metric-card danger">
                <span>今日风控拦截</span>
                <strong><%= metrics.getTodayRiskBlockCount() %></strong>
            </article>
            <article class="metric-card">
                <span>理财持仓本金</span>
                <strong>¥ <%= moneyText(metrics.getWealthHoldingPrincipal()) %></strong>
            </article>
        </section>
    <% } %>

    <section class="admin-work-grid">
        <article class="content-section admin-priority-card">
            <div class="section-title">
                <div>
                    <h2>今日必须处理</h2>
                    <p class="section-note">按“客户影响、资金风险、审计责任”排序，先处理会阻塞业务闭环的事项。</p>
                </div>
            </div>
            <div class="admin-task-queue">
                <% if (adminUser.hasPermission("ADMIN_ALERT_VIEW")) { %>
                    <a class="admin-task-card" href="<%= request.getContextPath() %>/admin/messages?status=NEW">
                        <span class="task-rank">01</span>
                        <div>
                            <strong>先接收后台待办</strong>
                            <p>把新消息确认到责任人名下，避免调账、工单和风控告警无人承接。</p>
                        </div>
                        <em><%= openAdminAlerts %></em>
                    </a>
                <% } %>
                <% if (adminUser.hasPermission("TICKET_VIEW")) { %>
                    <a class="admin-task-card" href="<%= request.getContextPath() %>/admin/tickets">
                        <span class="task-rank">02</span>
                        <div>
                            <strong>跟进客户服务工单</strong>
                            <p>交易争议和风控申诉需要给客户明确状态，必要时发起调账申请。</p>
                        </div>
                        <em><%= openServiceTickets %></em>
                    </a>
                <% } %>
                <% if (adminUser.hasPermission("ADJUSTMENT_REVIEW")) { %>
                    <a class="admin-task-card" href="<%= request.getContextPath() %>/admin/adjustments?status=PENDING_REVIEW">
                        <span class="task-rank">03</span>
                        <div>
                            <strong>复核调账申请</strong>
                            <p>资金修正必须先复核再执行，确保客户余额、流水和审计链路一致。</p>
                        </div>
                        <em><%= openAdjustments %></em>
                    </a>
                <% } %>
                <% if (adminUser.hasPermission("RISK_EVENT_VIEW")) { %>
                    <a class="admin-task-card" href="<%= request.getContextPath() %>/admin/risk/events">
                        <span class="task-rank">04</span>
                        <div>
                            <strong>核查异常交易事件</strong>
                            <p>优先确认大额、频繁、夜间或规则命中的交易，必要时冻结账户或转入工单。</p>
                        </div>
                        <em><%= metrics == null ? 0 : metrics.getTodayRiskBlockCount() %></em>
                    </a>
                <% } %>
            </div>
        </article>

        <article class="content-section admin-flow-card">
            <div class="section-title">
                <div>
                    <h2>业务闭环</h2>
                    <p class="section-note">后台处理从发现问题到留痕审计，所有动作都应可追踪。</p>
                </div>
            </div>
            <div class="admin-flow-list">
                <div><span>01</span><strong>发现</strong><p>消息、风控事件、工单、对账批次暴露异常。</p></div>
                <div><span>02</span><strong>处置</strong><p>按角色权限进行受理、复核、规则调整或产品维护。</p></div>
                <div><span>03</span><strong>闭环</strong><p>通知客户、更新状态，并进入审计日志和处理轨迹。</p></div>
            </div>
        </article>
    </section>

    <section class="section-block">
        <div class="section-title">
            <div>
                <h2>常用工作入口</h2>
                <p class="section-note">按运营、风控、账务、客服和审计的实际处理路径组织。</p>
            </div>
        </div>
        <div class="action-grid">
            <% if (adminUser.hasPermission("ADMIN_ALERT_VIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/messages"><strong>消息待办</strong><span>查看并处理后台业务待办</span></a><% } %>
            <% if (adminUser.hasPermission("TICKET_VIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/tickets"><strong>服务工单</strong><span>处理交易争议与客户服务请求</span></a><% } %>
            <% if (adminUser.hasPermission("RISK_EVENT_VIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/risk/events"><strong>风控事件</strong><span>核实异常交易和账户控制</span></a><% } %>
            <% if (adminUser.hasPermission("ADJUSTMENT_REVIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/adjustments"><strong>调账复核</strong><span>复核或执行账务修正申请</span></a><% } %>
            <% if (adminUser.hasPermission("RECONCILIATION_RUN")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/reconciliation"><strong>账务对账</strong><span>发起批次检查并处理差异</span></a><% } %>
            <% if (adminUser.hasPermission("CUSTOMER_VIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/customers"><strong>客户管理</strong><span>查看客户档案和风险等级</span></a><% } %>
            <% if (adminUser.hasPermission("WEALTH_PRODUCT_UPDATE")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/wealth/products"><strong>理财产品</strong><span>管理产品上下架和额度</span></a><% } %>
            <% if (adminUser.hasPermission("WEALTH_SETTLEMENT_VIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/wealth/settlement"><strong>理财清算</strong><span>确认申购、处理赎回到账和持仓状态</span></a><% } %>
            <% if (adminUser.hasPermission("ADMIN_AUDIT_VIEW")) { %><a class="action-card" href="<%= request.getContextPath() %>/admin/audit"><strong>审计中心</strong><span>追踪登录和后台高风险操作</span></a><% } %>
        </div>
    </section>
</main>
</body>
</html>
