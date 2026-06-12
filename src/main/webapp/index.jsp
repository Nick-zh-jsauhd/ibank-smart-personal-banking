<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.util.HtmlUtil" %>
<%
    if (session.getAttribute("loginUser") != null) {
        response.sendRedirect(request.getContextPath() + "/dashboard");
        return;
    }
    Object flash = session.getAttribute("flash");
    if (flash != null) {
        session.removeAttribute("flash");
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>iBank 智能个人银行系统</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="public-page cover-page portal-page" data-portal-index="0">
<header class="portal-nav" aria-label="iBank 首页导航">
    <a class="cover-brand" href="#portal-home" data-portal-jump="portal-home">
        <span class="cover-brand-mark"></span>
        <span>iBank</span>
    </a>
    <nav class="portal-nav-links" aria-label="封面分屏导航">
        <a href="#portal-home" data-portal-jump="portal-home">首页</a>
        <a href="#portal-customer" data-portal-jump="portal-customer">个人银行</a>
        <a href="#portal-admin" data-portal-jump="portal-admin">运营管理</a>
    </nav>
    <div class="cover-nav-actions">
        <a class="button secondary compact" href="#portal-customer" data-portal-jump="portal-customer">个人服务</a>
        <a class="button ghost compact" href="#portal-admin" data-portal-jump="portal-admin">管理入口</a>
    </div>
</header>

<nav class="portal-dots" aria-label="封面页码">
    <button type="button" class="active" data-portal-jump="portal-home"><span>首页</span></button>
    <button type="button" data-portal-jump="portal-customer"><span>个人</span></button>
    <button type="button" data-portal-jump="portal-admin"><span>后台</span></button>
</nav>

<main class="portal-scroll" id="portalScroll">
    <section class="portal-slide portal-slide-home active" id="portal-home" data-portal-section="portal-home">
        <div class="portal-bg" aria-hidden="true"></div>
        <div class="portal-slide-inner portal-hero-layout">
            <div class="portal-copy">
                <p class="eyebrow">IBANK WARM FINANCE</p>
                <h1>让个人金融一屏看清。</h1>
                <p class="cover-lede">账户、流水、月账单、理财、风险提醒和服务进度在这里汇总。先看资金状态，再看今天需要处理什么。</p>
                <div class="portal-hero-actions" aria-label="首页分屏入口">
                    <a href="#portal-customer" data-portal-jump="portal-customer">个人银行服务</a>
                    <a href="#portal-admin" data-portal-jump="portal-admin">运营管理能力</a>
                    <a href="<%= request.getContextPath() %>/register">开通客户账户</a>
                </div>
            </div>

            <aside class="portal-home-note" aria-label="iBank 门户说明">
                <span>今日门户</span>
                <strong>金融不是一堆入口，而是一条能解释、能提醒、能闭环的业务线。</strong>
                <p>需要办理时，再进入对应登录页；先在这里看清服务路径。</p>
            </aside>
        </div>

        <div class="portal-service-rail" aria-label="iBank 核心能力">
            <a href="#portal-customer" data-portal-jump="portal-customer">
                <span>01</span>
                <strong>资产洞察</strong>
                <em>账户余额、现金流和月账单一屏汇总</em>
            </a>
            <a href="#portal-customer" data-portal-jump="portal-customer">
                <span>02</span>
                <strong>生活账务</strong>
                <em>转账、缴费、存取款自动沉淀流水</em>
            </a>
            <a href="#portal-admin" data-portal-jump="portal-admin">
                <span>03</span>
                <strong>风险预警</strong>
                <em>异常交易说明原因和下一步动作</em>
            </a>
            <a href="#portal-admin" data-portal-jump="portal-admin">
                <span>04</span>
                <strong>服务闭环</strong>
                <em>通知、工单、调账和审计串联处理</em>
            </a>
        </div>
    </section>

    <section class="portal-slide portal-slide-customer" id="portal-customer" data-portal-section="portal-customer">
        <div class="portal-bg" aria-hidden="true"></div>
        <div class="portal-slide-inner portal-entry-layout">
            <div class="portal-copy">
                <p class="eyebrow">PERSONAL BANKING</p>
                <h1>登录后先看账户，再看账单和安全提醒。</h1>
                <p class="cover-lede">客户侧以“今天该关注什么”为主线，把资产总览、生活缴费、理财适当性、未读通知和服务工单放到同一个金融工作台。</p>
                <div class="portal-line-list" aria-label="个人银行服务">
                    <span>资金安全：支付密码、交易限额和异常提醒共同守护每一笔钱。</span>
                    <span>生活账务：缴费、转账、存取款完成后自动进入流水和月账单。</span>
                    <span>财富陪伴：风险测评、产品匹配、资金冻结和清算过程清楚可追踪。</span>
                </div>
            </div>

            <section class="portal-entry-panel" aria-label="个人银行登录入口">
                <p class="panel-kicker">个人客户</p>
                <h2>进入个人金融工作台</h2>
                <p>前往独立登录页后，系统会带你进入账户、账单、安全提醒和待办服务的工作台。</p>
                <% if (flash != null) { %>
                    <div class="alert success"><%= HtmlUtil.escape(String.valueOf(flash)) %></div>
                <% } %>
                <div class="portal-panel-actions">
                    <a class="button primary" href="<%= request.getContextPath() %>/login">前往客户登录页</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/register">开通客户账户</a>
                </div>
                <div class="portal-panel-meta">
                    <span>资产工作台</span>
                    <span>月账单</span>
                    <span>服务待办</span>
                </div>
            </section>
        </div>
    </section>

    <section class="portal-slide portal-slide-admin" id="portal-admin" data-portal-section="portal-admin">
        <div class="portal-bg" aria-hidden="true"></div>
        <div class="portal-slide-inner portal-entry-layout">
            <div class="portal-copy">
                <p class="eyebrow">OPERATIONS CONSOLE</p>
                <h1>风险、清算、服务闭环。</h1>
                <p class="cover-lede">管理端围绕待办闭环组织：客户管理、消息通知、风险事件、理财清算、对账异常、调账复核和审计追踪分工处理。</p>
                <div class="portal-line-list compact" aria-label="运营管理能力">
                    <span>风险：查看规则命中、模型评分、图谱解释和处理意见。</span>
                    <span>清算：跟踪理财申购、赎回、收益入账和资金冻结释放。</span>
                    <span>服务：把客户工单、消息待办和业务动作关联成闭环。</span>
                </div>
            </div>

            <section class="portal-entry-panel admin" aria-label="运营后台登录入口">
                <p class="panel-kicker">管理人员</p>
                <h2>进入运营控制台</h2>
                <p>后台登录页保留账号校验和权限控制，封面只提供角色入口，避免把敏感输入暴露在门户首屏。</p>
                <div class="portal-panel-actions">
                    <a class="button primary" href="<%= request.getContextPath() %>/admin/login">前往后台登录页</a>
                    <a class="button secondary" href="#portal-customer" data-portal-jump="portal-customer">查看个人入口</a>
                </div>
                <div class="portal-panel-meta">
                    <span>风控复核</span>
                    <span>理财清算</span>
                    <span>审计追踪</span>
                </div>
            </section>
        </div>
    </section>
</main>

<div class="portal-scroll-cue" aria-hidden="true">
    <span></span>
    <strong>滚动切换</strong>
</div>

<script src="<%= request.getContextPath() %>/assets/js/portal.js"></script>
</body>
</html>
