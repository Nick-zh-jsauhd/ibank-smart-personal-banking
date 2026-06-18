package com.bank.dto;

import com.bank.bean.SimulationEvent;
import com.bank.bean.SimulationRun;

import java.util.ArrayList;
import java.util.List;

public class SimulationDashboardView {
    private int totalRunCount;
    private int totalEventCount;
    private int totalBusinessSuccessCount;
    private int totalRiskWarningCount;
    private SimulationRun latestRun;
    private SimulationRun selectedRun;
    private List<SimulationRun> recentRuns = new ArrayList<SimulationRun>();
    private List<SimulationEvent> latestEvents = new ArrayList<SimulationEvent>();
    private List<SimulationEvent> selectedRunEvents = new ArrayList<SimulationEvent>();

    public int getTotalRunCount() {
        return totalRunCount;
    }

    public void setTotalRunCount(int totalRunCount) {
        this.totalRunCount = totalRunCount;
    }

    public int getTotalEventCount() {
        return totalEventCount;
    }

    public void setTotalEventCount(int totalEventCount) {
        this.totalEventCount = totalEventCount;
    }

    public int getTotalBusinessSuccessCount() {
        return totalBusinessSuccessCount;
    }

    public void setTotalBusinessSuccessCount(int totalBusinessSuccessCount) {
        this.totalBusinessSuccessCount = totalBusinessSuccessCount;
    }

    public int getTotalRiskWarningCount() {
        return totalRiskWarningCount;
    }

    public void setTotalRiskWarningCount(int totalRiskWarningCount) {
        this.totalRiskWarningCount = totalRiskWarningCount;
    }

    public SimulationRun getLatestRun() {
        return latestRun;
    }

    public void setLatestRun(SimulationRun latestRun) {
        this.latestRun = latestRun;
    }

    public SimulationRun getSelectedRun() {
        return selectedRun;
    }

    public void setSelectedRun(SimulationRun selectedRun) {
        this.selectedRun = selectedRun;
    }

    public List<SimulationRun> getRecentRuns() {
        return recentRuns;
    }

    public void setRecentRuns(List<SimulationRun> recentRuns) {
        this.recentRuns = recentRuns == null ? new ArrayList<SimulationRun>() : recentRuns;
    }

    public List<SimulationEvent> getLatestEvents() {
        return latestEvents;
    }

    public void setLatestEvents(List<SimulationEvent> latestEvents) {
        this.latestEvents = latestEvents == null ? new ArrayList<SimulationEvent>() : latestEvents;
    }

    public List<SimulationEvent> getSelectedRunEvents() {
        return selectedRunEvents;
    }

    public void setSelectedRunEvents(List<SimulationEvent> selectedRunEvents) {
        this.selectedRunEvents = selectedRunEvents == null ? new ArrayList<SimulationEvent>() : selectedRunEvents;
    }
}
