<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.SimulationRun,com.bank.dto.AdminDashboardMetrics,com.bank.dto.AdminSessionUser,com.bank.dto.SimulationDashboardView,com.bank.util.AdminRoleDisplayUtil,com.bank.util.HtmlUtil,com.bank.util.StatusDisplayUtil,java.util.Set" %>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminDashboardMetrics metrics = (AdminDashboardMetrics) request.getAttribute("metrics");
    SimulationDashboardView simulationView = (SimulationDashboardView) request.getAttribute("simulationView");
    SimulationRun selectedRun = simulationView == null ? null : simulationView.getSelectedRun();
    String ctx = request.getContextPath();
    String adminName = adminUser == null ? "" : HtmlUtil.escape(adminUser.getUsername());
    Set<String> roleCodes = adminUser == null ? null : adminUser.getRoleCodes();
    String primaryRoleName = AdminRoleDisplayUtil.primaryRoleName(roleCodes);
    String roleScopeText = AdminRoleDisplayUtil.scopeText(roleCodes);
    String roleCountText = AdminRoleDisplayUtil.roleCountText(roleCodes);
    String latestRunStatus = selectedRun == null ? "暂无批次" : HtmlUtil.escape(StatusDisplayUtil.simulationRunStatus(selectedRun.getStatus()));
    int openAlerts = metrics == null ? 0 : metrics.getOpenAdminAlertCount();
    int openTickets = metrics == null ? 0 : metrics.getOpenServiceTicketCount();
    int openAdjustments = metrics == null ? 0 : metrics.getOpenAdjustmentReviewCount();
    int openReconciliations = metrics == null ? 0 : metrics.getOpenReconciliationItemCount();
    int operationsQueue = openAlerts + openTickets + openAdjustments + openReconciliations;
    int graphCases = metrics == null ? 0 : metrics.getOpenRiskGraphReviewCaseCount();
    int simulationRuns = simulationView == null ? 0 : simulationView.getTotalRunCount();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>平台选择 - iBank</title>
    <link rel="stylesheet" href="<%= ctx %>/assets/css/main.css">
</head>
<body class="admin-page platform-select-page">
<main class="platform-gateway">
    <header class="platform-gateway-top">
        <a class="brand platform-brand" href="<%= ctx %>/">
            <span class="cover-brand-mark"></span>
            <strong>iBank</strong>
        </a>
        <nav class="platform-top-links" aria-label="后台平台导航">
            <a href="<%= ctx %>/admin/dashboard">运营管理</a>
            <a href="<%= ctx %>/admin/lab">智能实验室</a>
        </nav>
        <div class="platform-user-pill">
            <span><%= adminName %></span>
            <a href="<%= ctx %>/admin/logout">退出</a>
        </div>
    </header>

    <section class="platform-gateway-hero">
        <div class="platform-gateway-copy">
            <p class="eyebrow">后台工作台</p>
            <h1>先选择职责，再进入对应平台</h1>
            <p class="platform-hero-lead">运营平台处理客户影响、资金闭环和审计责任；智能实验室沉淀模型评分、图谱证据和仿真演练。两套工作域共享登录和留痕，但不混在同一条业务线上。</p>
            <div class="platform-hero-actions">
                <a class="platform-primary-action" href="<%= ctx %>/admin/dashboard">进入运营管理平台</a>
                <a class="platform-secondary-action" href="<%= ctx %>/admin/lab">进入智能风控实验室</a>
            </div>
        </div>
        <aside class="platform-gateway-status" aria-label="当前后台状态">
            <div class="platform-status-head">
                <span>当前身份</span>
                <strong><%= primaryRoleName %></strong>
                <div class="platform-role-line">
                    <em><%= roleScopeText %></em>
                    <em><%= roleCountText %></em>
                </div>
            </div>
            <p>系统会根据你的角色权限开放模块。高风险操作、模型复核和资金处理都会进入审计链路。</p>
            <div class="platform-status-grid">
                <div>
                    <span>待办队列</span>
                    <strong><%= operationsQueue %></strong>
                </div>
                <div>
                    <span>今日交易</span>
                    <strong><%= metrics == null ? 0 : metrics.getTodayTransactionCount() %></strong>
                </div>
                <div>
                    <span>复核样本</span>
                    <strong><%= graphCases %></strong>
                </div>
            </div>
        </aside>
    </section>

    <section class="platform-choice-grid" aria-label="后台平台入口">
        <article class="platform-choice-card operations">
            <div class="platform-choice-tag">业务运营</div>
            <div class="platform-choice-meta">
                <strong>运营管理平台</strong>
                <p>面向客户运营和后台审批人员，承接消息待办、服务工单、风控事件、对账调账、理财清算和审计追踪。</p>
            </div>
            <div class="platform-choice-metrics">
                <div><span>消息待办</span><strong><%= openAlerts %></strong></div>
                <div><span>服务工单</span><strong><%= openTickets %></strong></div>
                <div><span>调账复核</span><strong><%= openAdjustments %></strong></div>
                <div><span>对账异常</span><strong><%= openReconciliations %></strong></div>
            </div>
            <div class="platform-choice-links">
                <a href="<%= ctx %>/admin/messages">消息待办</a>
                <a href="<%= ctx %>/admin/tickets">服务工单</a>
                <a href="<%= ctx %>/admin/risk/events">风控事件</a>
                <a href="<%= ctx %>/admin/reconciliation">账务对账</a>
            </div>
            <div class="platform-choice-footer">
                <a class="platform-card-cta" href="<%= ctx %>/admin/dashboard">进入运营工作台</a>
                <em>业务处置 / 资金闭环 / 客户服务</em>
            </div>
        </article>

        <article class="platform-choice-card lab">
            <div class="platform-choice-tag">智能风控</div>
            <div class="platform-choice-meta">
                <strong>智能风控实验室</strong>
                <p>面向模型和技术人员，管理图谱评分、模型版本、人工反馈样本和实时仿真沙盘，输出可解释风险洞察。</p>
            </div>
            <div class="platform-choice-metrics">
                <div><span>复核样本</span><strong><%= graphCases %></strong></div>
                <div><span>仿真批次</span><strong><%= simulationRuns %></strong></div>
                <div><span>最新批次</span><strong><%= latestRunStatus %></strong></div>
                <div><span>风控拦截</span><strong><%= metrics == null ? 0 : metrics.getTodayRiskBlockCount() %></strong></div>
            </div>
            <div class="platform-choice-links">
                <a href="<%= ctx %>/admin/risk/graph-scores">图谱评分</a>
                <a href="<%= ctx %>/admin/risk/graph-models">模型治理</a>
                <a href="<%= ctx %>/admin/risk/graph-cases">反馈样本</a>
                <a href="<%= ctx %>/admin/simulation">仿真沙盘</a>
            </div>
            <div class="platform-choice-footer">
                <a class="platform-card-cta" href="<%= ctx %>/admin/lab">进入实验室首页</a>
                <em>模型治理 / 图谱证据 / 仿真演练</em>
            </div>
        </article>
    </section>

    <section class="platform-boundary-panel">
        <div class="platform-boundary-copy">
            <span>职责边界</span>
            <strong>模型给出线索，运营做出处置</strong>
            <p>实验室只输出评分、证据和策略建议；涉及客户通知、资金调整、账户控制和审计结论时，必须回到运营管理平台完成闭环。</p>
        </div>
        <div class="platform-boundary-flow" aria-label="风险处理闭环">
            <span><b>01</b>模型评分</span>
            <span><b>02</b>人工复核</span>
            <span><b>03</b>运营处置</span>
            <span><b>04</b>审计留痕</span>
        </div>
    </section>
</main>
</body>
</html>
