package com.bank.dao;

import com.bank.bean.SimulationEvent;
import com.bank.bean.SimulationRun;
import com.bank.dto.SimulationAccountCandidate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface SimulationDao {
    void ensureSchema(Connection connection) throws SQLException;

    long insertRun(Connection connection, SimulationRun run) throws SQLException;

    void updateRunResult(Connection connection, SimulationRun run) throws SQLException;

    void insertEvent(Connection connection, SimulationEvent event) throws SQLException;

    SimulationRun findLatestRun(Connection connection) throws SQLException;

    SimulationRun findRunById(Connection connection, long runId) throws SQLException;

    List<SimulationRun> findRecentRuns(Connection connection, int limit) throws SQLException;

    List<SimulationEvent> findRecentEvents(Connection connection, int limit) throws SQLException;

    List<SimulationEvent> findEventsByRun(Connection connection, long runId, int limit) throws SQLException;

    List<SimulationAccountCandidate> findAccountCandidates(Connection connection, int limit) throws SQLException;

    int maxEventSequence(Connection connection, long runId) throws SQLException;

    int countEventsByRun(Connection connection, long runId) throws SQLException;

    int countSuccessfulEventsByRun(Connection connection, long runId) throws SQLException;

    int countFailedEventsByRun(Connection connection, long runId) throws SQLException;

    int countRiskWarningEventsByRun(Connection connection, long runId) throws SQLException;

    int countRuns(Connection connection) throws SQLException;

    int countEvents(Connection connection) throws SQLException;

    int countBusinessSuccessEvents(Connection connection) throws SQLException;

    int countRiskWarningEvents(Connection connection) throws SQLException;
}
