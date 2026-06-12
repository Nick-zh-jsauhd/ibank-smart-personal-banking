<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.dto.TransactionResult,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode" %>
<%!
    private String txnTypeName(String txnType) {
        if ("DEPOSIT".equals(txnType)) {
            return "存款";
        }
        if ("WITHDRAW".equals(txnType)) {
            return "取款";
        }
        if ("TRANSFER_INNER".equals(txnType)) {
            return "本行转账";
        }
        if ("PAYMENT".equals(txnType)) {
            return "生活缴费";
        }
        if ("BUY_WEALTH".equals(txnType)) {
            return "理财申购";
        }
        if ("REDEEM_WEALTH".equals(txnType)) {
            return "理财赎回";
        }
        return txnType == null ? "" : txnType;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
%>
<%
    TransactionResult result = (TransactionResult) request.getAttribute("result");
    if (result == null) {
        response.sendRedirect(request.getContextPath() + "/dashboard");
        return;
    }
    boolean income = "IN".equals(result.getDirection());
    boolean freeze = "FREEZE".equals(result.getDirection());
    boolean pendingIn = "PENDING_IN".equals(result.getDirection());
    boolean cashOut = "OUT".equals(result.getDirection()) || freeze;
    boolean pending = !"SUCCESS".equals(result.getStatus());
    boolean wealthTxn = "BUY_WEALTH".equals(result.getTxnType()) || "REDEEM_WEALTH".equals(result.getTxnType());
    request.setAttribute("activeNav", wealthTxn ? "wealth" : "transfer");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>交易结果 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="customer-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout">
    <section class="result-panel">
        <div class="result-status-mark"><%= pending ? "PENDING" : "SUCCESS" %></div>
        <div>
            <p class="eyebrow">Transaction Result</p>
            <h1><%= pending ? "业务已提交，等待后续处理。" : "这笔交易已完成。" %></h1>
            <p class="muted"><%= pending
                    ? "当前只是进入业务处理队列，理财申购需后台确认后生成持仓，赎回需清算到账后才更新账户余额。"
                : "余额已同步更新，交易记录和流水明细已经落账，可在工作台、流水和月账单中继续核对。" %></p>
        </div>
        <strong class="<%= (income || pendingIn) ? "money-in" : "money-out" %>"><%= income ? "+" : (pendingIn ? "预计 +" : (cashOut ? "-" : "")) %> ¥ <%= moneyText(result.getAmount()) %></strong>
    </section>

    <section class="detail-workspace">
        <div class="detail-main">
            <section class="content-section">
                <div class="section-title">
                    <div>
                        <h2>交易详情</h2>
                        <p class="section-note"><%= HtmlUtil.escape(result.getSummary()) %></p>
                    </div>
                    <span class="status"><%= HtmlUtil.escape(result.getStatus()) %></span>
                </div>
                <dl class="detail-list">
                    <div>
                        <dt>交易编号</dt>
                        <dd><%= HtmlUtil.escape(result.getTransactionNo()) %></dd>
                    </div>
                    <div>
                        <dt>交易类型</dt>
                        <dd><%= HtmlUtil.escape(txnTypeName(result.getTxnType())) %></dd>
                    </div>
                    <div>
                        <dt>账户</dt>
                        <dd><%= HtmlUtil.escape(result.getAccountNo()) %></dd>
                    </div>
                    <div>
                        <dt>方向</dt>
                        <dd><span class="direction <%= (income || pendingIn) ? "direction-in" : "direction-out" %>"><%= HtmlUtil.escape(result.getDirection()) %></span></dd>
                    </div>
                    <div>
                        <dt>金额</dt>
                        <dd>¥ <%= moneyText(result.getAmount()) %></dd>
                    </div>
                    <div>
                        <dt><%= pendingIn ? "当前可用余额" : (freeze ? "冻结后可用余额" : "交易后余额") %></dt>
                        <dd>¥ <%= moneyText(result.getBalanceAfter()) %></dd>
                    </div>
                    <div>
                        <dt>状态</dt>
                        <dd><span class="status"><%= HtmlUtil.escape(result.getStatus()) %></span></dd>
                    </div>
                    <div>
                        <dt>摘要</dt>
                        <dd><%= HtmlUtil.escape(result.getSummary()) %></dd>
                    </div>
                </dl>
            </section>
        </div>

        <aside class="detail-side">
            <section class="next-step-panel">
                <h2>下一步</h2>
                <p><%= pending
                        ? "你可以在理财持仓或通知中心查看后续确认、到账进度；资金类结果以清算后的流水为准。"
                        : "如果金额、收款对象或业务类型不符合预期，可以先复制交易编号，再到服务中心发起争议工单。" %></p>
                <div class="quick-link-grid">
                    <a class="quick-link" href="<%= request.getContextPath() %>/dashboard"><strong>工作台</strong><span>查看资产和近期流水</span></a>
                    <a class="quick-link" href="<%= request.getContextPath() %>/transactions"><strong>流水</strong><span>核对交易明细</span></a>
                    <a class="quick-link" href="<%= request.getContextPath() %>/bill/report"><strong>收支报表</strong><span>查看账单分析</span></a>
                    <a class="quick-link" href="<%= request.getContextPath() %>/tickets"><strong>服务</strong><span>发起争议或咨询</span></a>
                </div>
            </section>
            <section class="content-section">
                <h2>继续办理</h2>
                <div class="action-row stacked-actions">
                    <a class="button secondary" href="<%= request.getContextPath() %>/deposit">继续存款</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/withdraw">继续取款</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/transfer">继续转账</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/payment">继续缴费</a>
                    <a class="button secondary" href="<%= request.getContextPath() %>/wealth/holdings">查看持仓</a>
                    <a class="button primary" href="<%= request.getContextPath() %>/dashboard">返回工作台</a>
                </div>
            </section>
        </aside>
    </section>
</main>
</body>
</html>
