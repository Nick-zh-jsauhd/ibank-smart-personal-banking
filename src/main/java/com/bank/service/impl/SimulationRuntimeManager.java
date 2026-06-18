package com.bank.service.impl;

import com.bank.bean.SimulationRun;
import com.bank.dto.ServiceResult;
import com.bank.dto.SimulationRuntimeState;

import java.sql.Timestamp;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class SimulationRuntimeManager {
    private static final SimulationRuntimeManager INSTANCE = new SimulationRuntimeManager();

    private final Object lock = new Object();
    private final SimulationServiceImpl simulationService = new SimulationServiceImpl();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ibank-simulation-runtime");
            thread.setDaemon(true);
            return thread;
        }
    });

    private ScheduledFuture<?> future;
    private SimulationRuntimeState state = idleState("实时沙盘未启动");
    private String ipAddress = "127.0.0.1";

    private SimulationRuntimeManager() {
    }

    public static SimulationRuntimeManager getInstance() {
        return INSTANCE;
    }

    public ServiceResult<SimulationRuntimeState> start(long adminUserId, String scenarioCode, int targetEventCount,
                                                       int eventsPerTick, String speed, String ipAddress) {
        synchronized (lock) {
            if (state.isRunning()) {
                return ServiceResult.failure("实时仿真正在运行，请先停止当前批次");
            }
            int target = clamp(targetEventCount, 10, 1000);
            int tickSize = clamp(eventsPerTick, 1, 10);
            String normalizedSpeed = normalizeSpeed(speed);
            int intervalMillis = intervalMillis(normalizedSpeed);
            ServiceResult<SimulationRun> created = simulationService.createRun(adminUserId, scenarioCode, target,
                    normalizedSpeed, "AUTO", ipAddress);
            if (!created.isSuccess()) {
                return ServiceResult.failure(created.getMessage());
            }
            SimulationRun run = created.getData();
            this.ipAddress = ipAddress;
            state = new SimulationRuntimeState();
            state.setRunning(true);
            state.setPaused(false);
            state.setRunId(run.getRunId());
            state.setRunCode(run.getRunCode());
            state.setScenarioCode(run.getScenarioCode());
            state.setScenarioName(run.getScenarioName());
            state.setSpeed(normalizedSpeed);
            state.setTargetEventCount(target);
            state.setEventsPerTick(tickSize);
            state.setIntervalMillis(intervalMillis);
            state.setGeneratedEventCount(0);
            state.setRiskEventCount(0);
            state.setFailureEventCount(0);
            state.setStartedAt(new Timestamp(System.currentTimeMillis()));
            state.setMessage("实时仿真已启动，事件流正在写入");

            future = executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    tick();
                }
            }, 0L, intervalMillis, TimeUnit.MILLISECONDS);
            return ServiceResult.success("实时仿真已启动", snapshotLocked());
        }
    }

    public ServiceResult<SimulationRuntimeState> pause() {
        synchronized (lock) {
            if (!state.isRunning()) {
                return ServiceResult.failure("当前没有运行中的实时仿真");
            }
            state.setPaused(true);
            state.setMessage("实时仿真已暂停，事件流暂不写入");
            return ServiceResult.success("实时仿真已暂停", snapshotLocked());
        }
    }

    public ServiceResult<SimulationRuntimeState> resume() {
        synchronized (lock) {
            if (!state.isRunning()) {
                return ServiceResult.failure("当前没有运行中的实时仿真");
            }
            state.setPaused(false);
            state.setMessage("实时仿真已继续运行");
            return ServiceResult.success("实时仿真已继续运行", snapshotLocked());
        }
    }

    public ServiceResult<SimulationRuntimeState> stop(String ipAddress) {
        synchronized (lock) {
            if (!state.isRunning()) {
                return ServiceResult.failure("当前没有运行中的实时仿真");
            }
            cancelFutureLocked();
            simulationService.finishRun(state.getRunId(), "STOPPED",
                    "管理员停止实时仿真，已生成 " + state.getGeneratedEventCount() + " 条事件", ipAddress);
            state.setRunning(false);
            state.setPaused(false);
            state.setMessage("实时仿真已停止");
            return ServiceResult.success("实时仿真已停止", snapshotLocked());
        }
    }

    public SimulationRuntimeState snapshot() {
        synchronized (lock) {
            return snapshotLocked();
        }
    }

    public void shutdown() {
        synchronized (lock) {
            if (state.isRunning() && state.getRunId() != null) {
                simulationService.finishRun(state.getRunId(), "STOPPED",
                        "应用停止，实时仿真自动结束", ipAddress);
            }
            cancelFutureLocked();
            executor.shutdownNow();
            state = idleState("实时沙盘已随应用停止");
        }
    }

    private void tick() {
        Long runId;
        int tickSize;
        synchronized (lock) {
            if (!state.isRunning() || state.isPaused() || state.getRunId() == null) {
                return;
            }
            runId = state.getRunId();
            tickSize = Math.min(state.getEventsPerTick(),
                    Math.max(0, state.getTargetEventCount() - state.getGeneratedEventCount()));
            if (tickSize <= 0) {
                finishReachedTargetLocked();
                return;
            }
        }

        ServiceResult<SimulationRun> result = simulationService.appendEvents(runId, tickSize, ipAddress, true);
        synchronized (lock) {
            if (!state.isRunning()) {
                return;
            }
            state.setLastTickAt(new Timestamp(System.currentTimeMillis()));
            if (!result.isSuccess()) {
                state.setMessage(result.getMessage());
                state.setPaused(true);
                return;
            }
            SimulationRun run = result.getData();
            state.setGeneratedEventCount(Math.min(state.getTargetEventCount(),
                    state.getGeneratedEventCount() + tickSize));
            state.setRiskEventCount(run.getRiskEventCount() == null ? 0 : run.getRiskEventCount());
            state.setFailureEventCount(run.getFailureEventCount() == null ? 0 : run.getFailureEventCount());
            state.setMessage("事件流运行中，已生成 " + state.getGeneratedEventCount()
                    + " / " + state.getTargetEventCount() + " 条");
            if (state.getGeneratedEventCount() >= state.getTargetEventCount()
                    || "COMPLETED".equals(run.getStatus()) || "COMPLETED_WITH_WARNINGS".equals(run.getStatus())) {
                cancelFutureLocked();
                state.setRunning(false);
                state.setPaused(false);
                state.setMessage("实时仿真已达到目标事件数");
            }
        }
    }

    private void finishReachedTargetLocked() {
        if (state.getRunId() != null) {
            simulationService.finishRun(state.getRunId(), "COMPLETED", "实时仿真已达到目标事件数", ipAddress);
        }
        cancelFutureLocked();
        state.setRunning(false);
        state.setPaused(false);
        state.setMessage("实时仿真已达到目标事件数");
    }

    private void cancelFutureLocked() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private SimulationRuntimeState snapshotLocked() {
        SimulationRuntimeState copy = new SimulationRuntimeState();
        copy.setRunning(state.isRunning());
        copy.setPaused(state.isPaused());
        copy.setRunId(state.getRunId());
        copy.setRunCode(state.getRunCode());
        copy.setScenarioCode(state.getScenarioCode());
        copy.setScenarioName(state.getScenarioName());
        copy.setSpeed(state.getSpeed());
        copy.setTargetEventCount(state.getTargetEventCount());
        copy.setEventsPerTick(state.getEventsPerTick());
        copy.setIntervalMillis(state.getIntervalMillis());
        copy.setGeneratedEventCount(state.getGeneratedEventCount());
        copy.setRiskEventCount(state.getRiskEventCount());
        copy.setFailureEventCount(state.getFailureEventCount());
        copy.setMessage(state.getMessage());
        copy.setStartedAt(state.getStartedAt());
        copy.setLastTickAt(state.getLastTickAt());
        return copy;
    }

    private SimulationRuntimeState idleState(String message) {
        SimulationRuntimeState idle = new SimulationRuntimeState();
        idle.setRunning(false);
        idle.setPaused(false);
        idle.setSpeed("NORMAL");
        idle.setTargetEventCount(0);
        idle.setEventsPerTick(0);
        idle.setIntervalMillis(0);
        idle.setGeneratedEventCount(0);
        idle.setRiskEventCount(0);
        idle.setFailureEventCount(0);
        idle.setMessage(message);
        return idle;
    }

    private int intervalMillis(String speed) {
        if ("FAST".equals(speed)) {
            return 1200;
        }
        if ("SLOW".equals(speed)) {
            return 4200;
        }
        return 2400;
    }

    private String normalizeSpeed(String speed) {
        if ("FAST".equals(speed) || "SLOW".equals(speed)) {
            return speed;
        }
        return "NORMAL";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
