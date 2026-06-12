<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRiskGraphCapacityMetric,com.bank.dto.AdminRiskGraphModelComparePage,com.bank.dto.AdminRiskGraphModelGovernanceView,com.bank.dto.AdminRiskGraphModelVersionMetric,com.bank.dto.AdminRiskGraphThresholdPlan,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.sql.Timestamp,java.util.Collections,java.util.List" %>
<%!
    private String rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return new BigDecimal(numerator).multiply(new BigDecimal("100"))
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String decimalText(BigDecimal value, int scale) {
        if (value == null) {
            return "0";
        }
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private String probabilityText(BigDecimal value) {
        if (value == null) {
            return "0.00%";
        }
        return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private AdminRiskGraphModelVersionMetric findMetric(List<AdminRiskGraphModelVersionMetric> metrics,
                                                        String modelVersion) {
        if (metrics == null || modelVersion == null) {
            return null;
        }
        for (AdminRiskGraphModelVersionMetric metric : metrics) {
            if (modelVersion.equals(metric.getModelVersion())) {
                return metric;
            }
        }
        return null;
    }

    private AdminRiskGraphModelGovernanceView findGovernance(List<AdminRiskGraphModelGovernanceView> governance,
                                                             String modelVersion) {
        if (governance == null || modelVersion == null) {
            return null;
        }
        for (AdminRiskGraphModelGovernanceView item : governance) {
            if (modelVersion.equals(item.getModelVersion())) {
                return item;
            }
        }
        return null;
    }

    private String delta(long current, long baseline) {
        long diff = current - baseline;
        if (diff > 0) {
            return "+" + diff;
        }
        return String.valueOf(diff);
    }

    private String roleLabel(String role) {
        if ("OPERATING".equals(role)) return "运营模型";
        if ("CANDIDATE".equals(role)) return "候选模型";
        if ("ARCHIVED".equals(role)) return "已归档";
        return "实验模型";
    }

    private String onlineModeLabel(String mode) {
        if ("SHADOW".equals(mode)) return "影子观测";
        if ("ONLINE_ASSIST".equals(mode)) return "在线辅助";
        return "离线复核";
    }

    private String governanceClass(AdminRiskGraphModelGovernanceView view) {
        if (view != null && view.isOperational()) return "severity-success";
        if (view != null && "CANDIDATE".equals(view.getModelRole())) return "severity-warning";
        if (view != null && "ARCHIVED".equals(view.getModelRole())) return "severity-info";
        return "";
    }

    private String selected(String actual, String expected) {
        return expected != null && expected.equals(actual) ? "selected" : "";
    }

    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "未记录" : timestamp.toString().substring(0, 19);
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminRiskGraphModelComparePage modelPage =
            (AdminRiskGraphModelComparePage) request.getAttribute("modelPage");
    if (modelPage == null) {
        modelPage = new AdminRiskGraphModelComparePage();
    }
    List<String> modelVersions = modelPage.getModelVersions() == null
            ? Collections.<String>emptyList() : modelPage.getModelVersions();
    List<AdminRiskGraphModelVersionMetric> modelMetrics = modelPage.getModelMetrics() == null
            ? Collections.<AdminRiskGraphModelVersionMetric>emptyList() : modelPage.getModelMetrics();
    List<AdminRiskGraphModelGovernanceView> modelGovernance = modelPage.getModelGovernance() == null
            ? Collections.<AdminRiskGraphModelGovernanceView>emptyList() : modelPage.getModelGovernance();
    List<AdminRiskGraphCapacityMetric> capacityMetrics = modelPage.getCapacityMetrics() == null
            ? Collections.<AdminRiskGraphCapacityMetric>emptyList() : modelPage.getCapacityMetrics();
    String baselineModelVersion = modelPage.getBaselineModelVersion();
    String candidateModelVersion = modelPage.getCandidateModelVersion();
    String operationalModelVersion = modelPage.getOperationalModelVersion();
    AdminRiskGraphModelGovernanceView operationalGovernance =
            findGovernance(modelGovernance, operationalModelVersion);
    AdminRiskGraphModelVersionMetric baselineMetric = findMetric(modelMetrics, baselineModelVersion);
    AdminRiskGraphModelVersionMetric candidateMetric = findMetric(modelMetrics, candidateModelVersion);
    AdminRiskGraphThresholdPlan thresholdPlan = modelPage.getThresholdPlan();
    AdminRiskGraphCapacityMetric reviewPlanMetric = thresholdPlan == null ? null : thresholdPlan.getReviewMetric();
    AdminRiskGraphCapacityMetric blockPlanMetric = thresholdPlan == null ? null : thresholdPlan.getBlockMetric();
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>GNN模型对比 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">RiskBrain Model Strategy</p>
        <h1>GNN模型对比与阈值策略</h1>
        <p class="muted">把不同版本的回灌评分放在同一张业务视图里，观察拦截量、复核量、真实标签命中和审核容量，避免只看单次 AUC 指标就上线模型。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>
    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>

    <section class="content-section admin-decision-strip">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Release Review</p>
                <h2>上线前先回答三个业务问题</h2>
                <p class="muted">模型治理页的核心不是“哪个指标更漂亮”，而是确认新模型会不会带来过多打扰、漏检或人工容量压力。</p>
            </div>
            <span class="tag severity-info">离线批量评分</span>
        </div>
        <div class="admin-decision-grid">
            <article class="admin-decision-card">
                <span>候选模型</span>
                <strong><%= HtmlUtil.escape(candidateModelVersion) %></strong>
                <p>候选版本先用于评分看板和复核队列对比，不直接改变交易拦截规则。</p>
            </article>
            <article class="admin-decision-card warning">
                <span>人工容量</span>
                <strong><%= candidateMetric == null ? 0 : candidateMetric.getFlaggedCount() %> 笔</strong>
                <p>这是 REVIEW 与 BLOCK 合计进入人工视野的规模，必须和风控团队每日处理能力匹配。</p>
            </article>
            <article class="admin-decision-card danger">
                <span>强干预队列</span>
                <strong><%= candidateMetric == null ? 0 : candidateMetric.getBlockCount() %> 笔</strong>
                <p>BLOCK 队列命中率 <%= candidateMetric == null ? "0.00%" : rate(candidateMetric.getBlockFraudCount(), candidateMetric.getBlockCount()) %>，仍需结合规则和复核结论。</p>
            </article>
            <article class="admin-decision-card">
                <span>上线建议</span>
                <strong>先影子验证</strong>
                <p>当前系统定位是辅助运营：先沉淀人工反馈，再把稳定版本切为运营模型。</p>
            </article>
        </div>
    </section>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/risk/graph-models" class="filter-form">
            <label>
                <span>基准版本</span>
                <select name="baselineModelVersion">
                    <% for (String version : modelVersions) { %>
                        <option value="<%= HtmlUtil.escape(version) %>" <%= version.equals(baselineModelVersion) ? "selected" : "" %>><%= HtmlUtil.escape(version) %></option>
                    <% } %>
                </select>
            </label>
            <label>
                <span>候选版本</span>
                <select name="candidateModelVersion">
                    <% for (String version : modelVersions) { %>
                        <option value="<%= HtmlUtil.escape(version) %>" <%= version.equals(candidateModelVersion) ? "selected" : "" %>><%= HtmlUtil.escape(version) %></option>
                    <% } %>
                </select>
            </label>
            <label>
                <span>复核目标笔数</span>
                <input type="number" min="1" max="200000" name="reviewCapacity"
                       value="<%= modelPage.getReviewCapacity() > 0 ? modelPage.getReviewCapacity() : 5000 %>">
            </label>
            <label>
                <span>强拦截目标笔数</span>
                <input type="number" min="1" max="200000" name="blockCapacity"
                       value="<%= modelPage.getBlockCapacity() > 0 ? modelPage.getBlockCapacity() : 100 %>">
            </label>
            <button class="button primary" type="submit">对比模型</button>
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/risk/graph-scores?modelVersion=<%= HtmlUtil.escape(candidateModelVersion) %>">查看候选评分</a>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Model Operations</p>
                <h2>模型运营状态</h2>
                <p class="muted">把训练结果转成系统可使用的运营资产：只有“运营模型”会默认进入评分看板和复核队列，候选与实验模型只用于对比和回测。</p>
            </div>
            <span class="tag severity-success">当前运营 <%= HtmlUtil.escape(operationalModelVersion) %></span>
        </div>
        <div class="admin-insight-grid">
            <div class="admin-insight-card decision-layer-card">
                <span>运营模型</span>
                <strong><%= HtmlUtil.escape(operationalModelVersion) %></strong>
                <p><%= operationalGovernance == null || operationalGovernance.getGovernanceNote() == null
                        ? "该模型用于后台默认评分、图谱证据查看和复核样本同步；在线交易拦截仍由规则引擎负责。"
                        : HtmlUtil.escape(operationalGovernance.getGovernanceNote()) %></p>
            </div>
            <div class="admin-insight-card">
                <span>接入方式</span>
                <strong><%= onlineModeLabel(operationalGovernance == null ? null : operationalGovernance.getOnlineMode()) %></strong>
                <p>当前阶段推荐保持“离线复核”：模型产生线索和优先级，不直接扣款、不直接冻结、不直接替代人工结论。</p>
            </div>
            <div class="admin-insight-card">
                <span>最后调整</span>
                <strong><%= timeText(operationalGovernance == null ? null : operationalGovernance.getPromotedAt()) %></strong>
                <p>每一次运营模型切换都会写入后台审计日志，后续可追溯是谁把哪个版本切到运营状态。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>模型版本</th>
                    <th>角色</th>
                    <th>生命周期</th>
                    <th>接入方式</th>
                    <th>运营默认</th>
                    <th>治理说明</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (modelGovernance.isEmpty()) { %>
                    <tr><td colspan="7" class="empty">还没有可治理的模型版本。先导入图模型评分文件后，这里会自动生成实验模型记录。</td></tr>
                <% } else {
                    int governanceIndex = 0;
                    for (AdminRiskGraphModelGovernanceView item : modelGovernance) {
                        String formId = "modelGovernanceForm" + governanceIndex++;
                %>
                    <tr>
                        <td>
                            <strong><%= HtmlUtil.escape(item.getModelVersion()) %></strong>
                            <span class="tag <%= governanceClass(item) %>"><%= roleLabel(item.getModelRole()) %></span>
                        </td>
                        <td>
                            <select class="compact-input" form="<%= formId %>" name="modelRole">
                                <option value="EXPERIMENT" <%= selected(item.getModelRole(), "EXPERIMENT") %>>实验模型</option>
                                <option value="CANDIDATE" <%= selected(item.getModelRole(), "CANDIDATE") %>>候选模型</option>
                                <option value="OPERATING" <%= selected(item.getModelRole(), "OPERATING") %>>运营模型</option>
                                <option value="ARCHIVED" <%= selected(item.getModelRole(), "ARCHIVED") %>>已归档</option>
                            </select>
                        </td>
                        <td>
                            <select class="compact-input" form="<%= formId %>" name="lifecycleStatus">
                                <option value="EVALUATING" <%= selected(item.getLifecycleStatus(), "EVALUATING") %>>评估中</option>
                                <option value="APPROVED" <%= selected(item.getLifecycleStatus(), "APPROVED") %>>已批准</option>
                                <option value="SHADOW" <%= selected(item.getLifecycleStatus(), "SHADOW") %>>影子验证</option>
                                <option value="RETIRED" <%= selected(item.getLifecycleStatus(), "RETIRED") %>>已退役</option>
                            </select>
                        </td>
                        <td>
                            <select class="compact-input" form="<%= formId %>" name="onlineMode">
                                <option value="OFFLINE_REVIEW" <%= selected(item.getOnlineMode(), "OFFLINE_REVIEW") %>>离线复核</option>
                                <option value="SHADOW" <%= selected(item.getOnlineMode(), "SHADOW") %>>影子观测</option>
                                <option value="ONLINE_ASSIST" <%= selected(item.getOnlineMode(), "ONLINE_ASSIST") %>>在线辅助</option>
                            </select>
                        </td>
                        <td>
                            <label class="inline-check">
                                <input form="<%= formId %>" type="checkbox" name="operational" value="1" <%= item.isOperational() ? "checked" : "" %>>
                                默认使用
                            </label>
                        </td>
                        <td>
                            <input class="compact-input wide-input" form="<%= formId %>" name="governanceNote"
                                   value="<%= HtmlUtil.escape(item.getGovernanceNote()) %>"
                                   placeholder="说明为什么保留、候选或设为运营模型">
                        </td>
                        <td>
                            <% if (adminUser != null && adminUser.hasPermission("RISK_GRAPH_CASE_HANDLE")) { %>
                                <form id="<%= formId %>" method="post" action="<%= request.getContextPath() %>/admin/risk/graph-models">
                                    <input type="hidden" name="modelVersion" value="<%= HtmlUtil.escape(item.getModelVersion()) %>">
                                </form>
                                <button class="button secondary compact" form="<%= formId %>" type="submit">保存</button>
                            <% } else { %>
                                <span class="tag">只读</span>
                            <% } %>
                        </td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="metric-grid">
        <article class="metric-card">
            <span>候选模型</span>
            <strong><%= HtmlUtil.escape(candidateModelVersion) %></strong>
            <small>当前准备进入业务评估的模型版本</small>
        </article>
        <article class="metric-card">
            <span>基准模型</span>
            <strong><%= HtmlUtil.escape(baselineModelVersion) %></strong>
            <small>用于横向比较的历史版本或当前线上版本</small>
        </article>
        <article class="metric-card warning">
            <span>候选复核容量</span>
            <strong><%= candidateMetric == null ? 0 : candidateMetric.getFlaggedCount() %></strong>
            <small>REVIEW + BLOCK，命中率 <%= candidateMetric == null ? "0.00%" : rate(candidateMetric.getFlaggedFraudCount(), candidateMetric.getFlaggedCount()) %></small>
        </article>
        <article class="metric-card danger">
            <span>候选强拦截</span>
            <strong><%= candidateMetric == null ? 0 : candidateMetric.getBlockCount() %></strong>
            <small>BLOCK 标签命中率 <%= candidateMetric == null ? "0.00%" : rate(candidateMetric.getBlockFraudCount(), candidateMetric.getBlockCount()) %></small>
        </article>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Threshold Lab</p>
                <h2>候选模型阈值实验台</h2>
                <p class="muted">输入业务团队一天能处理的 REVIEW 与 BLOCK 容量，系统会按候选模型风险分从高到低截断，反推出最低入队分，并估算命中率、召回率和正常交易打扰量。</p>
            </div>
            <span class="tag severity-info">候选版本 <%= HtmlUtil.escape(candidateModelVersion) %></span>
        </div>
        <% if (thresholdPlan == null || reviewPlanMetric == null || blockPlanMetric == null) { %>
            <div class="human-empty">
                <strong>还不能生成阈值实验结果</strong>
                <span>请先完成候选模型评分导入，或选择一个已有评分的模型版本。</span>
            </div>
        <% } else {
            String reviewQueueUrl = request.getContextPath() + "/admin/risk/graph-scores?modelVersion="
                    + HtmlUtil.escape(candidateModelVersion) + "&minScore=" + reviewPlanMetric.getThresholdScore();
            String blockQueueUrl = request.getContextPath() + "/admin/risk/graph-scores?modelVersion="
                    + HtmlUtil.escape(candidateModelVersion) + "&minScore=" + blockPlanMetric.getThresholdScore();
        %>
            <section class="metric-grid">
                <article class="metric-card warning">
                    <span>REVIEW 推荐入队分</span>
                    <strong><%= reviewPlanMetric.getThresholdScore() %></strong>
                    <small>Top <%= thresholdPlan.getReviewCapacity() %>，Precision <%= rate(reviewPlanMetric.getTruePositiveCount(), reviewPlanMetric.getSelectedCount()) %></small>
                </article>
                <article class="metric-card danger">
                    <span>BLOCK 推荐入队分</span>
                    <strong><%= blockPlanMetric.getThresholdScore() %></strong>
                    <small>Top <%= thresholdPlan.getBlockCapacity() %>，Precision <%= rate(blockPlanMetric.getTruePositiveCount(), blockPlanMetric.getSelectedCount()) %></small>
                </article>
                <article class="metric-card">
                    <span>人工复核净队列</span>
                    <strong><%= thresholdPlan.getReviewOnlySelectedCount() %></strong>
                    <small>排除 BLOCK 后仍需人工判断的交易量</small>
                </article>
                <article class="metric-card">
                    <span>预计总召回</span>
                    <strong><%= rate(reviewPlanMetric.getTruePositiveCount(), reviewPlanMetric.getTotalPositiveCount()) %></strong>
                    <small>在目标 REVIEW 容量内找回真实正样本的比例</small>
                </article>
            </section>
            <div class="table-wrap">
                <table class="wide-table">
                    <thead>
                    <tr>
                        <th>策略层</th>
                        <th>容量</th>
                        <th>最低分</th>
                        <th>命中正样本</th>
                        <th>正常样本打扰</th>
                        <th>Precision</th>
                        <th>Recall</th>
                        <th>业务解释</th>
                        <th>进入队列</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td><span class="tag severity-danger">BLOCK</span></td>
                        <td><%= blockPlanMetric.getSelectedCount() %></td>
                        <td><%= blockPlanMetric.getThresholdScore() %></td>
                        <td><%= blockPlanMetric.getTruePositiveCount() %></td>
                        <td><%= blockPlanMetric.getFalsePositiveCount() %></td>
                        <td><%= rate(blockPlanMetric.getTruePositiveCount(), blockPlanMetric.getSelectedCount()) %></td>
                        <td><%= rate(blockPlanMetric.getTruePositiveCount(), blockPlanMetric.getTotalPositiveCount()) %></td>
                        <td><small class="table-note">只建议作为最强干预队列，仍需要结合限额规则、标签冲突和人工复核确认。</small></td>
                        <td><a class="button secondary compact" href="<%= blockQueueUrl %>">查看 BLOCK 候选</a></td>
                    </tr>
                    <tr>
                        <td><span class="tag severity-warning">REVIEW</span></td>
                        <td><%= reviewPlanMetric.getSelectedCount() %></td>
                        <td><%= reviewPlanMetric.getThresholdScore() %></td>
                        <td><%= reviewPlanMetric.getTruePositiveCount() %></td>
                        <td><%= reviewPlanMetric.getFalsePositiveCount() %></td>
                        <td><%= rate(reviewPlanMetric.getTruePositiveCount(), reviewPlanMetric.getSelectedCount()) %></td>
                        <td><%= rate(reviewPlanMetric.getTruePositiveCount(), reviewPlanMetric.getTotalPositiveCount()) %></td>
                        <td><small class="table-note">这是完整人工复核队列，包含 BLOCK 高危子集；适合用于日常运营容量排班。</small></td>
                        <td><a class="button secondary compact" href="<%= reviewQueueUrl %>">查看 REVIEW 候选</a></td>
                    </tr>
                    <tr>
                        <td><span class="tag severity-info">REVIEW ONLY</span></td>
                        <td><%= thresholdPlan.getReviewOnlySelectedCount() %></td>
                        <td><%= reviewPlanMetric.getThresholdScore() %> - <%= blockPlanMetric.getThresholdScore() %></td>
                        <td><%= thresholdPlan.getReviewOnlyTruePositiveCount() %></td>
                        <td><%= thresholdPlan.getReviewOnlyFalsePositiveCount() %></td>
                        <td><%= rate(thresholdPlan.getReviewOnlyTruePositiveCount(), thresholdPlan.getReviewOnlySelectedCount()) %></td>
                        <td><%= rate(thresholdPlan.getReviewOnlyTruePositiveCount(), reviewPlanMetric.getTotalPositiveCount()) %></td>
                        <td><small class="table-note">这部分不建议自动拦截，应进入人工队列结合图谱证据、交易背景和客户历史解释。</small></td>
                        <td><span class="tag">人工判断</span></td>
                    </tr>
                    </tbody>
                </table>
            </div>
        <% } %>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Version Comparison</p>
                <h2>模型版本业务指标</h2>
                <p class="muted">这里展示的是评分回灌后的业务指标，不等同于训练集 AUC/AP。它回答的是：这个版本会让多少交易进入人工队列，能找回多少真实正样本，会造成多少正常交易被打扰。</p>
            </div>
            <% if (baselineMetric != null && candidateMetric != null) { %>
                <span class="tag">复核量变化 <%= HtmlUtil.escape(delta(candidateMetric.getFlaggedCount(), baselineMetric.getFlaggedCount())) %></span>
            <% } %>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>模型版本</th>
                    <th>特征版本</th>
                    <th>总评分</th>
                    <th>真实正样本</th>
                    <th>PASS</th>
                    <th>REVIEW</th>
                    <th>BLOCK</th>
                    <th>复核/拦截命中率</th>
                    <th>复核/拦截召回率</th>
                    <th>BLOCK命中率</th>
                    <th>平均分</th>
                    <th>阈值</th>
                </tr>
                </thead>
                <tbody>
                <% if (modelMetrics.isEmpty()) { %>
                    <tr><td colspan="12" class="empty">还没有可对比的 GNN 模型评分。完成模型评分导入后，这里会自动展示版本指标。</td></tr>
                <% } else {
                    for (AdminRiskGraphModelVersionMetric metric : modelMetrics) {
                %>
                    <tr>
                        <td>
                            <strong><%= HtmlUtil.escape(metric.getModelVersion()) %></strong>
                            <% if (metric.getModelVersion().equals(candidateModelVersion)) { %>
                                <span class="tag severity-info">候选</span>
                            <% } else if (metric.getModelVersion().equals(baselineModelVersion)) { %>
                                <span class="tag">基准</span>
                            <% } %>
                        </td>
                        <td><%= HtmlUtil.escape(metric.getFeatureVersion()) %></td>
                        <td><%= metric.getTotalScores() %></td>
                        <td><%= metric.getPositiveLabels() %></td>
                        <td><%= metric.getPassCount() %> <small class="table-note">漏过正样本 <%= metric.getPassFraudCount() %></small></td>
                        <td><%= metric.getReviewCount() %> <small class="table-note">命中正样本 <%= metric.getReviewFraudCount() %></small></td>
                        <td><%= metric.getBlockCount() %> <small class="table-note">命中正样本 <%= metric.getBlockFraudCount() %></small></td>
                        <td><%= rate(metric.getFlaggedFraudCount(), metric.getFlaggedCount()) %></td>
                        <td><%= rate(metric.getFlaggedFraudCount(), metric.getPositiveLabels()) %></td>
                        <td><%= rate(metric.getBlockFraudCount(), metric.getBlockCount()) %></td>
                        <td><%= decimalText(metric.getAverageScore(), 0) %></td>
                        <td>
                            <small class="table-note">REVIEW <%= probabilityText(metric.getReviewThreshold()) %></small>
                            <small class="table-note">BLOCK <%= probabilityText(metric.getBlockThreshold()) %></small>
                        </td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Review Capacity</p>
                <h2>Top-K 审核容量分析</h2>
                <p class="muted">风控不是分数越高越好，还要看团队每天能处理多少笔。Top-K 分析用于回答“如果今天只看前 500/1000/5000 笔，命中率和召回率分别是多少”。</p>
            </div>
            <span class="tag severity-warning">按风险分从高到低截断</span>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>模型版本</th>
                    <th>审核容量</th>
                    <th>实际纳入</th>
                    <th>最低入队分</th>
                    <th>命中正样本</th>
                    <th>正常样本打扰</th>
                    <th>Precision</th>
                    <th>Recall</th>
                    <th>查看队列</th>
                </tr>
                </thead>
                <tbody>
                <% if (capacityMetrics.isEmpty()) { %>
                    <tr><td colspan="9" class="empty">暂无容量曲线。完成 GNN 评分导入后，会按 Top-K 自动计算候选队列质量。</td></tr>
                <% } else {
                    for (AdminRiskGraphCapacityMetric metric : capacityMetrics) {
                        String capacityUrl = request.getContextPath() + "/admin/risk/graph-scores?modelVersion="
                                + HtmlUtil.escape(metric.getModelVersion()) + "&minScore=" + metric.getThresholdScore();
                %>
                    <tr>
                        <td>
                            <strong><%= HtmlUtil.escape(metric.getModelVersion()) %></strong>
                            <% if (metric.getModelVersion().equals(candidateModelVersion)) { %>
                                <span class="tag severity-info">候选</span>
                            <% } else if (metric.getModelVersion().equals(baselineModelVersion)) { %>
                                <span class="tag">基准</span>
                            <% } %>
                        </td>
                        <td>Top <%= metric.getCapacity() %></td>
                        <td><%= metric.getSelectedCount() %></td>
                        <td><%= metric.getThresholdScore() %></td>
                        <td><%= metric.getTruePositiveCount() %></td>
                        <td><%= metric.getFalsePositiveCount() %></td>
                        <td><%= rate(metric.getTruePositiveCount(), metric.getSelectedCount()) %></td>
                        <td><%= rate(metric.getTruePositiveCount(), metric.getTotalPositiveCount()) %></td>
                        <td><a class="button secondary compact" href="<%= capacityUrl %>">进入评分队列</a></td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Model Governance</p>
                <h2>上线判断建议</h2>
            </div>
            <span class="tag severity-info">当前为离线批量评分模式</span>
        </div>
        <div class="admin-insight-grid">
            <div class="admin-insight-card">
                <span>不能只看 BLOCK</span>
                <strong>看整体队列</strong>
                <p>BLOCK 是强动作，REVIEW 是运营容量。候选模型如果大幅减少 BLOCK，需要确认正样本是否被转移到了 REVIEW，而不是直接漏过。</p>
            </div>
            <div class="admin-insight-card">
                <span>不能只看准确率</span>
                <strong>看误伤成本</strong>
                <p>正常交易进入复核会增加客户打扰和人工成本。Top-K 容量表可以帮助确定每天更合理的人工处理阈值。</p>
            </div>
            <div class="admin-insight-card">
                <span>不能忽视漏检</span>
                <strong>看召回</strong>
                <p>PASS 中的真实正样本代表模型没有拦住的风险。后续应把人工反馈补回训练集，继续优化 GNN 特征和损失函数。</p>
            </div>
            <div class="admin-insight-card decision-layer-card">
                <span>推荐下一步</span>
                <strong>阈值实验</strong>
                <p>先用候选 V2 替代强拦截逻辑，保留人工复核队列；再用复核结果回灌，形成模型、规则、人工反馈的闭环。</p>
            </div>
        </div>
    </section>
</main>
</body>
</html>
