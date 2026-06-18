<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.SimulationRun,com.bank.dto.AdminDashboardMetrics,com.bank.dto.AdminSessionUser,com.bank.dto.SimulationDashboardView,com.bank.util.HtmlUtil,com.bank.util.StatusDisplayUtil" %>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminDashboardMetrics metrics = (AdminDashboardMetrics) request.getAttribute("metrics");
    SimulationDashboardView simulationView = (SimulationDashboardView) request.getAttribute("simulationView");
    SimulationRun selectedRun = simulationView == null ? null : simulationView.getSelectedRun();
    int graphCases = metrics == null ? 0 : metrics.getOpenRiskGraphReviewCaseCount();
    int simulationRuns = simulationView == null ? 0 : simulationView.getTotalRunCount();
    int simulationEvents = simulationView == null ? 0 : simulationView.getTotalEventCount();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>智能风控实验室 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page lab-page">
<%@ include file="/WEB-INF/jsp/adminLabTopbar.jspf" %>

<main class="layout layout-wide lab-dashboard">
    <section class="lab-hero">
        <div class="lab-hero-copy">
            <p class="eyebrow">智能风控实验室</p>
            <h1>智能风控实验室</h1>
            <p>把数据集、图谱评分、模型版本、人工反馈和仿真演练集中到技术侧平台。这里生产风险判断能力，不直接替代运营处置。</p>
            <div class="lab-hero-actions">
                <% if (adminUser.hasPermission("RISK_GRAPH_SCORE_VIEW")) { %>
                    <a class="button primary" href="<%= request.getContextPath() %>/admin/risk/graph-scores">查看图谱评分</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/admin/risk/graph-models">模型治理</a>
                <% } %>
                <% if (adminUser.hasPermission("ADMIN_DASHBOARD_VIEW")) { %>
                    <a class="button secondary" href="<%= request.getContextPath() %>/admin/simulation">启动仿真沙盘</a>
                <% } %>
            </div>
        </div>
        <aside class="lab-hero-console">
            <span>实验室运行状态</span>
            <strong><%= selectedRun == null ? "待启动" : HtmlUtil.escape(StatusDisplayUtil.simulationRunStatus(selectedRun.getStatus())) %></strong>
            <p><%= selectedRun == null ? "启动仿真后，这里会显示最新批次状态。" : HtmlUtil.escape(selectedRun.getScenarioName()) %></p>
            <div class="lab-console-grid">
                <div><span>复核样本</span><strong><%= graphCases %></strong></div>
                <div><span>仿真批次</span><strong><%= simulationRuns %></strong></div>
                <div><span>仿真事件</span><strong><%= simulationEvents %></strong></div>
            </div>
        </aside>
    </section>

    <section class="lab-system-map">
        <article class="lab-map-card model">
            <span>01</span>
            <strong>模型评分</strong>
            <p>查看不同模型版本的风险边排序、BLOCK/REVIEW/PASS 分布和局部图谱证据。</p>
            <% if (adminUser.hasPermission("RISK_GRAPH_SCORE_VIEW")) { %>
                <a href="<%= request.getContextPath() %>/admin/risk/graph-scores">进入评分看板</a>
            <% } %>
        </article>
        <article class="lab-map-card governance">
            <span>02</span>
            <strong>模型治理</strong>
            <p>比较候选模型与运营模型，管理上线模式、阈值容量和治理备注。</p>
            <% if (adminUser.hasPermission("RISK_GRAPH_SCORE_VIEW")) { %>
                <a href="<%= request.getContextPath() %>/admin/risk/graph-models">进入模型治理</a>
            <% } %>
        </article>
        <article class="lab-map-card review">
            <span>03</span>
            <strong>反馈样本</strong>
            <p>把标签冲突和高分样本交给人工解释，形成后续训练增强的可信反馈。</p>
            <% if (adminUser.hasPermission("RISK_GRAPH_CASE_VIEW")) { %>
                <a href="<%= request.getContextPath() %>/admin/risk/graph-cases">进入复核队列</a>
            <% } %>
        </article>
        <article class="lab-map-card simulation">
            <span>04</span>
            <strong>仿真沙盘</strong>
            <p>启动连续事件流，观察客户现金流、发薪日、市场波动和风控压力的链路反应。</p>
            <% if (adminUser.hasPermission("ADMIN_DASHBOARD_VIEW")) { %>
                <a href="<%= request.getContextPath() %>/admin/simulation">进入仿真沙盘</a>
            <% } %>
        </article>
    </section>

    <section class="lab-work-grid">
        <article class="content-section lab-boundary-card">
            <div class="section-title">
                <div>
                    <p class="eyebrow">治理边界</p>
                    <h2>实验室不直接处置客户资金</h2>
                    <p class="section-note">实验室负责产生评分、证据、评估和仿真结论；强动作仍需要进入运营平台的风控事件、工单或调账流程。</p>
                </div>
            </div>
            <div class="lab-boundary-steps">
                <div><span>Model</span><strong>输出风险线索</strong><p>模型分、置信度、图谱邻域和原因说明。</p></div>
                <div><span>Review</span><strong>人工解释样本</strong><p>确认风险、误报、需补证据或忽略。</p></div>
                <div><span>Operate</span><strong>运营闭环处理</strong><p>由真实业务队列承接客户影响和资金动作。</p></div>
            </div>
        </article>
        <aside class="content-section lab-next-card">
            <h2>建议工作顺序</h2>
            <div class="lab-next-list">
                <a href="<%= request.getContextPath() %>/admin/risk/graph-scores"><strong>先看运营模型评分</strong><span>确认当前默认模型输出的高风险交易边</span></a>
                <a href="<%= request.getContextPath() %>/admin/risk/graph-cases"><strong>再处理反馈样本</strong><span>把标签冲突变成可解释的人工结论</span></a>
                <a href="<%= request.getContextPath() %>/admin/simulation"><strong>最后用沙盘演练</strong><span>验证新策略会不会冲击运营队列</span></a>
            </div>
        </aside>
    </section>
</main>
</body>
</html>
