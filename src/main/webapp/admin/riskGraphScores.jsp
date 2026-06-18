<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRiskGraphScorePage,com.bank.dto.AdminRiskGraphScoreSummary,com.bank.dto.AdminRiskGraphScoreView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.Collections,java.util.List" %>
<%!
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

    private String rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return new BigDecimal(numerator).multiply(new BigDecimal("100"))
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String timeText(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private String compactNode(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 28) {
            return value;
        }
        return value.substring(0, 14) + "..." + value.substring(value.length() - 10);
    }

    private String decisionClass(String decision) {
        if ("BLOCK".equals(decision)) return "severity-danger";
        if ("REVIEW".equals(decision)) return "severity-warning";
        return "severity-success";
    }

    private String decisionName(String decision) {
        if ("BLOCK".equals(decision)) return "建议阻断";
        if ("REVIEW".equals(decision)) return "人工复核";
        if ("PASS".equals(decision)) return "正常放行";
        return decision == null ? "未评分" : decision;
    }

    private String severityClass(String severity) {
        return severity == null || severity.length() == 0 ? "severity-info" : severity;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminRiskGraphScorePage scorePage = (AdminRiskGraphScorePage) request.getAttribute("scorePage");
    if (scorePage == null) {
        scorePage = new AdminRiskGraphScorePage();
    }
    AdminRiskGraphScoreSummary summary = scorePage.getSummary();
    if (summary == null) {
        summary = new AdminRiskGraphScoreSummary();
    }
    List<AdminRiskGraphScoreView> scores = scorePage.getScores() == null
            ? Collections.<AdminRiskGraphScoreView>emptyList() : scorePage.getScores();
    List<String> modelVersions = scorePage.getModelVersions() == null
            ? Collections.<String>emptyList() : scorePage.getModelVersions();
    List<String> edgeTypes = scorePage.getEdgeTypes() == null
            ? Collections.<String>emptyList() : scorePage.getEdgeTypes();
    String error = (String) request.getAttribute("error");
    String selectedModelVersion = (String) request.getAttribute("selectedModelVersion");
    if ((selectedModelVersion == null || selectedModelVersion.length() == 0) && summary.getModelVersion() != null) {
        selectedModelVersion = summary.getModelVersion();
    }
    String operationalModelVersion = scorePage.getOperationalModelVersion();
    String selectedDecision = (String) request.getAttribute("selectedDecision");
    String selectedEdgeType = (String) request.getAttribute("selectedEdgeType");
    String selectedMinScore = (String) request.getAttribute("selectedMinScore");
    String selectedLabelFraud = (String) request.getAttribute("selectedLabelFraud");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>GNN风险评分 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page lab-page">
<%@ include file="/WEB-INF/jsp/adminLabTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">图谱评分模型</p>
        <h1>GNN风险评分看板</h1>
        <p class="muted">查看图神经网络对外部交易图边的风险排序结果，优先核查建议阻断和人工复核的高分边。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section admin-decision-strip">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">运营决策层</p>
                <h2>先看业务结论，再看模型细节</h2>
                <p class="muted">这张看板不是让管理员直接相信分数，而是把模型线索转成可执行的复核顺序。</p>
            </div>
            <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/risk/graph-cases">进入人工复核</a>
        </div>
        <div class="admin-decision-grid">
            <article class="admin-decision-card danger">
                <span>第一优先级</span>
                <strong>建议阻断高分边</strong>
                <p>优先查看是否存在大额、集中、拆分或中转链路；生产环境不能只凭模型分直接冻结。</p>
            </article>
            <article class="admin-decision-card warning">
                <span>第二优先级</span>
                <strong>标签冲突样本</strong>
                <p>源标签正常但模型强烈怀疑时，进入“标签冲突复核”，由人工解释是否为误报或潜在风险。</p>
            </article>
            <article class="admin-decision-card">
                <span>第三优先级</span>
                <strong>局部图谱证据</strong>
                <p>点击“查看图谱”后再判断中心交易的一跳邻域，不把原始 JSON 当作一线处理入口。</p>
            </article>
        </div>
    </section>

    <section class="metric-grid">
        <article class="metric-card">
            <span>模型版本</span>
            <strong><%= HtmlUtil.escape(summary.getModelVersion()) %></strong>
            <small><%= summary.getModelVersion() != null && summary.getModelVersion().equals(operationalModelVersion)
                    ? "当前运营模型，后台默认用于评分和复核线索"
                    : "非运营模型，仅用于对比、回测或专项排查" %></small>
        </article>
        <article class="metric-card danger">
            <span>建议阻断</span>
            <strong><%= summary.getBlockCount() %></strong>
            <small>标签命中率 <%= rate(summary.getBlockFraudCount(), summary.getBlockCount()) %></small>
        </article>
        <article class="metric-card warning">
            <span>人工复核</span>
            <strong><%= summary.getReviewCount() %></strong>
            <small>标签命中率 <%= rate(summary.getReviewFraudCount(), summary.getReviewCount()) %></small>
        </article>
        <article class="metric-card">
            <span>正常放行</span>
            <strong><%= summary.getPassCount() %></strong>
            <small>标签命中率 <%= rate(summary.getPassFraudCount(), summary.getPassCount()) %></small>
        </article>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">模型证据</p>
                <h2>评分分层说明</h2>
            </div>
            <span class="tag">总评分 <%= summary.getTotalScores() %> 条</span>
        </div>
        <div class="admin-insight-grid">
            <div class="admin-insight-card">
                <span>真实标签总量</span>
                <strong><%= summary.getFraudLabelCount() %></strong>
                <p>该值来自 IBM AML 的 Is Laundering 标签，仅用于训练评估和演示解释。</p>
            </div>
            <div class="admin-insight-card">
                <span>平均模型分</span>
                <strong><%= summary.getAverageScore() == null ? "0" : summary.getAverageScore().setScale(0, RoundingMode.HALF_UP).toPlainString() %></strong>
                <p>模型分为 0-1000，越高表示图结构和边特征越接近洗钱交易。</p>
            </div>
            <div class="admin-insight-card">
                <span>复核阈值</span>
                <strong><%= probabilityText(summary.getReviewThreshold()) %></strong>
                <p>达到该概率进入人工复核队列；达到阻断阈值则进入建议阻断队列。</p>
            </div>
            <div class="admin-insight-card">
                <span>阻断阈值</span>
                <strong><%= probabilityText(summary.getBlockThreshold()) %></strong>
                <p>当前为模型侧导出的演示阈值，生产环境还需要结合规则引擎与人工策略。</p>
            </div>
            <div class="admin-insight-card decision-layer-card">
                <span>业务决策层</span>
                <strong>模型 ≠ 裁决</strong>
                <p>最终建议会综合模型分、标签冲突和图谱邻域证据。模型高分但标签正常时，系统会标为“标签冲突复核”。</p>
            </div>
        </div>
    </section>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/risk/graph-scores" class="filter-form">
            <label>
                <span>模型版本</span>
                <select name="modelVersion">
                    <% for (String version : modelVersions) { %>
                        <option value="<%= HtmlUtil.escape(version) %>" <%= version.equals(selectedModelVersion) ? "selected" : "" %>><%= HtmlUtil.escape(version) %><%= version.equals(operationalModelVersion) ? "（运营）" : "" %></option>
                    <% } %>
                </select>
            </label>
            <label>
                <span>决策</span>
                <select name="decision">
                    <option value="">全部</option>
                    <option value="BLOCK" <%= "BLOCK".equals(selectedDecision) ? "selected" : "" %>>建议阻断</option>
                    <option value="REVIEW" <%= "REVIEW".equals(selectedDecision) ? "selected" : "" %>>人工复核</option>
                    <option value="PASS" <%= "PASS".equals(selectedDecision) ? "selected" : "" %>>正常放行</option>
                </select>
            </label>
            <label>
                <span>交易图边类型</span>
                <select name="edgeType">
                    <option value="">全部</option>
                    <% for (String type : edgeTypes) { %>
                        <option value="<%= HtmlUtil.escape(type) %>" <%= type.equals(selectedEdgeType) ? "selected" : "" %>><%= HtmlUtil.escape(type) %></option>
                    <% } %>
                </select>
            </label>
            <label>
                <span>最低评分</span>
                <input type="number" min="0" max="1000" name="minScore" value="<%= HtmlUtil.escape(selectedMinScore) %>" placeholder="如 800">
            </label>
            <label>
                <span>真实标签</span>
                <select name="labelFraud">
                    <option value="">全部</option>
                    <option value="1" <%= "1".equals(selectedLabelFraud) ? "selected" : "" %>>洗钱标签</option>
                    <option value="0" <%= "0".equals(selectedLabelFraud) ? "selected" : "" %>>正常标签</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section graph-evidence-section" id="graphEvidence">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">图谱证据</p>
                <h2>局部风险证据图谱</h2>
                <p class="muted">选择下方任意高分图边，查看它的来源账户、目标账户和一跳相邻交易。红色代表建议阻断，金色代表人工复核，虚线代表真实洗钱标签。</p>
            </div>
            <span class="tag" id="graphStatus">等待选择图边</span>
        </div>
        <div class="risk-graph-workbench">
            <div class="risk-graph-canvas" id="riskGraphCanvas">
                <div class="risk-graph-toolbar">
                    <div class="risk-graph-legend">
                        <span><i class="legend-dot source"></i>来源账户</span>
                        <span><i class="legend-dot target"></i>目标账户</span>
                        <span><i class="legend-line block"></i>建议阻断</span>
                        <span><i class="legend-line review"></i>人工复核</span>
                        <span><i class="legend-line conflict"></i>标签冲突</span>
                        <span><i class="legend-line fraud"></i>洗钱标签</span>
                    </div>
                    <div class="risk-graph-actions">
                        <button class="button secondary compact" type="button" id="graphFitButton" disabled>居中</button>
                        <button class="button secondary compact" type="button" id="graphLayoutButton" disabled>重排</button>
                        <button class="button secondary compact" type="button" id="graphLabelButton" disabled>标签</button>
                    </div>
                </div>
                <div class="risk-graph-viewport" id="riskGraphViewport">
                    <div class="human-empty">
                        <strong>先选择一条高风险图边</strong>
                        <span>点击表格中的“查看图谱”，这里会展示局部交易网络，帮助判断这笔交易是否处在可疑资金链路中。</span>
                    </div>
                </div>
                <div class="risk-graph-summary" id="riskGraphSummary">
                    <span>节点 0</span>
                    <span>图边 0</span>
                    <span>建议阻断 0</span>
                    <span>标签 0</span>
                </div>
            </div>
            <aside class="risk-graph-detail" id="riskGraphDetail">
                <p class="eyebrow">证据详情</p>
                <h3>图谱证据说明</h3>
                <p>当前图谱只展示中心交易的一跳邻域，避免把 18 万节点的训练图直接塞进浏览器。它用于辅助研判，不直接替代风控处置结论。</p>
                <dl>
                    <div><dt>节点颜色</dt><dd>蓝色为来源，绿色为目标，深色为相邻账户</dd></div>
                    <div><dt>边颜色</dt><dd>红色建议阻断，金色人工复核，灰色正常放行</dd></div>
                    <div><dt>虚线</dt><dd>数据集真实标签中标记为洗钱的交易边</dd></div>
                </dl>
            </aside>
        </div>
    </section>

    <section class="content-section">
        <div class="section-title-row">
            <div>
                <p class="eyebrow">高分交易边</p>
                <h2>高风险图边列表</h2>
            </div>
            <span class="tag">最多显示 200 条</span>
        </div>
        <div class="table-wrap">
            <table class="wide-table">
                <thead>
                <tr>
                    <th>模型决策</th>
                    <th>评分</th>
                    <th>模型置信度</th>
                    <th>业务建议</th>
                    <th>真实标签</th>
                    <th>邻域证据</th>
                    <th>类型</th>
                    <th>金额</th>
                    <th>时间</th>
                    <th>来源节点</th>
                    <th>目标节点</th>
                    <th>批次/行号</th>
                    <th>模型原始输出</th>
                    <th>图谱证据</th>
                </tr>
                </thead>
                <tbody>
                <% if (scores.isEmpty()) { %>
                    <tr><td colspan="14" class="empty">暂无模型评分。完成 GNN 评分回灌后会在这里展示。</td></tr>
                <% } else {
                    for (AdminRiskGraphScoreView score : scores) {
                %>
                    <tr>
                        <td><span class="tag <%= decisionClass(score.getDecision()) %>"><%= HtmlUtil.escape(decisionName(score.getDecision())) %></span></td>
                        <td><strong><%= score.getRiskScore() %></strong></td>
                        <td><%= probabilityText(score.getRiskProbability()) %></td>
                        <td>
                            <span class="tag <%= severityClass(score.getBusinessDecisionSeverity()) %>">
                                <%= HtmlUtil.escape(score.getBusinessDecisionLabel()) %>
                            </span>
                            <small class="table-note"><%= HtmlUtil.escape(score.getBusinessDecisionReason()) %></small>
                        </td>
                        <td>
                            <% if (score.isLabelFraud()) { %>
                                <span class="tag severity-danger">洗钱</span>
                            <% } else { %>
                                <span class="tag <%= score.isLabelConflict() ? "severity-conflict" : "" %>">正常</span>
                            <% } %>
                        </td>
                        <td>
                            <small class="table-note">点击右侧“查看图谱”后计算一跳邻域证据，避免列表页加载全量关系。</small>
                        </td>
                        <td><%= HtmlUtil.escape(score.getEdgeType()) %></td>
                        <td><%= HtmlUtil.escape(score.getCurrency()) %> <%= moneyText(score.getAmount()) %></td>
                        <td><%= HtmlUtil.escape(timeText(score.getEventTime())) %></td>
                        <td title="<%= HtmlUtil.escape(score.getFromExternalId()) %>"><%= HtmlUtil.escape(compactNode(score.getFromExternalId())) %></td>
                        <td title="<%= HtmlUtil.escape(score.getToExternalId()) %>"><%= HtmlUtil.escape(compactNode(score.getToExternalId())) %></td>
                        <td><%= HtmlUtil.escape(score.getBatchCode()) %> / <%= score.getSourceRowNo() %></td>
                        <td>
                            <details class="raw-reason">
                                <summary>查看原始输出</summary>
                                <code><%= HtmlUtil.escape(score.getReasonJson()) %></code>
                            </details>
                        </td>
                        <td>
                            <button class="button secondary compact js-open-graph" type="button"
                                    data-edge-id="<%= score.getGraphEdgeId() %>"
                                    data-model-version="<%= HtmlUtil.escape(score.getModelVersion()) %>">
                                查看图谱
                            </button>
                        </td>
                    </tr>
                <%  }
                } %>
                </tbody>
            </table>
        </div>
    </section>
</main>
<script>
window.__ibankCytoscapeModule = window.module;
window.__ibankCytoscapeExports = window.exports;
try {
    window.module = undefined;
    window.exports = undefined;
} catch (e) {
    // Keep the page usable even in browsers that disallow rewriting these globals.
}
</script>
<script src="<%= request.getContextPath() %>/assets/vendor/cytoscape/cytoscape.min.js"></script>
<script>
try {
    window.module = window.__ibankCytoscapeModule;
    window.exports = window.__ibankCytoscapeExports;
} catch (e) {
    // Ignore restoration errors in constrained browser runtimes.
}
</script>
<script>
(function () {
    var endpoint = '<%= request.getContextPath() %>/admin/risk/graph-neighborhood';
    var canvas = document.getElementById('riskGraphViewport');
    var detail = document.getElementById('riskGraphDetail');
    var status = document.getElementById('graphStatus');
    var summary = document.getElementById('riskGraphSummary');
    var fitButton = document.getElementById('graphFitButton');
    var layoutButton = document.getElementById('graphLayoutButton');
    var labelButton = document.getElementById('graphLabelButton');
    var graph = null;
    var labelsVisible = false;

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value).replace(/[&<>"']/g, function (ch) {
            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[ch];
        });
    }

    function decisionLabel(value) {
        if (value === 'BLOCK') return '建议阻断';
        if (value === 'REVIEW') return '人工复核';
        if (value === 'PASS') return '正常放行';
        return value || '未评分';
    }

    function money(value, currency) {
        if (value === null || value === undefined) {
            return escapeHtml(currency || '') + ' 0.00';
        }
        return escapeHtml(currency || '') + ' ' + Number(value).toLocaleString('zh-CN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function percent(value) {
        if (value === null || value === undefined) {
            return '0.00%';
        }
        return (Number(value) * 100).toFixed(2) + '%';
    }

    function setLoading() {
        status.textContent = '正在加载图谱';
        setGraphControls(false);
        canvas.innerHTML = '<div class="human-empty"><strong>正在生成局部证据图谱</strong><span>系统正在读取中心交易的一跳邻域和模型评分。</span></div>';
    }

    function setError(message) {
        status.textContent = '图谱加载失败';
        setGraphControls(false);
        canvas.innerHTML = '<div class="human-empty danger"><strong>暂时无法展示图谱</strong><span>' + escapeHtml(message) + '</span></div>';
    }

    function setGraphControls(enabled) {
        fitButton.disabled = !enabled;
        layoutButton.disabled = !enabled;
        labelButton.disabled = !enabled;
    }

    function updateGraphSummary(data) {
        summary.innerHTML =
            '<span>节点 ' + data.nodeCount + '</span>' +
            '<span>图边 ' + data.edgeCount + '</span>' +
            '<span>建议阻断 ' + data.blockEdgeCount + '</span>' +
            '<span>人工复核 ' + data.reviewEdgeCount + '</span>' +
            '<span>标签 ' + data.fraudEdgeCount + '</span>';
    }

    function showOverview(data) {
        detail.innerHTML =
            '<p class="eyebrow">图谱证据</p>' +
            '<h3>局部证据概览</h3>' +
            '<p>本次展示中心图边的一跳邻域，用于解释模型为什么把这条交易排在高风险队列中。</p>' +
            '<dl>' +
            '<div><dt>模型版本</dt><dd>' + escapeHtml(data.modelVersion) + '</dd></div>' +
            '<div><dt>节点 / 图边</dt><dd>' + data.nodeCount + ' / ' + data.edgeCount + '</dd></div>' +
            '<div><dt>阻断 / 复核</dt><dd>' + data.blockEdgeCount + ' / ' + data.reviewEdgeCount + '</dd></div>' +
            '<div><dt>真实洗钱标签</dt><dd>' + data.fraudEdgeCount + ' 条</dd></div>' +
            '</dl>';
    }

    function showNode(node) {
        detail.innerHTML =
            '<p class="eyebrow">选中节点</p>' +
            '<h3>' + escapeHtml(node.label) + '</h3>' +
            '<p>节点代表外部交易图中的账户或机构实体。风险人员可以关注它是否连接了多条建议阻断或人工复核边。</p>' +
            '<dl>' +
            '<div><dt>外部 ID</dt><dd>' + escapeHtml(node.externalId) + '</dd></div>' +
            '<div><dt>节点类型</dt><dd>' + escapeHtml(node.nodeType) + '</dd></div>' +
            '<div><dt>全局入/出度</dt><dd>' + node.inDegree + ' / ' + node.outDegree + '</dd></div>' +
            '<div><dt>邻域图边</dt><dd>' + node.neighborhoodEdges + ' 条，其中风险相关 ' + node.neighborhoodRiskEdges + ' 条</dd></div>' +
            '<div><dt>总入账</dt><dd>' + money(node.totalInAmount, '') + '</dd></div>' +
            '<div><dt>总出账</dt><dd>' + money(node.totalOutAmount, '') + '</dd></div>' +
            '</dl>';
    }

    function showEdge(edge) {
        detail.innerHTML =
            '<p class="eyebrow">选中交易边</p>' +
            '<h3>' + escapeHtml(decisionLabel(edge.decision)) + ' · ' + edge.riskScore + '</h3>' +
            '<p>边代表一次交易或资金流动。中心边是你在表格中选择的那笔交易，相邻边用于判断是否存在拆分、汇聚或中转迹象。业务建议不会直接等同于模型概率。</p>' +
            '<dl>' +
            '<div><dt>业务建议</dt><dd>' + escapeHtml(edge.businessDecisionLabel) + '</dd></div>' +
            '<div><dt>建议原因</dt><dd>' + escapeHtml(edge.businessDecisionReason) + '</dd></div>' +
            '<div><dt>图边 ID</dt><dd>' + edge.graphEdgeId + '</dd></div>' +
            '<div><dt>交易类型</dt><dd>' + escapeHtml(edge.edgeType) + '</dd></div>' +
            '<div><dt>金额</dt><dd>' + money(edge.amount, edge.currency) + '</dd></div>' +
            '<div><dt>模型置信度</dt><dd>' + percent(edge.riskProbability) + '</dd></div>' +
            '<div><dt>真实标签</dt><dd>' + (edge.labelFraud ? '洗钱标签' : '正常标签') + '</dd></div>' +
            '<div><dt>批次/行号</dt><dd>' + escapeHtml(edge.batchCode) + ' / ' + edge.sourceRowNo + '</dd></div>' +
            '<div><dt>时间</dt><dd>' + escapeHtml(edge.eventTime) + '</dd></div>' +
            '</dl>' +
            '<pre class="risk-graph-reason">' + escapeHtml(edge.reasonJson || '') + '</pre>';
    }

    function render(data) {
        var cytoscapeFactory = window.cytoscape;
        if (!cytoscapeFactory && typeof cytoscape !== 'undefined') {
            cytoscapeFactory = cytoscape;
        }
        if (!cytoscapeFactory) {
            setError('Cytoscape.js 没有加载成功，请检查静态资源是否已部署。');
            return;
        }
        if (graph) {
            graph.destroy();
            graph = null;
        }
        labelsVisible = false;
        labelButton.classList.remove('active');
        canvas.innerHTML = '';
        graph = cytoscapeFactory({
            container: canvas,
            elements: data.nodes.concat(data.edges),
            style: [
                {selector: 'node', style: {
                    'label': '',
                    'font-size': 12,
                    'font-weight': 800,
                    'text-wrap': 'wrap',
                    'text-max-width': 118,
                    'text-valign': 'bottom',
                    'text-margin-y': 10,
                    'text-outline-width': 3,
                    'text-outline-color': '#071326',
                    'color': '#f8fbff',
                    'background-color': '#64748b',
                    'border-width': 2,
                    'border-color': 'rgba(255,255,255,0.72)',
                    'width': 'mapData(degree, 1, 120, 34, 76)',
                    'height': 'mapData(degree, 1, 120, 34, 76)',
                    'shadow-blur': 12,
                    'shadow-color': 'rgba(0,0,0,0.32)',
                    'shadow-opacity': 0.7,
                    'shadow-offset-x': 0,
                    'shadow-offset-y': 6
                }},
                {selector: 'node.risk-node', style: {'border-color': '#d83b2d', 'border-width': 5}},
                {selector: 'node.role-source', style: {'label': 'data(label)', 'background-color': '#2563eb', 'width': 76, 'height': 76}},
                {selector: 'node.role-target', style: {'label': 'data(label)', 'background-color': '#0f766e', 'width': 76, 'height': 76}},
                {selector: 'node.role-source-target', style: {'label': 'data(label)', 'background-color': '#7c3aed', 'width': 80, 'height': 80}},
                {selector: 'node.labels-on, node.selected, node.hovered', style: {'label': 'data(label)'}},
                {selector: 'edge', style: {
                    'curve-style': 'unbundled-bezier',
                    'control-point-distances': 36,
                    'control-point-weights': 0.5,
                    'target-arrow-shape': 'triangle',
                    'label': '',
                    'font-size': 10,
                    'font-weight': 800,
                    'text-rotation': 'autorotate',
                    'text-margin-y': -10,
                    'text-outline-width': 3,
                    'text-outline-color': '#071326',
                    'color': '#f8fbff',
                    'line-color': 'rgba(148, 163, 184, 0.62)',
                    'target-arrow-color': 'rgba(148, 163, 184, 0.7)',
                    'width': 2.2,
                    'opacity': 0.72
                }},
                {selector: 'edge.decision-block', style: {'line-color': '#ef4444', 'target-arrow-color': '#ef4444', 'width': 3.8, 'opacity': 0.9}},
                {selector: 'edge.decision-review', style: {'line-color': '#f5b43f', 'target-arrow-color': '#f5b43f', 'width': 3, 'opacity': 0.86}},
                {selector: 'edge.decision-pass', style: {'line-color': 'rgba(148, 163, 184, 0.5)', 'target-arrow-color': 'rgba(148, 163, 184, 0.58)'}},
                {selector: 'edge.label-conflict-edge', style: {'line-color': '#f97316', 'target-arrow-color': '#f97316', 'line-style': 'dotted', 'width': 5}},
                {selector: 'edge.fraud-edge', style: {'line-style': 'dashed'}},
                {selector: 'edge.center-edge', style: {'label': '中心交易 data(riskScore)', 'line-color': '#ff3d2e', 'target-arrow-color': '#ff3d2e', 'width': 8, 'opacity': 1, 'z-index': 30}},
                {selector: 'edge.labels-on, edge.selected, edge.hovered', style: {'label': 'data(label)'}},
                {selector: '.selected', style: {'overlay-opacity': 0.2, 'overlay-color': '#38bdf8'}},
                {selector: '.hovered', style: {'overlay-opacity': 0.12, 'overlay-color': '#f8fbff'}}
            ],
            layout: {
                name: 'cose',
                animate: false,
                fit: true,
                padding: 54,
                nodeRepulsion: 12000,
                idealEdgeLength: 145,
                edgeElasticity: 90,
                gravity: 0.22
            },
            wheelSensitivity: 0.22
        });
        graph.on('mouseover', 'node, edge', function (event) {
            event.target.addClass('hovered');
        });
        graph.on('mouseout', 'node, edge', function (event) {
            event.target.removeClass('hovered');
        });
        graph.on('tap', 'node', function (event) {
            graph.elements().removeClass('selected');
            event.target.addClass('selected');
            showNode(event.target.data());
        });
        graph.on('tap', 'edge', function (event) {
            graph.elements().removeClass('selected');
            event.target.addClass('selected');
            showEdge(event.target.data());
        });
        status.textContent = '已展示 ' + data.nodeCount + ' 个节点 / ' + data.edgeCount + ' 条边';
        updateGraphSummary(data);
        setGraphControls(true);
        showOverview(data);
        var centerEdge = graph.edges('.center-edge');
        if (centerEdge.length > 0) {
            centerEdge.addClass('selected');
            showEdge(centerEdge.data());
        }
    }

    fitButton.addEventListener('click', function () {
        if (graph) {
            graph.fit(graph.elements(), 46);
            graph.center();
        }
    });

    layoutButton.addEventListener('click', function () {
        if (graph) {
            graph.layout({
                name: 'cose',
                animate: false,
                fit: true,
                padding: 54,
                nodeRepulsion: 12000,
                idealEdgeLength: 145,
                edgeElasticity: 90,
                gravity: 0.22
            }).run();
        }
    });

    labelButton.addEventListener('click', function () {
        if (!graph) {
            return;
        }
        labelsVisible = !labelsVisible;
        labelButton.classList.toggle('active', labelsVisible);
        if (labelsVisible) {
            graph.elements().addClass('labels-on');
        } else {
            graph.elements().removeClass('labels-on');
        }
    });

    document.querySelectorAll('.js-open-graph').forEach(function (button) {
        button.addEventListener('click', function () {
            setLoading();
            document.getElementById('graphEvidence').scrollIntoView({behavior: 'smooth', block: 'start'});
            var url = endpoint + '?graphEdgeId=' + encodeURIComponent(button.getAttribute('data-edge-id')) +
                '&modelVersion=' + encodeURIComponent(button.getAttribute('data-model-version') || '');
            fetch(url, {
                credentials: 'same-origin',
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
                .then(function (response) {
                    var contentType = response.headers.get('Content-Type') || '';
                    if (contentType.indexOf('application/json') === -1) {
                        return response.text().then(function (text) {
                            if (response.status === 401 || text.indexOf('/admin/login') >= 0
                                    || text.indexOf('进入运营控制台') >= 0) {
                                throw new Error('后台登录已过期，请重新登录后再查看图谱。');
                            }
                            if (response.status === 403 || text.indexOf('requiredPermission') >= 0) {
                                throw new Error('当前账号没有查看图谱的权限。');
                            }
                            throw new Error('图谱接口返回了非 JSON 页面，请刷新后重试。');
                        });
                    }
                    return response.json().then(function (data) {
                        if (!response.ok || !data.success) {
                            throw new Error(data.message || '图谱接口返回异常。');
                        }
                        return data;
                    });
                })
                .then(render)
                .catch(function (error) {
                    setError(error.message || '图谱加载失败。');
                });
        });
    });
})();
</script>
</body>
</html>
