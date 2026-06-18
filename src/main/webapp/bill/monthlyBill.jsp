<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.Account,com.bank.dto.BillReportQuery,com.bank.dto.BillReportSummary,com.bank.dto.BillReportView,com.bank.dto.CategorySummary,com.bank.dto.LedgerEntryView,com.bank.dto.SessionUser,com.bank.dto.TimeBucketSummary,com.bank.util.HtmlUtil,java.math.BigDecimal,java.math.RoundingMode,java.net.URLEncoder,java.sql.Timestamp,java.util.List" %>
<%!
    private String txnTypeName(String txnType) {
        if ("DEPOSIT".equals(txnType)) return "存款";
        if ("WITHDRAW".equals(txnType)) return "取款";
        if ("TRANSFER_INNER".equals(txnType)) return "本行转账";
        if ("PAYMENT".equals(txnType)) return "生活缴费";
        if ("BUY_WEALTH".equals(txnType)) return "理财申购";
        if ("REDEEM_WEALTH".equals(txnType)) return "理财赎回";
        return txnType == null ? "其他交易" : txnType;
    }

    private String directionName(String direction) {
        if ("IN".equals(direction)) return "收入";
        if ("OUT".equals(direction)) return "支出";
        return direction == null ? "" : direction;
    }

    private String moneyText(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String timeText(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toString().substring(0, 19);
    }

    private int percent(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int value = amount.multiply(new BigDecimal("100")).divide(total, 0, RoundingMode.HALF_UP).intValue();
        if (value > 100) return 100;
        return value < 4 ? 4 : value;
    }

    private BigDecimal maxBucketAmount(List<TimeBucketSummary> buckets) {
        BigDecimal max = BigDecimal.ZERO.setScale(2);
        if (buckets == null) {
            return max;
        }
        for (TimeBucketSummary bucket : buckets) {
            if (bucket.getTotalIncome() != null && bucket.getTotalIncome().compareTo(max) > 0) {
                max = bucket.getTotalIncome();
            }
            if (bucket.getTotalExpense() != null && bucket.getTotalExpense().compareTo(max) > 0) {
                max = bucket.getTotalExpense();
            }
        }
        return max;
    }

    private BigDecimal maxCategoryAmount(List<CategorySummary> categories, String direction) {
        BigDecimal max = BigDecimal.ZERO.setScale(2);
        if (categories == null) {
            return max;
        }
        for (CategorySummary category : categories) {
            if (direction.equals(category.getDirection())
                    && category.getTotalAmount() != null
                    && category.getTotalAmount().compareTo(max) > 0) {
                max = category.getTotalAmount();
            }
        }
        return max;
    }

    private String selected(String left, String right) {
        if (left == null) {
            return right == null || right.length() == 0 ? "selected" : "";
        }
        return left.equals(right) ? "selected" : "";
    }

    private String enc(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private void appendParam(StringBuilder builder, String name, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        if (builder.length() == 0) {
            builder.append('?');
        } else {
            builder.append('&');
        }
        builder.append(name).append('=').append(enc(value));
    }

    private String buildQuery(BillReportQuery query, String overridePeriodType) {
        StringBuilder builder = new StringBuilder();
        appendParam(builder, "periodType", overridePeriodType == null ? query.getPeriodType() : overridePeriodType);
        appendParam(builder, "date", query.getDate());
        appendParam(builder, "yearMonth", query.getYearMonth());
        appendParam(builder, "year", query.getYear());
        appendParam(builder, "direction", query.getDirection());
        appendParam(builder, "txnType", query.getTxnType());
        if (query.getAccountId() != null) {
            appendParam(builder, "accountId", String.valueOf(query.getAccountId()));
        }
        return builder.toString();
    }
%>
<%
    SessionUser loginUser = (SessionUser) session.getAttribute("loginUser");
    List<Account> accounts = (List<Account>) request.getAttribute("accounts");
    BillReportQuery query = (BillReportQuery) request.getAttribute("query");
    if (query == null) {
        query = new BillReportQuery();
    }
    BillReportView report = (BillReportView) request.getAttribute("report");
    BillReportSummary summary = report == null ? new BillReportSummary() : report.getSummary();
    String error = (String) request.getAttribute("error");
    String periodType = query.getPeriodType() == null ? BillReportQuery.PERIOD_MONTH : query.getPeriodType();
    boolean dayPeriod = BillReportQuery.PERIOD_DAY.equals(periodType);
    boolean yearPeriod = BillReportQuery.PERIOD_YEAR.equals(periodType);
    BigDecimal maxBucket = report == null ? BigDecimal.ZERO : maxBucketAmount(report.getTimeBuckets());
    BigDecimal maxIncomeCategory = report == null ? BigDecimal.ZERO : maxCategoryAmount(report.getCategories(), "IN");
    BigDecimal maxExpenseCategory = report == null ? BigDecimal.ZERO : maxCategoryAmount(report.getCategories(), "OUT");
    String exportUrl = request.getContextPath() + "/bill/export" + buildQuery(query, null);
    request.setAttribute("activeNav", "bill");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>收支分析报表 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css?v=report-layout-20260615a">
</head>
<body class="customer-page report-page">
<%@ include file="/WEB-INF/jsp/customerTopbar.jspf" %>

<main class="layout report-layout">
    <section class="compact-page-head report-page-head">
        <div class="page-heading">
            <p class="eyebrow">收支分析</p>
            <h1>收支分析报表</h1>
            <p class="muted">按日、月、年复盘账户现金流，导出明细用于对账，打印报表用于留存。</p>
        </div>
        <div class="report-actions no-print">
            <button class="button secondary compact" type="button" onclick="window.print()">打印报表</button>
            <a class="button secondary compact" href="<%= exportUrl %>">导出 CSV</a>
            <a class="button primary compact" href="<%= request.getContextPath() %>/transactions">查看流水</a>
        </div>
    </section>

    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="report-tabs no-print" aria-label="账单周期">
        <a class="<%= dayPeriod ? "active" : "" %>" href="<%= request.getContextPath() %>/bill/report<%= buildQuery(query, BillReportQuery.PERIOD_DAY) %>">日账单</a>
        <a class="<%= (!dayPeriod && !yearPeriod) ? "active" : "" %>" href="<%= request.getContextPath() %>/bill/report<%= buildQuery(query, BillReportQuery.PERIOD_MONTH) %>">月账单</a>
        <a class="<%= yearPeriod ? "active" : "" %>" href="<%= request.getContextPath() %>/bill/report<%= buildQuery(query, BillReportQuery.PERIOD_YEAR) %>">年账单</a>
    </section>

    <section class="content-section filter-section no-print">
        <form method="get" action="<%= request.getContextPath() %>/bill/report" class="filter-form report-filter">
            <input type="hidden" name="periodType" value="<%= HtmlUtil.escape(periodType) %>">
            <% if (dayPeriod) { %>
                <label>
                    <span>日期</span>
                    <input name="date" type="date" value="<%= HtmlUtil.escape(query.getDate()) %>">
                </label>
            <% } else if (yearPeriod) { %>
                <label>
                    <span>年份</span>
                    <input name="year" type="number" min="2000" max="2100" value="<%= HtmlUtil.escape(query.getYear()) %>">
                </label>
            <% } else { %>
                <label>
                    <span>月份</span>
                    <input name="yearMonth" type="month" value="<%= HtmlUtil.escape(query.getYearMonth()) %>">
                </label>
            <% } %>
            <label>
                <span>账户</span>
                <select name="accountId">
                    <option value="">全部账户</option>
                    <% if (accounts != null) {
                        for (Account account : accounts) {
                            boolean accountSelected = query.getAccountId() != null && query.getAccountId().equals(account.getAccountId());
                    %>
                        <option value="<%= account.getAccountId() %>" <%= accountSelected ? "selected" : "" %>><%= HtmlUtil.escape(account.getAccountNo()) %></option>
                    <%  }
                    } %>
                </select>
            </label>
            <label>
                <span>方向</span>
                <select name="direction">
                    <option value="">全部方向</option>
                    <option value="IN" <%= selected(query.getDirection(), "IN") %>>收入</option>
                    <option value="OUT" <%= selected(query.getDirection(), "OUT") %>>支出</option>
                </select>
            </label>
            <label>
                <span>交易类型</span>
                <select name="txnType">
                    <option value="">全部类型</option>
                    <option value="DEPOSIT" <%= selected(query.getTxnType(), "DEPOSIT") %>>存款</option>
                    <option value="WITHDRAW" <%= selected(query.getTxnType(), "WITHDRAW") %>>取款</option>
                    <option value="TRANSFER_INNER" <%= selected(query.getTxnType(), "TRANSFER_INNER") %>>本行转账</option>
                    <option value="PAYMENT" <%= selected(query.getTxnType(), "PAYMENT") %>>生活缴费</option>
                    <option value="BUY_WEALTH" <%= selected(query.getTxnType(), "BUY_WEALTH") %>>理财申购</option>
                    <option value="REDEEM_WEALTH" <%= selected(query.getTxnType(), "REDEEM_WEALTH") %>>理财赎回</option>
                </select>
            </label>
            <button class="button primary" type="submit">生成报表</button>
        </form>
    </section>

    <% if (report != null) { %>
        <section class="print-report-cover">
            <div>
                <strong>iBank 智能个人银行系统</strong>
                <h1>收支分析报表</h1>
            </div>
            <dl>
                <div><dt>客户</dt><dd><%= loginUser == null ? "" : HtmlUtil.escape(loginUser.getFullName()) %></dd></div>
                <div><dt>账户范围</dt><dd><%= HtmlUtil.escape(report.getAccountScope()) %></dd></div>
                <div><dt>报表周期</dt><dd><%= HtmlUtil.escape(report.getPeriodLabel()) %></dd></div>
                <div><dt>生成时间</dt><dd><%= timeText(new Timestamp(System.currentTimeMillis())) %></dd></div>
            </dl>
        </section>

        <section class="report-hero content-section">
            <div>
                <p class="eyebrow"><%= HtmlUtil.escape(report.getPeriodLabel()) %></p>
                <h2>这份报表先看现金流方向，再看钱花在了哪里。</h2>
                <p class="section-note"><%= HtmlUtil.escape(report.getInsightText()) %></p>
            </div>
            <div class="report-period-card">
                <span>账户范围</span>
                <strong><%= HtmlUtil.escape(report.getAccountScope()) %></strong>
                <em>明细上限 <%= report.getEntries() == null ? 0 : report.getEntries().size() %> 条</em>
            </div>
        </section>

        <section class="metric-grid report-metric-grid">
            <article class="metric-card">
                <span>总收入</span>
                <strong class="money-in">¥ <%= moneyText(summary.getTotalIncome()) %></strong>
                <p><%= summary.getIncomeCount() %> 笔收入流水</p>
            </article>
            <article class="metric-card">
                <span>总支出</span>
                <strong class="money-out">¥ <%= moneyText(summary.getTotalExpense()) %></strong>
                <p><%= summary.getExpenseCount() %> 笔支出流水</p>
            </article>
            <article class="metric-card">
                <span>净流入</span>
                <strong>¥ <%= moneyText(summary.getNetIncome()) %></strong>
                <p>储蓄率 <%= moneyText(summary.getSavingRate()) %>%</p>
            </article>
            <article class="metric-card">
                <span>最大支出</span>
                <strong>¥ <%= moneyText(summary.getLargestExpense()) %></strong>
                <p>优先核对大额资金流出</p>
            </article>
        </section>

        <section class="report-analysis-grid">
            <div class="report-analysis-primary">
            <article class="content-section report-chart-card report-card-trend">
                <div class="section-title">
                    <div>
                        <h2>收支趋势</h2>
                        <p class="section-note"><%= dayPeriod ? "按小时聚合当天交易。" : (yearPeriod ? "按月份展示年度现金流。" : "按自然日展示本月现金流。") %></p>
                    </div>
                    <div class="report-legend">
                        <span class="legend-income">收入</span>
                        <span class="legend-expense">支出</span>
                    </div>
                </div>
                <div class="report-flow-board <%= dayPeriod ? "report-flow-day" : (yearPeriod ? "report-flow-year" : "report-flow-month") %>">
                    <div class="report-flow-scroll" aria-label="收支趋势，可横向滑动查看更多时间点">
                        <% List<TimeBucketSummary> timeBuckets = report.getTimeBuckets();
                        if (timeBuckets != null) {
                            for (TimeBucketSummary bucket : timeBuckets) {
                            BigDecimal incomeAmount = bucket.getTotalIncome() == null ? BigDecimal.ZERO : bucket.getTotalIncome();
                            BigDecimal expenseAmount = bucket.getTotalExpense() == null ? BigDecimal.ZERO : bucket.getTotalExpense();
                            BigDecimal netAmount = incomeAmount.subtract(expenseAmount);
                            boolean netIncome = netAmount.compareTo(BigDecimal.ZERO) >= 0;
                        %>
                            <article class="report-flow-point <%= netIncome ? "net-in" : "net-out" %>"
                                     title="<%= HtmlUtil.escape(bucket.getBucketLabel()) %> 收入 ¥<%= moneyText(incomeAmount) %>，支出 ¥<%= moneyText(expenseAmount) %>">
                                <div class="flow-label">
                                    <strong><%= HtmlUtil.escape(bucket.getBucketLabel()) %></strong>
                                    <span><%= netIncome ? "净流入" : "净流出" %> ¥ <%= moneyText(netAmount.abs()) %></span>
                                </div>
                                <div class="flow-rails" aria-hidden="true">
                                    <div class="flow-rail income"><span style="width:<%= percent(incomeAmount, maxBucket) %>%"></span></div>
                                    <div class="flow-rail expense"><span style="width:<%= percent(expenseAmount, maxBucket) %>%"></span></div>
                                </div>
                                <div class="flow-amounts">
                                    <span class="money-in">收 ¥ <%= moneyText(incomeAmount) %></span>
                                    <span class="money-out">支 ¥ <%= moneyText(expenseAmount) %></span>
                                </div>
                            </article>
                        <%  }
                        } %>
                    </div>
                </div>
                <p class="report-chart-hint no-print">横向滑动查看更多时间点，悬停可查看该时点收支明细</p>
            </article>

            <article class="content-section report-card-movements">
                <div class="section-title">
                    <div>
                        <h2>关键资金流动</h2>
                        <p class="section-note">按金额排序，优先展示最需要核对的交易。</p>
                    </div>
                </div>
                <div class="movement-list report-movement-list">
                    <% if (report.getTopMovements() == null || report.getTopMovements().isEmpty()) { %>
                        <div class="human-empty">
                            <strong>暂无关键资金流动。</strong>
                            <p>当前周期没有可展示的流水，报表会在交易落账后自动更新。</p>
                        </div>
                    <% } else {
                        for (LedgerEntryView movement : report.getTopMovements()) {
                            boolean income = "IN".equals(movement.getDirection());
                    %>
                        <div class="movement-item report-movement-item">
                            <div>
                                <strong><%= HtmlUtil.escape(txnTypeName(movement.getTxnType())) %></strong>
                                <span><%= timeText(movement.getCreatedAt()) %> · <%= HtmlUtil.escape(movement.getSummary()) %></span>
                            </div>
                            <em class="<%= income ? "money-in" : "money-out" %>"><%= income ? "+" : "-" %>¥ <%= moneyText(movement.getAmount()) %></em>
                        </div>
                    <%  }
                    } %>
                </div>
            </article>
            </div>

            <aside class="report-analysis-side">
            <article class="content-section report-card-category">
                <div class="section-title">
                    <div>
                        <h2>分类分析</h2>
                        <p class="section-note">按交易类型和收支方向聚合，帮助定位主要资金流向。</p>
                    </div>
                </div>
                <div class="category-list report-category-list">
                    <% if (report.getCategories() == null || report.getCategories().isEmpty()) { %>
                        <div class="human-empty">
                            <strong>这个周期还没有形成分类统计。</strong>
                            <p>完成转账、缴费、存取款或理财交易后，系统会自动把流水归入对应类别。</p>
                        </div>
                    <% } else {
                        for (CategorySummary category : report.getCategories()) {
                            BigDecimal categoryBase = "IN".equals(category.getDirection()) ? maxIncomeCategory : maxExpenseCategory;
                    %>
                        <div class="category-item">
                            <div class="category-head">
                                <span><%= HtmlUtil.escape(txnTypeName(category.getTxnType())) %> · <%= HtmlUtil.escape(directionName(category.getDirection())) %></span>
                                <strong>¥ <%= moneyText(category.getTotalAmount()) %></strong>
                            </div>
                            <div class="cashflow-track"><div class="cashflow-fill <%= "IN".equals(category.getDirection()) ? "income" : "expense" %>" style="width: <%= percent(category.getTotalAmount(), categoryBase) %>%"></div></div>
                            <small><%= category.getEntryCount() %> 笔</small>
                        </div>
                    <%  }
                    } %>
                </div>
            </article>

            <section class="next-step-panel report-next-step">
                <strong>下一步建议</strong>
                <p><%= HtmlUtil.escape(report.getNextStepText()) %></p>
                <ul class="task-checklist">
                    <li>导出 CSV 后可以用 Excel 做进一步对账。</li>
                    <li>打印报表会自动隐藏导航和按钮，保留账单摘要与明细。</li>
                    <li>如果发现异常大额支出，请进入安全中心或服务中心处理。</li>
                </ul>
            </section>
            </aside>
        </section>

        <section class="content-section report-detail-section">
            <div class="section-title">
                <div>
                    <h2>账单明细</h2>
                    <p class="section-note">明细和交易流水保持一致；导出时最多包含 10000 条当前筛选结果。</p>
                </div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>时间</th>
                        <th>交易编号</th>
                        <th>账户</th>
                        <th>类型</th>
                        <th>方向</th>
                        <th>金额</th>
                        <th>交易后余额</th>
                        <th>状态</th>
                        <th>摘要</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% if (report.getEntries() == null || report.getEntries().isEmpty()) { %>
                        <tr><td colspan="9" class="empty">这个周期还没有相关流水，完成交易后会自动汇总到这里。</td></tr>
                    <% } else {
                        for (LedgerEntryView entry : report.getEntries()) {
                            boolean income = "IN".equals(entry.getDirection());
                    %>
                        <tr>
                            <td><%= timeText(entry.getCreatedAt()) %></td>
                            <td><%= HtmlUtil.escape(entry.getTransactionNo()) %></td>
                            <td><%= HtmlUtil.escape(entry.getAccountNo()) %></td>
                            <td><%= HtmlUtil.escape(txnTypeName(entry.getTxnType())) %></td>
                            <td><span class="direction <%= income ? "direction-in" : "direction-out" %>"><%= income ? "收入" : "支出" %></span></td>
                            <td>¥ <%= moneyText(entry.getAmount()) %></td>
                            <td>¥ <%= moneyText(entry.getBalanceAfter()) %></td>
                            <td><span class="status"><%= HtmlUtil.escape(entry.getStatus()) %></span></td>
                            <td><%= HtmlUtil.escape(entry.getSummary()) %></td>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>
            <p class="print-disclaimer">本报表仅用于客户账户查询和个人财务复盘，最终账务结果以 iBank 核心账务记录为准。</p>
        </section>
    <% } %>
</main>
</body>
</html>
