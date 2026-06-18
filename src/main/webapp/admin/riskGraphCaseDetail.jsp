<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AdminRiskGraphReviewCaseView,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.sql.Timestamp" %>
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

    private String caseTypeLabel(String type) {
        if ("LABEL_CONFLICT".equals(type)) return "标签冲突复核";
        if ("TRUE_POSITIVE_BLOCK".equals(type)) return "重点调查样本";
        if ("STRONG_REVIEW".equals(type)) return "强复核样本";
        if ("MODEL_REVIEW".equals(type)) return "模型复核样本";
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

    private String businessDecisionLabel(String decision) {
        if ("LABEL_CONFLICT".equals(decision)) return "标签冲突复核";
        if ("INVESTIGATION_REQUIRED".equals(decision)) return "重点调查";
        if ("STRONG_REVIEW".equals(decision)) return "强化复核";
        if ("REVIEW".equals(decision)) return "人工复核";
        if ("BLOCK".equals(decision)) return "建议阻断";
        if ("PASS".equals(decision)) return "正常放行";
        return decision == null ? "" : decision;
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    AdminRiskGraphReviewCaseView reviewCase =
            (AdminRiskGraphReviewCaseView) request.getAttribute("reviewCase");
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    boolean canHandle = adminUser != null && adminUser.hasPermission("RISK_GRAPH_CASE_HANDLE");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>GNN复核详情 - iBank Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page lab-page">
<%@ include file="/WEB-INF/jsp/adminLabTopbar.jspf" %>

<main class="layout layout-wide">
    <section class="page-heading">
        <p class="eyebrow">复核样本详情</p>
        <h1>GNN复核样本处理</h1>
        <p class="muted">人工结论会保留在复核队列中，用来解释模型误判、识别真实风险，并为后续训练集增强提供反馈。</p>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <% if (reviewCase == null) { %>
        <section class="content-section">
            <div class="human-empty">
                <strong>没有找到这条复核样本</strong>
                <span>返回复核队列，重新选择需要处理的模型样本。</span>
            </div>
            <a class="button secondary" href="<%= request.getContextPath() %>/admin/risk/graph-cases">返回复核队列</a>
        </section>
    <% } else { %>
        <section class="summary-strip">
            <article class="summary-item">
                <span>样本类型</span>
                <strong><%= HtmlUtil.escape(caseTypeLabel(reviewCase.getCaseType())) %></strong>
            </article>
            <article class="summary-item">
                <span>状态</span>
                <strong><%= HtmlUtil.escape(statusLabel(reviewCase.getCaseStatus())) %></strong>
            </article>
            <article class="summary-item">
                <span>模型分</span>
                <strong><%= reviewCase.getRiskScore() %></strong>
            </article>
            <article class="summary-item">
                <span>源标签</span>
                <strong><%= reviewCase.isLabelFraud() ? "洗钱" : "正常" %></strong>
            </article>
        </section>

        <div class="detail-workspace">
            <div class="detail-main">
                <section class="content-section emphasis">
                    <div class="section-title">
                        <div>
                            <h2>复核原因</h2>
                            <p class="section-note"><%= HtmlUtil.escape(reviewCase.getReason()) %></p>
                        </div>
                        <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/risk/graph-cases">返回队列</a>
                    </div>
                    <dl class="detail-list">
                        <div><dt>复核编号</dt><dd><%= reviewCase.getCaseId() %></dd></div>
                        <div><dt>图边 ID</dt><dd><%= reviewCase.getGraphEdgeId() %></dd></div>
                        <div><dt>模型版本</dt><dd><%= HtmlUtil.escape(reviewCase.getModelVersion()) %></dd></div>
                        <div><dt>模型决策</dt><dd><%= HtmlUtil.escape(businessDecisionLabel(reviewCase.getModelDecision())) %></dd></div>
                        <div><dt>业务建议</dt><dd><%= HtmlUtil.escape(businessDecisionLabel(reviewCase.getBusinessDecision())) %></dd></div>
                        <div><dt>模型置信度</dt><dd><%= probabilityText(reviewCase.getRiskProbability()) %></dd></div>
                        <div><dt>优先级</dt><dd><%= reviewCase.getPriority() %></dd></div>
                        <div><dt>创建时间</dt><dd><%= timeText(reviewCase.getCreatedAt()) %></dd></div>
                    </dl>
                </section>

                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2>交易图边信息</h2>
                            <p class="section-note">这部分来自 IBM AML 图数据，表示一次账户之间的资金流动。复核时重点看金额、交易类型、节点关系和图谱邻域。</p>
                        </div>
                    </div>
                    <dl class="detail-list">
                        <div><dt>批次 / 行号</dt><dd><%= HtmlUtil.escape(reviewCase.getBatchCode()) %> / <%= reviewCase.getSourceRowNo() %></dd></div>
                        <div><dt>数据集</dt><dd><%= HtmlUtil.escape(reviewCase.getDatasetName()) %></dd></div>
                        <div><dt>交易类型</dt><dd><%= HtmlUtil.escape(reviewCase.getEdgeType()) %></dd></div>
                        <div><dt>金额</dt><dd><%= HtmlUtil.escape(reviewCase.getCurrency()) %> <%= moneyText(reviewCase.getAmount()) %></dd></div>
                        <div><dt>交易时间</dt><dd><%= timeText(reviewCase.getEventTime()) %></dd></div>
                        <div><dt>来源节点</dt><dd><%= HtmlUtil.escape(reviewCase.getFromExternalId()) %></dd></div>
                        <div><dt>目标节点</dt><dd><%= HtmlUtil.escape(reviewCase.getToExternalId()) %></dd></div>
                    </dl>
                </section>

                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2>模型输出原因</h2>
                            <p class="section-note">这是训练脚本回灌的结构化评分信息，用于辅助人工判断，不是最终风控结论。</p>
                        </div>
                    </div>
                    <pre class="risk-graph-reason"><%= HtmlUtil.escape(reviewCase.getReasonJson()) %></pre>
                </section>
            </div>

            <aside class="detail-side">
                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2>处理建议</h2>
                            <p class="section-note">标签冲突样本的重点不是“模型错/数据错”二选一，而是记录为什么业务上应该确认、降级或继续补证据。</p>
                        </div>
                    </div>
                    <div class="section-actions">
                        <a class="button secondary compact" href="<%= request.getContextPath() %>/admin/risk/graph-scores?modelVersion=<%= HtmlUtil.escape(reviewCase.getModelVersion()) %>&minScore=<%= reviewCase.getRiskScore() %>">回到评分看板</a>
                    </div>
                </section>

                <section class="content-section">
                    <div class="section-title">
                        <div>
                            <h2>人工复核结论</h2>
                            <p class="section-note">结论会写入审计日志，并保留在模型反馈样本表中。</p>
                        </div>
                    </div>
                    <% if (reviewCase.getReviewResult() != null) { %>
                        <dl class="detail-list">
                            <div><dt>当前结论</dt><dd><%= HtmlUtil.escape(reviewResultLabel(reviewCase.getReviewResult())) %></dd></div>
                            <div><dt>复核人</dt><dd><%= HtmlUtil.escape(reviewCase.getReviewerUsername()) %></dd></div>
                            <div><dt>复核时间</dt><dd><%= timeText(reviewCase.getReviewedAt()) %></dd></div>
                            <div><dt>备注</dt><dd><%= HtmlUtil.escape(reviewCase.getReviewNote()) %></dd></div>
                        </dl>
                    <% } %>

                    <% if (canHandle) { %>
                        <form class="form grid-form" method="post" action="<%= request.getContextPath() %>/admin/risk/graph-case/detail">
                            <input type="hidden" name="caseId" value="<%= reviewCase.getCaseId() %>">
                            <label>
                                <span>复核结论</span>
                                <select name="reviewResult" required>
                                    <option value="CONFIRMED_RISK">确认风险：可作为正样本/重点调查</option>
                                    <option value="FALSE_POSITIVE">误报样本：模型高分但业务判断正常</option>
                                    <option value="NEED_MORE_DATA">需补证据：继续保留待复核</option>
                                    <option value="IGNORE">忽略：不进入本轮反馈</option>
                                </select>
                            </label>
                            <label>
                                <span>复核说明</span>
                                <textarea name="reviewNote" maxlength="1000" rows="6" placeholder="写清楚判断依据：金额、节点关系、交易类型、是否需要补充资料或纳入训练反馈。"></textarea>
                            </label>
                            <button class="button primary" type="submit">保存复核结论</button>
                        </form>
                    <% } else { %>
                        <div class="human-empty">
                            <strong>当前账号只有查看权限</strong>
                            <span>需要风控运营或风控管理角色才能写入人工复核结论。</span>
                        </div>
                    <% } %>
                </section>
            </aside>
        </div>
    <% } %>
</main>
</body>
</html>
