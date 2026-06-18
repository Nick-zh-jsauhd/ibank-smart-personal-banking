<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.AssistantAction,com.bank.dto.AssistantReply,com.bank.dto.SessionUser,com.bank.util.HtmlUtil,java.util.List" %>
<%!
    private String actionHref(String contextPath, String href) {
        if (href == null || href.trim().length() == 0) {
            return contextPath + "/dashboard";
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        return contextPath + href;
    }

    private String actionClass(String style) {
        return "primary".equals(style) ? "button primary" : "button secondary";
    }

    private String displayQuestion(String question) {
        return question == null || question.trim().length() == 0 ? "" : question.trim();
    }

    private String intentName(String intent) {
        if ("WELCOME".equals(intent)) return "业务规则";
        if ("BILL".equals(intent)) return "账单解读";
        if ("TRANSACTION".equals(intent)) return "流水解读";
        if ("RISK".equals(intent)) return "风险解释";
        if ("SERVICE".equals(intent)) return "服务进度";
        if ("WEALTH".equals(intent)) return "理财匹配";
        if ("NOTICE".equals(intent)) return "通知待办";
        if ("SECURITY".equals(intent)) return "安全提醒";
        if ("FALLBACK".equals(intent)) return "问题引导";
        return "业务解释";
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    AssistantReply reply = (AssistantReply) request.getAttribute("reply");
    String question = (String) request.getAttribute("question");
    String error = (String) request.getAttribute("error");
    String displayName = loginUser != null && loginUser.getFullName() != null && loginUser.getFullName().trim().length() > 0
            ? loginUser.getFullName().trim()
            : "客户";
    request.setAttribute("activeNav", "assistant");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>小 i 助手 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page assistant-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="assistant-hero">
        <div class="assistant-hero-copy">
            <p class="eyebrow">小 i 助手</p>
            <h1>小 i 帮你先读懂账户、账单、风险和待办。</h1>
            <p class="section-note">它会基于当前系统数据做解释和导航，不替你提交任何资金类操作。需要转账、缴费、理财或改密码时，仍由你本人在业务页面确认。</p>
        </div>
        <div class="assistant-hero-card">
            <span>正在服务</span>
            <strong><%= HtmlUtil.escape(displayName) %></strong>
            <p>可询问：本月钱花到哪了、有没有异常交易、哪些理财更匹配、工单处理到哪了。</p>
        </div>
    </section>

    <section class="assistant-workspace">
        <article class="assistant-input-panel">
            <div class="section-title">
                <div>
                    <h2>问小 i</h2>
                    <p class="section-note">建议用业务语言提问，例如“帮我解释本月账单”或“最近有没有异常交易”。</p>
                </div>
            </div>
            <form class="assistant-form" method="post" action="<%= request.getContextPath() %>/assistant">
                <label>
                    <span>你的问题</span>
                    <textarea name="question" rows="5" maxlength="300" placeholder="例如：帮我总结本月账单，并告诉我下一步应该看哪里"><%= HtmlUtil.escape(displayQuestion(question)) %></textarea>
                </label>
                <div class="form-actions">
                    <button class="button primary" type="submit">发送给小 i</button>
                    <a class="button secondary" href="<%= request.getContextPath() %>/assistant">重新开始</a>
                </div>
            </form>
            <div class="assistant-quick-grid">
                <form method="get" action="<%= request.getContextPath() %>/assistant">
                    <input type="hidden" name="question" value="帮我总结本月账单">
                    <button type="submit">总结本月账单</button>
                </form>
                <form method="get" action="<%= request.getContextPath() %>/assistant">
                    <input type="hidden" name="question" value="最近有哪些大额交易">
                    <button type="submit">最近大额交易</button>
                </form>
                <form method="get" action="<%= request.getContextPath() %>/assistant">
                    <input type="hidden" name="question" value="我的风险等级是什么意思">
                    <button type="submit">解释风险等级</button>
                </form>
                <form method="get" action="<%= request.getContextPath() %>/assistant">
                    <input type="hidden" name="question" value="我的工单处理到哪了">
                    <button type="submit">工单处理进度</button>
                </form>
                <form method="get" action="<%= request.getContextPath() %>/assistant">
                    <input type="hidden" name="question" value="哪些理财产品适合我">
                    <button type="submit">理财匹配</button>
                </form>
                <form method="get" action="<%= request.getContextPath() %>/assistant">
                    <input type="hidden" name="question" value="有哪些未读通知">
                    <button type="submit">未读通知</button>
                </form>
            </div>
        </article>

        <aside class="assistant-boundary-panel">
            <p class="eyebrow">安全边界</p>
            <h2>助手只解释，不代操作。</h2>
            <ul class="task-checklist">
                <li>不会替你转账、缴费、申购理财或修改密码。</li>
                <li>不会要求你提供短信验证码、登录密码或支付密码。</li>
                <li>风险和理财回答只用于解释，最终以业务页面校验为准。</li>
                <li>发现异常交易时，优先查看风控原因或发起服务工单。</li>
            </ul>
        </aside>
    </section>

    <% if (reply != null) { %>
        <section class="assistant-reply-panel">
            <div class="assistant-reply-head">
                <div>
                    <p class="eyebrow"><%= HtmlUtil.escape(intentName(reply.getIntent())) %></p>
                    <h2><%= HtmlUtil.escape(reply.getTitle()) %></h2>
                    <p class="section-note"><%= HtmlUtil.escape(reply.getSummary()) %></p>
                </div>
                <div class="assistant-source-tag">
                    <% if ("LOCAL_LLM".equals(reply.getAnswerSource())) { %>
                        <span>本地模型增强</span>
                        <strong><%= HtmlUtil.escape(reply.getModelName()) %></strong>
                    <% } else if ("DEEPSEEK".equals(reply.getAnswerSource())) { %>
                        <span>DeepSeek 增强</span>
                        <strong><%= HtmlUtil.escape(reply.getModelName()) %></strong>
                    <% } else { %>
                        <span>本地解释</span>
                        <strong>业务规则</strong>
                    <% } %>
                </div>
            </div>
            <% if (reply.getNarrative() != null && reply.getNarrative().trim().length() > 0) { %>
                <div class="assistant-narrative">
                    <%= HtmlUtil.escape(reply.getNarrative()).replace("\n", "<br>") %>
                </div>
            <% } %>
            <% if (reply.getFallbackNote() != null && reply.getFallbackNote().trim().length() > 0) { %>
                <div class="alert warning"><%= HtmlUtil.escape(reply.getFallbackNote()) %></div>
            <% } %>
            <div class="assistant-answer-list">
                <% List<String> highlights = reply.getHighlights();
                   if (highlights == null || highlights.isEmpty()) { %>
                    <div class="human-empty">
                        <strong>小 i 暂时没有找到可解释的数据。</strong>
                        <p>完成交易、缴费、风险测评或提交工单后，再回到这里提问，会得到更具体的回答。</p>
                    </div>
                <% } else {
                    int index = 1;
                    for (String highlight : highlights) {
                %>
                    <div class="assistant-answer-item">
                        <span><%= index++ %></span>
                        <p><%= HtmlUtil.escape(highlight) %></p>
                    </div>
                <%  }
                } %>
            </div>
            <% if (reply.getSafetyNote() != null && reply.getSafetyNote().length() > 0) { %>
                <div class="security-advice assistant-security-note">
                    <strong>安全提示</strong>
                    <p><%= HtmlUtil.escape(reply.getSafetyNote()) %></p>
                </div>
            <% } %>
            <% List<AssistantAction> actions = reply.getActions();
               if (actions != null && !actions.isEmpty()) { %>
                <div class="assistant-action-grid">
                    <% for (AssistantAction action : actions) { %>
                        <a class="<%= actionClass(action.getStyle()) %>" href="<%= HtmlUtil.escape(actionHref(request.getContextPath(), action.getHref())) %>"><%= HtmlUtil.escape(action.getLabel()) %></a>
                    <% } %>
                </div>
            <% } %>
        </section>
    <% } %>
</main>
</body>
</html>
