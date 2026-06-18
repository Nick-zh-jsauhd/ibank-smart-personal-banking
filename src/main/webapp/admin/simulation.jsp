<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.bank.bean.SimulationRun,com.bank.dto.SimulationDashboardView,com.bank.util.HtmlUtil,com.bank.util.StatusDisplayUtil,java.util.Collections,java.util.List" %>
<%!
    private String safe(String value) {
        return HtmlUtil.escape(value == null ? "-" : value);
    }
%>
<%
    SimulationDashboardView view = (SimulationDashboardView) request.getAttribute("view");
    String success = (String) request.getAttribute("success");
    String error = (String) request.getAttribute("error");
    List<SimulationRun> recentRuns = view == null ? Collections.<SimulationRun>emptyList() : view.getRecentRuns();
    SimulationRun selectedRun = view == null ? null : view.getSelectedRun();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>仿真沙盘 - iBank</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
</head>
<body class="admin-page lab-page">
<%@ include file="/WEB-INF/jsp/adminLabTopbar.jspf" %>

<main class="layout layout-wide simulation-page simulation-lab-page">
    <section class="simulation-lab-hero">
        <div class="simulation-lab-copy">
            <p class="eyebrow">仿真沙盘</p>
            <h1>实时金融仿真沙盘</h1>
            <p>把客户现金流、发薪日、风控压力和市场波动变成可启动、可暂停、可回放的连续事件流，用来观察交易、流水、通知和运营处理链路是否真正闭环。</p>
            <div class="simulation-hero-badges">
                <span>真实入账会写入交易流水</span>
                <span>风险与市场先进入演练事件</span>
                <span>所有批次可审计回放</span>
            </div>
        </div>
        <aside class="simulation-live-orb-card">
            <div class="simulation-live-ring" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
            </div>
            <div>
                <span class="small-label">Live Runtime</span>
                <strong id="runtimeHeadline">等待启动</strong>
                <p id="runtimeMessage">选择场景后启动实时仿真，事件会按节奏持续写入。</p>
            </div>
        </aside>
    </section>

    <% if (success != null) { %>
        <div class="alert success"><%= HtmlUtil.escape(success) %></div>
    <% } %>
    <% if (error != null) { %>
        <div class="alert danger"><%= HtmlUtil.escape(error) %></div>
    <% } %>

    <section class="simulation-operator-grid">
        <article class="content-section simulation-command-deck">
            <div class="section-title">
                <div>
                    <h2>运行控制</h2>
                    <p class="section-note">这里控制实时流的节奏。先从小规模开始，确认页面、业务链路和数据库写入稳定后再提高强度。</p>
                </div>
                <span id="runtimeStatusPill" class="tag">未启动</span>
            </div>

            <form id="runtimeForm" class="simulation-live-form">
                <div class="simulation-scenario-grid simulation-scenario-grid-rich">
                    <label class="simulation-scenario-card simulation-scenario-card-rich">
                        <input type="radio" name="scenarioCode" value="DAILY_FLOW" checked>
                        <span>Daily Flow</span>
                        <strong>日常流水回放</strong>
                        <em>小额资金进出，适合观察工作台、流水和月账单变化</em>
                    </label>
                    <label class="simulation-scenario-card simulation-scenario-card-rich">
                        <input type="radio" name="scenarioCode" value="PAYDAY">
                        <span>Payday</span>
                        <strong>发薪日现金流</strong>
                        <em>批量工资入账，适合观察客户资产和通知更新</em>
                    </label>
                    <label class="simulation-scenario-card simulation-scenario-card-rich">
                        <input type="radio" name="scenarioCode" value="RISK_STRESS">
                        <span>Risk Stress</span>
                        <strong>风控压力演练</strong>
                        <em>大额、陌生收款人和高频信号，沉淀预警样本</em>
                    </label>
                    <label class="simulation-scenario-card simulation-scenario-card-rich">
                        <input type="radio" name="scenarioCode" value="MARKET_SWING">
                        <span>Market</span>
                        <strong>市场波动联动</strong>
                        <em>模拟行情冲击，后续可联动理财提醒和适配建议</em>
                    </label>
                </div>

                <div class="simulation-live-controls">
                    <label>
                        <span>目标事件数</span>
                        <input type="number" name="targetEventCount" min="10" max="1000" value="120">
                    </label>
                    <label>
                        <span>每次写入</span>
                        <input type="number" name="eventsPerTick" min="1" max="10" value="2">
                    </label>
                    <label>
                        <span>运行节奏</span>
                        <select name="speed">
                            <option value="NORMAL">标准节奏</option>
                            <option value="SLOW">慢速观察</option>
                            <option value="FAST">快速压测</option>
                        </select>
                    </label>
                </div>

                <div class="simulation-live-actions">
                    <button class="button primary" type="button" id="startRuntimeButton">启动实时流</button>
                    <button class="button secondary" type="button" id="pauseRuntimeButton">暂停</button>
                    <button class="button secondary" type="button" id="resumeRuntimeButton">继续</button>
                    <button class="button secondary" type="button" id="stopRuntimeButton">停止</button>
                </div>
            </form>
        </article>

        <article class="content-section simulation-flow-panel">
            <div class="section-title">
                <div>
                    <h2>流动面板</h2>
                    <p class="section-note">用事件流表达系统正在发生什么，而不是只看一次刷新后的静态表格。</p>
                </div>
            </div>

            <div class="simulation-flow-stage" aria-hidden="true">
                <div class="simulation-flow-track">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
                <div class="simulation-flow-nodes">
                    <div><strong>客户</strong><em>账户池</em></div>
                    <div><strong>交易</strong><em>流水写入</em></div>
                    <div><strong>风控</strong><em>信号识别</em></div>
                    <div><strong>运营</strong><em>看板闭环</em></div>
                </div>
            </div>

            <div class="simulation-progress-panel">
                <div>
                    <span>当前批次</span>
                    <strong id="activeRunCode"><%= selectedRun == null ? "-" : safe(selectedRun.getRunCode()) %></strong>
                </div>
                <div class="simulation-progress-bar">
                    <span id="runtimeProgressFill"></span>
                </div>
                <p id="runtimeProgressText">0 / 0 条事件</p>
            </div>

            <div class="simulation-kpi-strip">
                <div><span>累计批次</span><strong id="totalRuns"><%= view == null ? 0 : view.getTotalRunCount() %></strong></div>
                <div><span>累计事件</span><strong id="totalEvents"><%= view == null ? 0 : view.getTotalEventCount() %></strong></div>
                <div><span>真实交易</span><strong id="totalBusiness"><%= view == null ? 0 : view.getTotalBusinessSuccessCount() %></strong></div>
                <div><span>风险信号</span><strong id="totalRisk"><%= view == null ? 0 : view.getTotalRiskWarningCount() %></strong></div>
            </div>
        </article>
    </section>

    <section class="simulation-observe-grid">
        <article class="content-section simulation-event-board">
            <div class="section-title">
                <div>
                    <h2>实时事件流</h2>
                    <p class="section-note" id="selectedRunSummary">事件会按时间进入这里，最新事件优先显示。</p>
                </div>
                <button class="button secondary compact" type="button" id="refreshRuntimeButton">刷新</button>
            </div>
            <div id="eventStream" class="simulation-live-event-stream">
                <div class="human-empty">
                    <strong>等待事件进入</strong>
                    <p>启动实时流后，真实入账、风控演练和市场波动会逐条出现在这里。</p>
                </div>
            </div>
        </article>

        <aside class="content-section simulation-run-board">
            <div class="section-title">
                <div>
                    <h2>最近批次</h2>
                    <p class="section-note">用于回放和审计每一轮沙盘。</p>
                </div>
            </div>
            <div class="simulation-run-list">
                <% if (recentRuns.isEmpty()) { %>
                    <div class="human-empty">
                        <strong>暂无历史批次</strong>
                        <p>启动第一轮实时仿真后，这里会保留批次号和运行结果。</p>
                    </div>
                <% } else { %>
                    <% for (SimulationRun run : recentRuns) { %>
                        <a class="simulation-run-row" href="<%= request.getContextPath() %>/admin/simulation?runId=<%= run.getRunId() %>">
                            <strong><%= safe(run.getScenarioName()) %></strong>
                            <span><%= safe(run.getRunCode()) %></span>
                            <em><%= HtmlUtil.escape(StatusDisplayUtil.simulationRunStatus(run.getStatus())) %></em>
                        </a>
                    <% } %>
                <% } %>
            </div>
        </aside>
    </section>
</main>

<script>
(function () {
    var apiUrl = '<%= request.getContextPath() %>/admin/simulation/runtime';
    var form = document.getElementById('runtimeForm');
    var pollTimer = null;

    function postAction(action) {
        var params = new URLSearchParams(new FormData(form));
        params.set('action', action);
        return fetch(apiUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
            body: params.toString(),
            credentials: 'same-origin'
        }).then(readJson).then(renderState).catch(showRuntimeError);
    }

    function refreshState() {
        return fetch(apiUrl, {credentials: 'same-origin'})
            .then(readJson)
            .then(renderState)
            .catch(showRuntimeError);
    }

    function readJson(response) {
        if (!response.ok) {
            throw new Error('沙盘接口返回异常');
        }
        return response.json();
    }

    function renderState(data) {
        var runtime = data.runtime || {};
        var totals = data.totals || {};
        var selectedRun = data.selectedRun || {};
        var events = data.events || [];
        var isRunning = !!runtime.running;
        var isPaused = !!runtime.paused;
        var target = Number(runtime.targetEventCount || selectedRun.requestedEventCount || 0);
        var generated = Number(runtime.generatedEventCount || countGenerated(selectedRun));
        var percent = target > 0 ? Math.min(100, Math.round(generated * 100 / target)) : 0;

        text('runtimeHeadline', isRunning ? (isPaused ? '已暂停' : '实时运行中') : '等待启动');
        text('runtimeMessage', runtime.message || data.message || '选择场景后启动实时仿真');
        text('runtimeStatusPill', isRunning ? (isPaused ? '暂停中' : '运行中') : '未启动');
        text('activeRunCode', runtime.runCode || selectedRun.runCode || '-');
        text('runtimeProgressText', generated + ' / ' + target + ' 条事件');
        text('totalRuns', totals.runs || 0);
        text('totalEvents', totals.events || 0);
        text('totalBusiness', totals.businessSuccess || 0);
        text('totalRisk', totals.riskWarnings || 0);
        text('selectedRunSummary', selectedRun.summary || '事件会按时间进入这里，最新事件优先显示');
        document.getElementById('runtimeProgressFill').style.width = percent + '%';

        var pill = document.getElementById('runtimeStatusPill');
        pill.className = 'tag ' + (isRunning ? (isPaused ? 'severity-warning' : 'severity-success') : '');

        document.body.classList.toggle('simulation-runtime-active', isRunning && !isPaused);
        document.getElementById('startRuntimeButton').disabled = isRunning;
        document.getElementById('pauseRuntimeButton').disabled = !isRunning || isPaused;
        document.getElementById('resumeRuntimeButton').disabled = !isRunning || !isPaused;
        document.getElementById('stopRuntimeButton').disabled = !isRunning;

        renderEvents(events);
        ensurePolling(isRunning);
    }

    function renderEvents(events) {
        var container = document.getElementById('eventStream');
        if (!events || events.length === 0) {
            container.innerHTML = '<div class="human-empty"><strong>等待事件进入</strong><p>启动实时流后，真实入账、风控演练和市场波动会逐条出现在这里。</p></div>';
            return;
        }
        var latest = events.slice(-16).reverse();
        container.innerHTML = latest.map(function (event, index) {
            return '<article class="simulation-live-event-card ' + (index === 0 ? 'incoming' : '') + '">' +
                '<div class="simulation-live-event-dot"></div>' +
                '<div class="simulation-live-event-main">' +
                    '<div class="simulation-live-event-head">' +
                        '<strong>' + escapeHtml(event.eventType || '-') + '</strong>' +
                        '<span class="tag ' + statusClass(event.status) + '">' + statusText(event.status) + '</span>' +
                    '</div>' +
                    '<p>' + escapeHtml(event.message || '-') + '</p>' +
                    '<div class="simulation-event-meta">' +
                        '<span>客户 ' + escapeHtml(event.customerName || '-') + '</span>' +
                        '<span>账户 ' + escapeHtml(event.accountNo || '-') + '</span>' +
                        '<span>金额 ¥ ' + escapeHtml(event.amount || '-') + '</span>' +
                        '<span>' + escapeHtml(event.createdAt || '-') + '</span>' +
                    '</div>' +
                '</div>' +
            '</article>';
        }).join('');
    }

    function ensurePolling(active) {
        if (active && !pollTimer) {
            pollTimer = window.setInterval(refreshState, 2000);
        }
        if (!active && pollTimer) {
            window.clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function countGenerated(run) {
        return Number(run.successEventCount || 0) + Number(run.failureEventCount || 0) + Number(run.riskEventCount || 0);
    }

    function statusText(status) {
        if (status === 'SUCCESS') return '成功';
        if (status === 'RECORDED') return '记录';
        if (status === 'WARNING') return '预警';
        if (status === 'FAILED') return '失败';
        if (status === 'COMPLETED') return '完成';
        if (status === 'COMPLETED_WITH_WARNINGS') return '有警告';
        if (status === 'STOPPED') return '停止';
        return status || '-';
    }

    function statusClass(status) {
        if (status === 'SUCCESS' || status === 'RECORDED' || status === 'COMPLETED') return 'severity-success';
        if (status === 'WARNING' || status === 'COMPLETED_WITH_WARNINGS') return 'severity-warning';
        if (status === 'FAILED' || status === 'STOPPED' || status === 'BLOCKED') return 'severity-critical';
        return '';
    }

    function showRuntimeError(error) {
        text('runtimeMessage', error.message || '沙盘状态刷新失败');
    }

    function text(id, value) {
        var el = document.getElementById(id);
        if (el) {
            el.textContent = value == null ? '' : String(value);
        }
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    document.getElementById('startRuntimeButton').addEventListener('click', function () { postAction('start'); });
    document.getElementById('pauseRuntimeButton').addEventListener('click', function () { postAction('pause'); });
    document.getElementById('resumeRuntimeButton').addEventListener('click', function () { postAction('resume'); });
    document.getElementById('stopRuntimeButton').addEventListener('click', function () { postAction('stop'); });
    document.getElementById('refreshRuntimeButton').addEventListener('click', refreshState);
    refreshState();
})();
</script>
</body>
</html>
