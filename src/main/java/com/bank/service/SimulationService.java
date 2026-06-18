package com.bank.service;

import com.bank.bean.SimulationRun;
import com.bank.dto.ServiceResult;
import com.bank.dto.SimulationDashboardView;

public interface SimulationService {
    ServiceResult<SimulationDashboardView> dashboard(Long selectedRunId);

    ServiceResult<SimulationRun> runOnce(long adminUserId, String scenarioCode, int eventCount,
                                         String speed, String ipAddress);

    ServiceResult<SimulationRun> createRun(long adminUserId, String scenarioCode, int requestedEventCount,
                                           String speed, String mode, String ipAddress);

    ServiceResult<SimulationRun> appendEvents(long runId, int eventCount, String ipAddress,
                                              boolean completeWhenTargetReached);

    ServiceResult<SimulationRun> finishRun(long runId, String status, String summary, String ipAddress);
}
