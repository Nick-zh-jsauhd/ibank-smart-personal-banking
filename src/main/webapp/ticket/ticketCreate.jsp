<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.util.HtmlUtil" %>
<%!
    private boolean selected(String current, String expected) {
        return expected != null && expected.equals(current);
    }
%>
<%
    String error = (String) request.getAttribute("error");
    String prefillType = (String) request.getAttribute("prefillType");
    String prefillPriority = (String) request.getAttribute("prefillPriority");
    String prefillTitle = (String) request.getAttribute("prefillTitle");
    String prefillDescription = (String) request.getAttribute("prefillDescription");
    String prefillBusinessType = (String) request.getAttribute("prefillBusinessType");
    String prefillBusinessId = (String) request.getAttribute("prefillBusinessId");
    if (prefillPriority == null || prefillPriority.length() == 0) {
        prefillPriority = "NORMAL";
    }
    request.setAttribute("activeNav", "service");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>发起服务工单 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="compact-page-head">
        <div class="page-heading">
            <p class="eyebrow">新建工单</p>
            <h1>发起服务工单</h1>
            <p class="muted">把诉求、金额、时间、对方账户或业务编号写清楚，后台会自动分配给对应角色处理。</p>
        </div>
        <a class="button secondary compact" href="<%= request.getContextPath() %>/tickets">返回工单列表</a>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <div class="care-note">
        <div>
            <strong>描述越具体，后台越容易处理。</strong>
            <p>建议写清楚发生时间、金额、交易编号、当前问题和你希望银行给出的处理结果。</p>
        </div>
    </div>

    <section class="task-layout">
        <article class="task-panel with-warm-accent">
            <div class="section-title">
                <div>
                    <h2>工单内容</h2>
                    <p class="section-note">交易争议、风控申诉、账户和理财服务都可以在这里发起。</p>
                </div>
            </div>
            <form class="form grid-form" method="post" action="<%= request.getContextPath() %>/ticket/create">
                <label>
                    <span>问题类型</span>
                    <select name="ticketType" required>
                        <option value="">请选择</option>
                        <option value="TRANSACTION_DISPUTE" <%= selected(prefillType, "TRANSACTION_DISPUTE") ? "selected" : "" %>>交易争议</option>
                        <option value="RISK_APPEAL" <%= selected(prefillType, "RISK_APPEAL") ? "selected" : "" %>>风控申诉</option>
                        <option value="ACCOUNT_SERVICE" <%= selected(prefillType, "ACCOUNT_SERVICE") ? "selected" : "" %>>账户服务</option>
                        <option value="WEALTH_SERVICE" <%= selected(prefillType, "WEALTH_SERVICE") ? "selected" : "" %>>理财服务</option>
                        <option value="ADJUSTMENT_INQUIRY" <%= selected(prefillType, "ADJUSTMENT_INQUIRY") ? "selected" : "" %>>调账咨询</option>
                        <option value="GENERAL" <%= selected(prefillType, "GENERAL") ? "selected" : "" %>>其他问题</option>
                    </select>
                </label>
                <label>
                    <span>优先级</span>
                    <select name="priority">
                        <option value="NORMAL" <%= selected(prefillPriority, "NORMAL") ? "selected" : "" %>>普通</option>
                        <option value="HIGH" <%= selected(prefillPriority, "HIGH") ? "selected" : "" %>>高</option>
                        <option value="URGENT" <%= selected(prefillPriority, "URGENT") ? "selected" : "" %>>紧急</option>
                        <option value="LOW" <%= selected(prefillPriority, "LOW") ? "selected" : "" %>>低</option>
                    </select>
                </label>
                <label class="full-row">
                    <span>标题</span>
                    <input name="title" maxlength="120" required value="<%= HtmlUtil.escape(prefillTitle) %>" placeholder="例如：5 月 18 日转账未到账">
                </label>
                <label>
                    <span>关联业务类型</span>
                    <select name="relatedBusinessType">
                        <option value="">无</option>
                        <option value="TRANSACTION" <%= selected(prefillBusinessType, "TRANSACTION") ? "selected" : "" %>>交易流水</option>
                        <option value="RISK_EVENT" <%= selected(prefillBusinessType, "RISK_EVENT") ? "selected" : "" %>>风控事件</option>
                        <option value="WEALTH_ORDER" <%= selected(prefillBusinessType, "WEALTH_ORDER") ? "selected" : "" %>>理财订单</option>
                        <option value="ADJUSTMENT_REQUEST" <%= selected(prefillBusinessType, "ADJUSTMENT_REQUEST") ? "selected" : "" %>>调账申请</option>
                        <option value="ACCOUNT" <%= selected(prefillBusinessType, "ACCOUNT") ? "selected" : "" %>>账户</option>
                        <option value="OTHER" <%= selected(prefillBusinessType, "OTHER") ? "selected" : "" %>>其他</option>
                    </select>
                </label>
                <label>
                    <span>关联业务编号</span>
                    <input name="relatedBusinessId" maxlength="64" value="<%= HtmlUtil.escape(prefillBusinessId) %>" placeholder="交易号、风控事件 ID 或订单号">
                </label>
                <label class="full-row">
                    <span>问题描述</span>
                    <textarea name="description" rows="8" maxlength="1000" required placeholder="请写清楚发生时间、金额、账户、当前状态和你希望银行处理的结果"><%= HtmlUtil.escape(prefillDescription) %></textarea>
                </label>
                <div class="form-actions full-row">
                    <button class="button safe" type="submit">提交工单</button>
                    <a class="button secondary" href="<%= request.getContextPath() %>/tickets">返回列表</a>
                </div>
            </form>
        </article>

        <aside class="task-side">
            <section class="content-section">
                <h2>填写建议</h2>
                <ul class="task-checklist">
                    <li>交易争议请填写交易编号、金额、时间和对方账户。</li>
                    <li>风控申诉请从风控事件入口带入事件 ID，便于后台核查。</li>
                    <li>理财服务请填写产品名称、申购金额或持仓信息。</li>
                    <li>紧急工单会优先进入后台待办，但仍需完整描述。</li>
                </ul>
            </section>
            <section class="content-section">
                <h2>服务闭环</h2>
                <div class="event-feed">
                    <article class="event-card">
                        <div class="event-card-main">
                            <strong>提交</strong>
                            <p>客户发起诉求，系统记录关联业务。</p>
                        </div>
                        <span class="tag">1</span>
                    </article>
                    <article class="event-card">
                        <div class="event-card-main">
                            <strong>处理</strong>
                            <p>后台按角色受理、回复或调整。</p>
                        </div>
                        <span class="tag">2</span>
                    </article>
                    <article class="event-card">
                        <div class="event-card-main">
                            <strong>确认</strong>
                            <p>客户确认关闭，或重新打开继续跟进。</p>
                        </div>
                        <span class="tag">3</span>
                    </article>
                </div>
            </section>
        </aside>
    </section>
</main>
</body>
</html>
