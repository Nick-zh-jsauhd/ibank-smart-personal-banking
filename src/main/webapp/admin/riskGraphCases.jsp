<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRiskGraphReviewCasePage,com.bank.dto.AdminRiskGraphReviewCaseView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.sql.Timestamp,java.util.Collections,java.util.List" %>
<%!
    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String probabilityText(BigDecimal value) {
        if (value == null) {
            return "0.00%";
        }
        return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String compactNode(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 26) {
            return value;
        }
        return value.substring(0, 12) + "..." + value.substring(value.length() - 10);
    }

    private String caseTypeLabel(String type) {
        if ("LABEL_CONFLICT".equals(type)) return "标签冲突";
        if ("TRUE_POSITIVE_BLOCK".equals(type)) return "重点调查";
        if ("STRONG_REVIEW".equals(type)) return "强复核";
        if ("MODEL_REVIEW".equals(type)) return "模型复核";
        return type == null ? "" : type;
    }

    private String statusLabel(String status) {
        if ("OPEN".equals(status)) return "待复核";
        if ("REVIEWED".equals(status)) return "已复核";
        if ("IGNORED".equals(status)) return "已忽略";
        return status == null ? "" : status;
    }

    private String reviewResultLabel(String result) {
        if ("CONFIRMED_RISK".equals(result)) return "确认风险";
        if ("FALSE_POSITIVE".equals(result)) return "误报样本";
        if ("NEED_MORE_DATA".equals(result)) return "需补证据";
        if ("IGNORE".equals(result)) return "忽略";
        return result == null ? "" : result;
    }

    private String severityClass(String type) {
        if ("LABEL_CONFLICT".equals(type)) return "severity-conflict";
        if ("TRUE_POSITIVE_BLOCK".equals(type)) return "severity-danger";
        if ("STRONG_REVIEW".equals(type)) return "severity-high";
        return "severity-warning";
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminRiskGraphReviewCasePage casePage = (AdminRiskGraphReviewCasePage) request.getAttribute("casePage");
    if (casePage == null) {
        casePage = new AdminRiskGraphReviewCasePage();
    }
    List<AdminRiskGraphReviewCaseView> cases = casePage.getCases() == null
            ? Collections.<AdminRiskGraphReviewCaseView>emptyList() : casePage.getCases();
    List<String> modelVersions = casePage.getModelVersions() == null
            ? Collections.<String>emptyList() : casePage.getModelVersions();
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    String selectedModelVersion = (String) request.getAttribute("selectedModelVersion");
    if ((selectedModelVersion == null || selectedModelVersion.length() == 0)
            && casePage.getSelectedModelVersion() != null) {
        selectedModelVersion = casePage.getSelectedModelVersion();
    }
    if ((selectedModelVersion == null || selectedModelVersion.length() == 0) && !modelVersions.isEmpty()) {
        selectedModelVersion = modelVersions.get(0);
    }
    String operationalModelVersion = casePage.getOperationalModelVersion();
    String selectedCaseStatus = (String) request.getAttribute("selectedCaseStatus");
    String selectedCaseType = (String) request.getAttribute("selectedCaseType");
    String selectedReviewResult = (String) request.getAttribute("selectedReviewResult");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>GNN复核队列 - iBank Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout layout-wide">
    <section class="page-heading">
        <p class="eyebrow">RiskBrain Feedback Loop</p>
        <h1>GNN复核队列</h1>
        <p class="muted">把模型高风险、标签冲突和重点调查样本从评分看板中沉淀出来，由风控人员给出人工结论，后续用于阈值校准和再训练。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="metric-grid">
        <article class="metric-card warning">
            <span>待复核</span>
            <strong><%= casePage.getOpenCount() %></strong>
            <small>需要人工确认，不直接等同于洗钱结论</small>
        </article>
        <article class="metric-card danger">
            <span>标签冲突</span>
            <strong><%= casePage.getConflictCount() %></strong>
            <small>源标签正常但模型强烈怀疑，需要重点解释</small>
        </article>
        <article class="metric-card">
            <span>已复核</span>
            <strong><%= casePage.getReviewedCount() %></strong>
            <small>可作为后续模型反馈样本</small>
        </article>
        <article class="metric-card">
            <span>已忽略</span>
            <strong><%= casePage.getIgnoredCount() %></strong>
            <small>低业务价值或不进入本轮训练闭环</small>
        </article>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Feedback Operations</p>
                <h2>复核闭环操作</h2>
                <p class="section-note">先从运营模型评分里同步候选样本，再由风控人员复核；已有样本只更新模型分数和原因，不覆盖人工结论。</p>
            </div>
            <div class="section-actions">
                <% if (adminUser != null && adminUser.hasPermission("RISK_GRAPH_CASE_HANDLE")) { %>
                    <form method="post" action="<%= request.getContextPath() %>/admin/risk/graph-cases" class="inline-form">
                        <input type="hidden" name="modelVersion" value="<%= HtmlUtil.escape(selectedModelVersion) %>">
                        <button class="button primary" type="submit">同步候选样本</button>
                    </form>
                <% } %>
                <form method="post" action="<%= request.getContextPath() %>/admin/risk/graph-feedback-export" class="inline-form">
                    <input type="hidden" name="modelVersion" value="<%= HtmlUtil.escape(selectedModelVersion) %>">
                    <select class="compact-input" name="reviewResult" aria-label="导出人工结论">
                        <option value="">全部反馈</option>
                        <option value="CONFIRMED_RISK">确认风险</option>
                        <option value="FALSE_POSITIVE">误报样本</option>
                        <option value="NEED_MORE_DATA">需补证据</option>
                        <option value="IGNORE">忽略样本</option>
                    </select>
                    <button class="button secondary" type="submit">导出训练反馈</button>
                </form>
            </div>
        </div>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Feedback Quality</p>
                <h2>训练反馈质量</h2>
                <p class="section-note">只有“确认风险”和“误报样本”会变成明确训练标签；“需补证据”和“忽略”会进入治理记录，但不会强行喂给模型。</p>
            </div>
            <span class="tag severity-info">可训练反馈 <%= casePage.getTrainableFeedbackCount() %> 条</span>
        </div>
        <div class="admin-insight-grid">
            <div class="admin-insight-card">
                <span>确认风险</span>
                <strong><%= casePage.getConfirmedRiskCount() %></strong>
                <p>导出时写成 human_label=1，用于补充模型没有充分识别的高价值正样本。</p>
            </div>
            <div class="admin-insight-card">
                <span>误报样本</span>
                <strong><%= casePage.getFalsePositiveCount() %></strong>
                <p>导出时写成 human_label=0，用于压低模型对正常交易的过度怀疑。</p>
            </div>
            <div class="admin-insight-card">
                <span>需补证据</span>
                <strong><%= casePage.getNeedMoreDataCount() %></strong>
                <p>这类样本说明现有图谱证据不足，先补材料，不进入硬标签训练。</p>
            </div>
            <div class="admin-insight-card decision-layer-card">
                <span>再训练建议</span>
                <strong><%= casePage.getTrainableFeedbackCount() >= 50 ? "可以小批量回流" : "继续积累反馈" %></strong>
                <p><%= casePage.getTrainableFeedbackCount() >= 50
                        ? "当前已有一定人工标签，可以先做一次反馈增强训练，并与运营模型做影子对比。"
                        : "反馈样本量还偏少，建议先让风控人员继续处理高分冲突样本，再启动下一轮训练。" %></p>
            </div>
        </div>
    </section>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/risk/graph-cases" class="filter-form">
            <label>
                <span>模型版本</span>
                <select name="modelVersion">
                    <% for (String version : modelVersions) { %>
                        <option value="<%= HtmlUtil.escape(version) %>" <%= version.equals(selectedModelVersion) ? "selected" : "" %>><%= HtmlUtil.escape(version) %><%= version.equals(operationalModelVersion) ? "（运营）" : "" %></option>
                    <% } %>
                </select>
            </label>
            <label>
                <span>状态</span>
                <select name="caseStatus">
                    <option value="OPEN" <%= "OPEN".equals(selectedCaseStatus) ? "selected" : "" %>>待复核</option>
                    <option value="REVIEWED" <%= "REVIEWED".equals(selectedCaseStatus) ? "selected" : "" %>>已复核</option>
                    <option value="IGNORED" <%= "IGNORED".equals(selectedCaseStatus) ? "selected" : "" %>>已忽略</option>
                    <option value="ALL" <%= "ALL".equals(selectedCaseStatus) ? "selected" : "" %>>全部</option>
                </select>
            </label>
            <label>
                <span>样本类型</span>
                <select name="caseType">
                    <option value="ALL">全部</option>
                    <option value="LABEL_CONFLICT" <%= "LABEL_CONFLICT".equals(selectedCaseType) ? "selected" : "" %>>标签冲突</option>
                    <option value="TRUE_POSITIVE_BLOCK" <%= "TRUE_POSITIVE_BLOCK".equals(selectedCaseType) ? "selected" : "" %>>重点调查</option>
                    <option value="STRONG_REVIEW" <%= "STRONG_REVIEW".equals(selectedCaseType) ? "selected" : "" %>>强复核</option>
                    <option value="MODEL_REVIEW" <%= "MODEL_REVIEW".equals(selectedCaseType) ? "selected" : "" %>>模型复核</option>
                </select>
            </label>
            <label>
                <span>人工结论</span>
                <select name="reviewResult">
                    <option value="ALL">全部</option>
                    <option value="CONFIRMED_RISK" <%= "CONFIRMED_RISK".equals(selectedReviewResult) ? "selected" : "" %>>确认风险</option>
                    <option value="FALSE_POSITIVE" <%= "FALSE_POSITIVE".equals(selectedReviewResult) ? "selected" : "" %>>误报样本</option>
                    <option value="NEED_MORE_DATA" <%= "NEED_MORE_DATA".equals(selectedReviewResult) ? "selected" : "" %>>需补证据</option>
                    <option value="IGNORE" <%= "IGNORE".equals(selectedReviewResult) ? "selected" : "" %>>忽略</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">Review Queue</p>
                <h2>复核样本列表</h2>
                <p class="section-note">优先处理“标签冲突”：它代表模型和原始标签不一致，不能直接判定，只能进入人工解释与反馈闭环。</p>
            </div>
            <span class="tag">最多展示 200 条</span>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>类型</th>
                    <th>状态</th>
                    <th>优先级</th>
                    <th>模型分</th>
                    <th>置信度</th>
                    <th>模型决策</th>
                    <th>源标签</th>
                    <th>金额</th>
                    <th>时间</th>
                    <th>来源节点</th>
                    <th>目标节点</th>
                    <th>人工结论</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (cases.isEmpty()) { %>
                    <tr><td colspan="13" class="empty">当前没有复核样本。先点击“同步候选样本”，系统会从GNN评分中提取需要人工判断的高优先级样本。</td></tr>
                <% } else {
                    for (AdminRiskGraphReviewCaseView item : cases) {
                %>
                    <tr>
                        <td><span class="tag <%= severityClass(item.getCaseType()) %>"><%= HtmlUtil.escape(caseTypeLabel(item.getCaseType())) %></span></td>
                        <td><%= HtmlUtil.escape(statusLabel(item.getCaseStatus())) %></td>
                        <td><strong><%= item.getPriority() %></strong></td>
                        <td><strong><%= item.getRiskScore() %></strong></td>
                        <td><%= probabilityText(item.getRiskProbability()) %></td>
                        <td><%= HtmlUtil.escape(item.getModelDecision()) %></td>
                        <td><%= item.isLabelFraud() ? "洗钱" : "正常" %></td>
                        <td><%= HtmlUtil.escape(item.getCurrency()) %> <%= moneyText(item.getAmount()) %></td>
                        <td><%= HtmlUtil.escape(timeText(item.getEventTime())) %></td>
                        <td title="<%= HtmlUtil.escape(item.getFromExternalId()) %>"><%= HtmlUtil.escape(compactNode(item.getFromExternalId())) %></td>
                        <td title="<%= HtmlUtil.escape(item.getToExternalId()) %>"><%= HtmlUtil.escape(compactNode(item.getToExternalId())) %></td>
                        <td><%= HtmlUtil.escape(reviewResultLabel(item.getReviewResult())) %></td>
                        <td>
                            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/risk/graph-case/detail?caseId=<%= item.getCaseId() %>">复核</a>
                        </td>
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
