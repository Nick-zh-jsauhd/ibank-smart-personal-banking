<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Customer,com.bank.bean.RiskAssessment,com.bank.util.HtmlUtil" %>
<%!
    private boolean isValid(RiskAssessment assessment) {
        return assessment != null && assessment.getEffectiveUntil() != null
                && assessment.getEffectiveUntil().getTime() >= System.currentTimeMillis();
    }

    private String sourceName(String source) {
        if ("ASSESSMENT".equals(source)) return "风险测评";
        if ("ADMIN".equals(source)) return "管理员调整";
        if ("SYSTEM".equals(source)) return "系统默认";
        return source == null ? "" : source;
    }
%>
<%
    Customer customer = (Customer) request.getAttribute("customer");
    RiskAssessment latestAssessment = (RiskAssessment) request.getAttribute("latestAssessment");
    String error = (String) request.getAttribute("error");
    String success = (String) request.getAttribute("success");
    String currentRiskLevel = customer == null ? "C2" : customer.getRiskLevel();
    String source = customer == null ? "SYSTEM" : customer.getRiskLevelSource();
    request.setAttribute("activeNav", "wealth");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>风险测评 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">风险测评</p>
            <h1>客户风险测评</h1>
            <p class="muted">完成投资者适当性问卷后，系统会更新风险承受能力等级，并用于理财产品购买校验。</p>
        </div>
        <div class="section-actions">
            <a class="button secondary compact" href="<%= request.getContextPath() %>/wealth/products">理财产品</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/wealth/holdings">我的持仓</a>
        </div>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="metric-grid">
        <article class="metric-card">
            <span>当前风险等级</span>
            <strong><%= HtmlUtil.escape(currentRiskLevel) %></strong>
        </article>
        <article class="metric-card">
            <span>等级来源</span>
            <strong><%= HtmlUtil.escape(sourceName(source)) %></strong>
        </article>
        <article class="metric-card">
            <span>最近测评分数</span>
            <strong><%= latestAssessment == null ? "--" : latestAssessment.getTotalScore() %></strong>
        </article>
        <article class="metric-card">
            <span>测评有效期</span>
            <strong><%= latestAssessment == null ? "--" : latestAssessment.getEffectiveUntil().toString().substring(0, 10) %></strong>
            <p class="section-note"><%= isValid(latestAssessment) ? "有效" : "未测评或已过期" %></p>
        </article>
    </section>

    <section class="task-layout">
        <article class="task-panel">
            <div class="section-title">
                <div>
                    <h2>适当性问卷</h2>
                    <p class="section-note">请按真实情况选择。测评有效期为 1 年，到期后需重新测评。</p>
                </div>
            </div>
            <form method="post" action="<%= request.getContextPath() %>/risk/assessment" class="form grid-form">
                <label>
                    <span>年龄区间</span>
                    <select name="age" required>
                        <option value="">请选择</option>
                        <option value="AGE_OVER_60">60 岁以上</option>
                        <option value="AGE_50_60">50-60 岁</option>
                        <option value="AGE_36_49">36-49 岁</option>
                        <option value="AGE_25_35">25-35 岁</option>
                        <option value="AGE_18_24">18-24 岁</option>
                    </select>
                </label>
                <label>
                    <span>投资经验</span>
                    <select name="experience" required>
                        <option value="">请选择</option>
                        <option value="EXP_NONE">无投资经验</option>
                        <option value="EXP_LT1">1 年以内</option>
                        <option value="EXP_1_3">1-3 年</option>
                        <option value="EXP_3_5">3-5 年</option>
                        <option value="EXP_GT5">5 年以上</option>
                    </select>
                </label>
                <label>
                    <span>可承受本金亏损比例</span>
                    <select name="lossTolerance" required>
                        <option value="">请选择</option>
                        <option value="LOSS_NONE">不能亏损</option>
                        <option value="LOSS_5">5% 以内</option>
                        <option value="LOSS_10">10% 以内</option>
                        <option value="LOSS_20">20% 以内</option>
                        <option value="LOSS_GT20">20% 以上</option>
                    </select>
                </label>
                <label>
                    <span>投资目标</span>
                    <select name="goal" required>
                        <option value="">请选择</option>
                        <option value="GOAL_PRINCIPAL">保本优先</option>
                        <option value="GOAL_STABLE">稳定收益</option>
                        <option value="GOAL_BALANCE">收益与风险平衡</option>
                        <option value="GOAL_GROWTH">追求较高收益</option>
                        <option value="GOAL_AGGRESSIVE">高收益高波动</option>
                    </select>
                </label>
                <label>
                    <span>投资期限偏好</span>
                    <select name="horizon" required>
                        <option value="">请选择</option>
                        <option value="HORIZON_1M">1 个月内</option>
                        <option value="HORIZON_3M">3 个月内</option>
                        <option value="HORIZON_6M">半年内</option>
                        <option value="HORIZON_1Y">1 年内</option>
                        <option value="HORIZON_GT1Y">1 年以上</option>
                    </select>
                </label>
                <label>
                    <span>流动性要求</span>
                    <select name="liquidity" required>
                        <option value="">请选择</option>
                        <option value="LIQUIDITY_ANYTIME">随时可用</option>
                        <option value="LIQUIDITY_WEEK">1 周内可用</option>
                        <option value="LIQUIDITY_MONTH">1 个月内可用</option>
                        <option value="LIQUIDITY_QUARTER">3 个月内可用</option>
                        <option value="LIQUIDITY_LONG">可长期持有</option>
                    </select>
                </label>
                <label class="full-row">
                    <span>金融知识水平</span>
                    <select name="knowledge" required>
                        <option value="">请选择</option>
                        <option value="KNOWLEDGE_LOW">很不了解</option>
                        <option value="KNOWLEDGE_BASIC">了解基础概念</option>
                        <option value="KNOWLEDGE_COMMON">熟悉常见理财产品</option>
                        <option value="KNOWLEDGE_MULTI">熟悉多类资产</option>
                        <option value="KNOWLEDGE_PRO">专业水平</option>
                    </select>
                </label>
                <div class="full-row action-row">
                    <a class="button secondary" href="<%= request.getContextPath() %>/wealth/products">返回理财产品</a>
                    <button class="button primary" type="submit">提交测评</button>
                </div>
            </form>
        </article>

        <aside class="task-side">
            <section class="content-section">
                <h2>等级用途</h2>
                <ul class="task-checklist">
                    <li>申购理财产品时，会比较客户风险等级和产品风险等级。</li>
                    <li>测评过期后，系统会要求重新完成问卷再购买产品。</li>
                    <li>后台管理员可因合规原因调整等级来源，页面会展示来源。</li>
                    <li>风险等级只影响适当性校验，不改变账户余额和交易流水。</li>
                </ul>
            </section>
            <section class="content-section">
                <h2>等级参考</h2>
                <div class="event-feed">
                    <article class="event-card">
                        <div class="event-card-main"><strong>C1-C2</strong><p>偏保守，适合低风险或稳健产品。</p></div>
                        <span class="tag">低</span>
                    </article>
                    <article class="event-card">
                        <div class="event-card-main"><strong>C3</strong><p>平衡型，关注收益和风险匹配。</p></div>
                        <span class="tag">中</span>
                    </article>
                    <article class="event-card">
                        <div class="event-card-main"><strong>C4-C5</strong><p>可承受更高波动，仍需确认产品揭示。</p></div>
                        <span class="tag">高</span>
                    </article>
                </div>
            </section>
        </aside>
    </section>
</main>
</body>
</html>
