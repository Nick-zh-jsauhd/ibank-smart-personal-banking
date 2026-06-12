<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.AdjustmentRequest,com.bank.dto.AdminSessionUser,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.util.List" %>
<%!
    private String moneyText(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String statusName(String status) {
        if ("PENDING_REVIEW".equals(status)) return "待复核";
        if ("APPROVED".equals(status)) return "复核通过";
        if ("REJECTED".equals(status)) return "已驳回";
        if ("EXECUTED".equals(status)) return "已执行";
        if ("FAILED".equals(status)) return "执行失败";
        return status == null ? "" : status;
    }

    private String statusClass(String status) {
        if ("PENDING_REVIEW".equals(status) || "APPROVED".equals(status)) return "tag";
        if ("REJECTED".equals(status) || "FAILED".equals(status)) return "direction direction-out";
        return "status";
    }

    private String directionName(String direction) {
        if ("INCREASE".equals(direction)) return "调增";
        if ("DECREASE".equals(direction)) return "调减";
        return direction == null ? "" : direction;
    }

    private String sourceName(AdjustmentRequest adjustment) {
        if (adjustment != null && "SERVICE_TICKET".equals(adjustment.getSourceType())
                && adjustment.getSourceTicketId() != null) {
            return "服务工单 #" + adjustment.getSourceTicketId();
        }
        if (adjustment != null && adjustment.getReconciliationItemId() != null) {
            String businessId = adjustment.getReconciliationBusinessId() == null ? ""
                    : " " + adjustment.getReconciliationBusinessId();
            return "对账异常 #" + adjustment.getReconciliationItemId() + businessId;
        }
        return "未记录";
    }

    private String timeText(java.sql.Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }
%>
<%
    AdminSessionUser adminUser = (AdminSessionUser) session.getAttribute("adminUser");
    List<AdjustmentRequest> adjustments = (List<AdjustmentRequest>) request.getAttribute("adjustments");
    String error = (String) request.getAttribute("error");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>调账申请 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page">
<%@ include file="/WEB-INF/jsp/adminTopbar.jspf" %>

<main class="layout">
    <section class="page-heading">
        <p class="eyebrow">Adjustment Requests</p>
        <h1>调账申请</h1>
        <p class="muted">查看调账申请、复核结论和执行状态。资金修正必须先申请、再复核、后执行。</p>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="content-section filter-section">
        <form method="get" action="<%= request.getContextPath() %>/admin/adjustments" class="filter-form two-col">
            <label>
                <span>申请状态</span>
                <select name="status">
                    <option value="">全部</option>
                    <option value="PENDING_REVIEW" <%= "PENDING_REVIEW".equals(selectedStatus) ? "selected" : "" %>>待复核</option>
                    <option value="APPROVED" <%= "APPROVED".equals(selectedStatus) ? "selected" : "" %>>复核通过</option>
                    <option value="REJECTED" <%= "REJECTED".equals(selectedStatus) ? "selected" : "" %>>已驳回</option>
                    <option value="EXECUTED" <%= "EXECUTED".equals(selectedStatus) ? "selected" : "" %>>已执行</option>
                    <option value="FAILED" <%= "FAILED".equals(selectedStatus) ? "selected" : "" %>>执行失败</option>
                </select>
            </label>
            <button class="button primary" type="submit">查询</button>
        </form>
    </section>

    <section class="content-section">
        <div class="section-title">
            <div>
                <h2>申请列表</h2>
                <p class="section-note">默认最多展示最近 100 条调账申请。</p>
            </div>
        </div>
        <div class="table-wrap">
            <table class="wide-table admin-product-table">
                <thead>
                <tr>
                    <th>申请号</th>
                    <th>状态</th>
                    <th>客户</th>
                    <th>账户</th>
                    <th>方向</th>
                    <th>金额</th>
                    <th>来源</th>
                    <th>申请人</th>
                    <th>复核人</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% if (adjustments == null || adjustments.isEmpty()) { %>
                    <tr><td colspan="11" class="empty">暂无调账申请</td></tr>
                <% } else {
                    for (AdjustmentRequest adjustment : adjustments) {
                %>
                    <tr>
                        <td><strong><%= HtmlUtil.escape(adjustment.getAdjustmentNo()) %></strong></td>
                        <td><span class="<%= statusClass(adjustment.getStatus()) %>"><%= statusName(adjustment.getStatus()) %></span></td>
                        <td><%= HtmlUtil.escape(adjustment.getCustomerName()) %></td>
                        <td><%= HtmlUtil.escape(adjustment.getAccountNo()) %></td>
                        <td><%= directionName(adjustment.getDirection()) %></td>
                        <td>¥ <%= moneyText(adjustment.getAmount()) %></td>
                        <td><%= HtmlUtil.escape(sourceName(adjustment)) %></td>
                        <td><%= HtmlUtil.escape(adjustment.getApplicantUsername()) %></td>
                        <td><%= adjustment.getReviewerUsername() == null ? "未复核" : HtmlUtil.escape(adjustment.getReviewerUsername()) %></td>
                        <td><%= timeText(adjustment.getCreatedAt()) %></td>
                        <td>
                            <a class="button secondary compact"
                               href="<%= request.getContextPath() %>/admin/adjustment/detail?adjustmentId=<%= adjustment.getAdjustmentId() %>">详情</a>
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

