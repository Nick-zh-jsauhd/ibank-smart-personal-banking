package com.bank.service.impl;

import com.bank.bean.SimulationEvent;
import com.bank.bean.SimulationRun;
import com.bank.dao.AdminAuditLogDao;
import com.bank.dao.SimulationDao;
import com.bank.dao.impl.AdminAuditLogDaoImpl;
import com.bank.dao.impl.SimulationDaoImpl;
import com.bank.dto.ServiceResult;
import com.bank.dto.SimulationAccountCandidate;
import com.bank.dto.SimulationDashboardView;
import com.bank.dto.TransactionResult;
import com.bank.service.SimulationService;
import com.bank.service.TransactionService;
import com.bank.util.DBUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

public class SimulationServiceImpl implements SimulationService {
    private static final int MAX_MANUAL_EVENT_COUNT = 50;
    private static final int MAX_AUTO_TARGET_COUNT = 1000;
    private static final int RECENT_RUN_LIMIT = 12;
    private static final int RECENT_EVENT_LIMIT = 60;

    private final SimulationDao simulationDao = new SimulationDaoImpl();
    private final TransactionService transactionService = new TransactionServiceImpl();
    private final AdminAuditLogDao adminAuditLogDao = new AdminAuditLogDaoImpl();
    private final Random random = new Random();

    @Override
    public ServiceResult<SimulationDashboardView> dashboard(Long selectedRunId) {
        try (Connection connection = DBUtil.getConnection()) {
            simulationDao.ensureSchema(connection);
            SimulationDashboardView view = new SimulationDashboardView();
            view.setTotalRunCount(simulationDao.countRuns(connection));
            view.setTotalEventCount(simulationDao.countEvents(connection));
            view.setTotalBusinessSuccessCount(simulationDao.countBusinessSuccessEvents(connection));
            view.setTotalRiskWarningCount(simulationDao.countRiskWarningEvents(connection));
            view.setLatestRun(simulationDao.findLatestRun(connection));
            view.setRecentRuns(simulationDao.findRecentRuns(connection, RECENT_RUN_LIMIT));
            view.setLatestEvents(simulationDao.findRecentEvents(connection, RECENT_EVENT_LIMIT));
            if (selectedRunId != null && selectedRunId.longValue() > 0) {
                view.setSelectedRun(simulationDao.findRunById(connection, selectedRunId.longValue()));
                view.setSelectedRunEvents(simulationDao.findEventsByRun(connection, selectedRunId.longValue(),
                        RECENT_EVENT_LIMIT));
            } else if (view.getLatestRun() != null) {
                view.setSelectedRun(view.getLatestRun());
                view.setSelectedRunEvents(simulationDao.findEventsByRun(connection,
                        view.getLatestRun().getRunId(), RECENT_EVENT_LIMIT));
            }
            return ServiceResult.success("仿真沙盘数据加载成功", view);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("仿真沙盘数据加载失败，请检查数据库状态");
        }
    }

    @Override
    public ServiceResult<SimulationRun> runOnce(long adminUserId, String scenarioCode, int eventCount,
                                                String speed, String ipAddress) {
        int normalizedEventCount = Math.min(normalizePositive(eventCount, 10), MAX_MANUAL_EVENT_COUNT);
        ServiceResult<SimulationRun> created = createRun(adminUserId, scenarioCode, normalizedEventCount,
                speed, "MANUAL", ipAddress);
        if (!created.isSuccess()) {
            return created;
        }
        return appendEvents(created.getData().getRunId(), normalizedEventCount, ipAddress, true);
    }

    @Override
    public ServiceResult<SimulationRun> createRun(long adminUserId, String scenarioCode, int requestedEventCount,
                                                  String speed, String mode, String ipAddress) {
        String normalizedScenario = normalizeScenario(scenarioCode);
        if (normalizedScenario == null) {
            return ServiceResult.failure("请选择有效的仿真场景");
        }
        int normalizedTarget = Math.min(normalizePositive(requestedEventCount, 120), MAX_AUTO_TARGET_COUNT);
        String normalizedSpeed = normalizeSpeed(speed);
        String normalizedMode = "AUTO".equals(mode) ? "AUTO" : "MANUAL";

        try (Connection connection = DBUtil.getConnection()) {
            simulationDao.ensureSchema(connection);
            if (simulationDao.findAccountCandidates(connection, 1).isEmpty()) {
                return ServiceResult.failure("当前没有可用于仿真的正常客户账户，请先创建客户和账户数据");
            }

            SimulationRun run = new SimulationRun();
            run.setRunCode("SIM-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000)));
            run.setScenarioCode(normalizedScenario);
            run.setScenarioName(scenarioName(normalizedScenario));
            run.setStatus("RUNNING");
            run.setSpeed(normalizedSpeed);
            run.setRequestedEventCount(normalizedTarget);
            run.setSuccessEventCount(0);
            run.setFailureEventCount(0);
            run.setRiskEventCount(0);
            run.setAdminUserId(adminUserId);
            run.setConfigJson("{\"mode\":\"" + normalizedMode + "\",\"target\":" + normalizedTarget
                    + ",\"speed\":\"" + jsonEscape(normalizedSpeed) + "\"}");
            run.setSummary("仿真批次已启动，事件流正在等待写入");
            long runId = simulationDao.insertRun(connection, run);
            run.setRunId(runId);
            adminAuditLogDao.insert(connection, adminUserId, "SIMULATION_RUN", "SIMULATION",
                    run.getRunCode(), "启动" + normalizedMode + "仿真：" + run.getScenarioName()
                            + "，目标事件数 " + normalizedTarget, ipAddress);
            return ServiceResult.success("仿真批次已创建", run);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("仿真批次创建失败，请检查数据库状态");
        }
    }

    @Override
    public ServiceResult<SimulationRun> appendEvents(long runId, int eventCount, String ipAddress,
                                                     boolean completeWhenTargetReached) {
        int normalizedEventCount = Math.min(normalizePositive(eventCount, 1), MAX_MANUAL_EVENT_COUNT);
        try (Connection connection = DBUtil.getConnection()) {
            simulationDao.ensureSchema(connection);
            SimulationRun run = simulationDao.findRunById(connection, runId);
            if (run == null) {
                return ServiceResult.failure("仿真批次不存在");
            }
            int currentTotal = simulationDao.countEventsByRun(connection, runId);
            int target = normalizePositive(run.getRequestedEventCount(), currentTotal + normalizedEventCount);
            if (currentTotal >= target) {
                return updateRunSummary(connection, run, true, "仿真批次已达到目标事件数");
            }

            int toGenerate = Math.min(normalizedEventCount, target - currentTotal);
            int startSequence = simulationDao.maxEventSequence(connection, runId) + 1;
            List<SimulationAccountCandidate> candidates = simulationDao.findAccountCandidates(connection,
                    Math.max(20, toGenerate * 2));
            if (candidates == null || candidates.isEmpty()) {
                return ServiceResult.failure("当前没有可用于仿真的正常客户账户");
            }

            for (int i = 0; i < toGenerate; i++) {
                SimulationAccountCandidate candidate = candidates.get(random.nextInt(candidates.size()));
                SimulationEvent event;
                try {
                    event = executeScenario(run, startSequence + i, candidate, ipAddress);
                } catch (Exception e) {
                    event = failureEvent(run, startSequence + i, candidate,
                            "仿真事件执行失败：" + safeMessage(e.getMessage()));
                }
                simulationDao.insertEvent(connection, event);
            }

            boolean reachedTarget = currentTotal + toGenerate >= target;
            return updateRunSummary(connection, run, completeWhenTargetReached && reachedTarget,
                    reachedTarget ? "仿真批次已达到目标事件数" : null);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("仿真事件写入失败，请检查数据库状态");
        }
    }

    @Override
    public ServiceResult<SimulationRun> finishRun(long runId, String status, String summary, String ipAddress) {
        try (Connection connection = DBUtil.getConnection()) {
            simulationDao.ensureSchema(connection);
            SimulationRun run = simulationDao.findRunById(connection, runId);
            if (run == null) {
                return ServiceResult.failure("仿真批次不存在");
            }
            run.setStatus(status == null || status.trim().length() == 0 ? "STOPPED" : status.trim());
            run.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            applyRunCounters(connection, run);
            run.setSummary(summary == null || summary.trim().length() == 0
                    ? "仿真批次已停止" : summary.trim());
            simulationDao.updateRunResult(connection, run);
            if (run.getAdminUserId() != null) {
                adminAuditLogDao.insert(connection, run.getAdminUserId(), "SIMULATION_STOP", "SIMULATION",
                        run.getRunCode(), run.getSummary(), ipAddress);
            }
            return ServiceResult.success("仿真批次已结束", run);
        } catch (SQLException e) {
            e.printStackTrace();
            return ServiceResult.failure("仿真批次结束失败，请检查数据库状态");
        }
    }

    private ServiceResult<SimulationRun> updateRunSummary(Connection connection, SimulationRun run,
                                                          boolean complete, String customSummary)
            throws SQLException {
        applyRunCounters(connection, run);
        int total = simulationDao.countEventsByRun(connection, run.getRunId());
        int target = normalizePositive(run.getRequestedEventCount(), total);
        if (complete) {
            run.setStatus(run.getFailureEventCount() == 0 ? "COMPLETED" : "COMPLETED_WITH_WARNINGS");
            run.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            run.setSummary(customSummary == null
                    ? "完成 " + total + " 条仿真事件，其中业务写入/记录 " + run.getSuccessEventCount()
                    + " 条，风险演练 " + run.getRiskEventCount() + " 条，失败 "
                    + run.getFailureEventCount() + " 条"
                    : customSummary);
        } else {
            run.setStatus("RUNNING");
            run.setCompletedAt(null);
            run.setSummary("自动仿真运行中：已生成 " + total + " / " + target + " 条事件，风险信号 "
                    + run.getRiskEventCount() + " 条");
        }
        simulationDao.updateRunResult(connection, run);
        return ServiceResult.success(complete ? "仿真批次执行完成" : "仿真事件已追加", run);
    }

    private void applyRunCounters(Connection connection, SimulationRun run) throws SQLException {
        run.setSuccessEventCount(simulationDao.countSuccessfulEventsByRun(connection, run.getRunId()));
        run.setFailureEventCount(simulationDao.countFailedEventsByRun(connection, run.getRunId()));
        run.setRiskEventCount(simulationDao.countRiskWarningEventsByRun(connection, run.getRunId()));
    }

    private SimulationEvent executeScenario(SimulationRun run, int sequence, SimulationAccountCandidate candidate,
                                            String ipAddress) {
        if ("PAYDAY".equals(run.getScenarioCode())) {
            return depositEvent(run, sequence, candidate, randomAmount(4800, 28000),
                    "工资入账仿真", "SALARY_INFLOW", ipAddress);
        }
        if ("RISK_STRESS".equals(run.getScenarioCode())) {
            if (sequence % 3 == 0) {
                return depositEvent(run, sequence, candidate, randomAmount(30000, 180000),
                        "大额入账压力演练", "LARGE_INFLOW", ipAddress);
            }
            return riskProbeEvent(run, sequence, candidate);
        }
        if ("MARKET_SWING".equals(run.getScenarioCode())) {
            return marketEvent(run, sequence, candidate);
        }
        return depositEvent(run, sequence, candidate, randomAmount(80, 3200),
                "日常现金流仿真", "DAILY_INFLOW", ipAddress);
    }

    private SimulationEvent depositEvent(SimulationRun run, int sequence, SimulationAccountCandidate candidate,
                                         BigDecimal amount, String remark, String eventType, String ipAddress) {
        ServiceResult<TransactionResult> result = transactionService.deposit(candidate.getCustomerId(),
                candidate.getUserId(), candidate.getAccountId(), amount.toPlainString(),
                remark + " - " + run.getRunCode(), ipAddress);
        SimulationEvent event = baseEvent(run, sequence, candidate);
        event.setEventType(eventType);
        event.setBusinessType("TRANSACTION");
        event.setAmount(amount);
        if (result.isSuccess()) {
            TransactionResult transaction = result.getData();
            event.setBusinessId(transaction == null ? null : transaction.getTransactionNo());
            event.setStatus("SUCCESS");
            event.setMessage(remark + "已写入真实交易、流水和客户通知，交易号 "
                    + (transaction == null ? "-" : transaction.getTransactionNo()));
        } else {
            event.setStatus("FAILED");
            event.setMessage(remark + "未能写入业务链路：" + result.getMessage());
        }
        event.setPayloadJson(payload(run, candidate, "deposit", result.getMessage()));
        return event;
    }

    private SimulationEvent riskProbeEvent(SimulationRun run, int sequence, SimulationAccountCandidate candidate) {
        BigDecimal amount = randomAmount(12000, 240000);
        SimulationEvent event = baseEvent(run, sequence, candidate);
        event.setEventType("RISK_TRANSFER_BURST");
        event.setBusinessType("RISK_SCENARIO");
        event.setAmount(amount);
        event.setStatus("WARNING");
        event.setMessage("生成一条风控压力演练：陌生收款人、短时高频、夜间交易组合信号，建议后续接入实时拦截链路");
        event.setPayloadJson(payload(run, candidate, "risk_probe",
                "amount=" + amount.toPlainString() + ",riskLevel=" + candidate.getRiskLevel()));
        return event;
    }

    private SimulationEvent marketEvent(SimulationRun run, int sequence, SimulationAccountCandidate candidate) {
        BigDecimal referenceAmount = randomAmount(1000, 80000);
        int basisPoints = random.nextInt(821) - 410;
        SimulationEvent event = baseEvent(run, sequence, candidate);
        event.setEventType("MARKET_PRICE_MOVE");
        event.setBusinessType("MARKET_SCENARIO");
        event.setAmount(referenceAmount);
        event.setStatus(Math.abs(basisPoints) >= 250 ? "WARNING" : "RECORDED");
        event.setMessage("记录一条市场波动仿真：参考资产 " + referenceAmount.toPlainString()
                + " 元，波动 " + basisPoints + "bp，用于后续联动理财适配和客户提醒");
        event.setPayloadJson(payload(run, candidate, "market_move", "basisPoints=" + basisPoints));
        return event;
    }

    private SimulationEvent failureEvent(SimulationRun run, int sequence, SimulationAccountCandidate candidate,
                                         String message) {
        SimulationEvent event = baseEvent(run, sequence, candidate);
        event.setEventType("SIMULATION_FAILURE");
        event.setBusinessType("SIMULATION");
        event.setStatus("FAILED");
        event.setMessage(message);
        event.setPayloadJson(payload(run, candidate, "failure", message));
        return event;
    }

    private SimulationEvent baseEvent(SimulationRun run, int sequence, SimulationAccountCandidate candidate) {
        SimulationEvent event = new SimulationEvent();
        event.setRunId(run.getRunId());
        event.setEventSequence(sequence);
        event.setCustomerId(candidate.getCustomerId());
        event.setCustomerName(candidate.getCustomerName());
        event.setAccountId(candidate.getAccountId());
        event.setAccountNo(candidate.getAccountNo());
        return event;
    }

    private String normalizeScenario(String scenarioCode) {
        if ("DAILY_FLOW".equals(scenarioCode) || "PAYDAY".equals(scenarioCode)
                || "RISK_STRESS".equals(scenarioCode) || "MARKET_SWING".equals(scenarioCode)) {
            return scenarioCode;
        }
        return null;
    }

    private int normalizePositive(Integer value, int defaultValue) {
        if (value == null || value.intValue() <= 0) {
            return defaultValue;
        }
        return value.intValue();
    }

    private String normalizeSpeed(String speed) {
        if ("SLOW".equals(speed) || "FAST".equals(speed)) {
            return speed;
        }
        return "NORMAL";
    }

    private String scenarioName(String scenarioCode) {
        if ("PAYDAY".equals(scenarioCode)) {
            return "发薪日现金流";
        }
        if ("RISK_STRESS".equals(scenarioCode)) {
            return "风控压力演练";
        }
        if ("MARKET_SWING".equals(scenarioCode)) {
            return "市场波动联动";
        }
        return "日常流水回放";
    }

    private BigDecimal randomAmount(int min, int max) {
        long minCents = min * 100L;
        long maxCents = max * 100L;
        long cents = minCents + (random.nextLong() & Long.MAX_VALUE) % (maxCents - minCents + 1L);
        return BigDecimal.valueOf(cents, 2);
    }

    private String payload(SimulationRun run, SimulationAccountCandidate candidate, String action, String note) {
        return "{\"runCode\":\"" + jsonEscape(run.getRunCode())
                + "\",\"scenario\":\"" + jsonEscape(run.getScenarioCode())
                + "\",\"action\":\"" + jsonEscape(action)
                + "\",\"customerId\":" + candidate.getCustomerId()
                + ",\"accountId\":" + candidate.getAccountId()
                + ",\"riskLevel\":\"" + jsonEscape(candidate.getRiskLevel())
                + "\",\"note\":\"" + jsonEscape(note) + "\"}";
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String safeMessage(String value) {
        if (value == null || value.trim().length() == 0) {
            return "未知错误";
        }
        return value.length() > 220 ? value.substring(0, 220) : value;
    }
}
