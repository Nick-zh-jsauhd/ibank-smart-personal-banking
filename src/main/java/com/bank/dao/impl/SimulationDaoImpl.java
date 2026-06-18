package com.bank.dao.impl;

import com.bank.bean.SimulationEvent;
import com.bank.bean.SimulationRun;
import com.bank.dao.SimulationDao;
import com.bank.dto.SimulationAccountCandidate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SimulationDaoImpl implements SimulationDao {
    @Override
    public void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS t_simulation_run ("
                    + "run_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "run_code VARCHAR(64) NOT NULL,"
                    + "scenario_code VARCHAR(40) NOT NULL,"
                    + "scenario_name VARCHAR(80) NOT NULL,"
                    + "status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',"
                    + "speed VARCHAR(30) NOT NULL DEFAULT 'NORMAL',"
                    + "requested_event_count INT NOT NULL DEFAULT 0,"
                    + "success_event_count INT NOT NULL DEFAULT 0,"
                    + "failure_event_count INT NOT NULL DEFAULT 0,"
                    + "risk_event_count INT NOT NULL DEFAULT 0,"
                    + "admin_user_id BIGINT NULL,"
                    + "config_json TEXT NULL,"
                    + "summary VARCHAR(500) NULL,"
                    + "error_message VARCHAR(1000) NULL,"
                    + "started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "completed_at DATETIME NULL,"
                    + "UNIQUE KEY uk_simulation_run_code (run_code),"
                    + "KEY idx_simulation_run_started_at (started_at),"
                    + "KEY idx_simulation_run_status (status),"
                    + "KEY idx_simulation_run_admin (admin_user_id),"
                    + "CONSTRAINT fk_simulation_run_admin FOREIGN KEY (admin_user_id) REFERENCES t_user (user_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.execute("CREATE TABLE IF NOT EXISTS t_simulation_event ("
                    + "event_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "run_id BIGINT NOT NULL,"
                    + "event_sequence INT NOT NULL,"
                    + "event_type VARCHAR(50) NOT NULL,"
                    + "business_type VARCHAR(50) NULL,"
                    + "business_id VARCHAR(80) NULL,"
                    + "customer_id BIGINT NULL,"
                    + "account_id BIGINT NULL,"
                    + "amount DECIMAL(18,2) NULL,"
                    + "status VARCHAR(30) NOT NULL DEFAULT 'RECORDED',"
                    + "message VARCHAR(500) NULL,"
                    + "payload_json TEXT NULL,"
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "KEY idx_simulation_event_run (run_id, event_sequence),"
                    + "KEY idx_simulation_event_created_at (created_at),"
                    + "KEY idx_simulation_event_status (status),"
                    + "KEY idx_simulation_event_customer (customer_id),"
                    + "CONSTRAINT fk_simulation_event_run FOREIGN KEY (run_id) REFERENCES t_simulation_run (run_id),"
                    + "CONSTRAINT fk_simulation_event_customer FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),"
                    + "CONSTRAINT fk_simulation_event_account FOREIGN KEY (account_id) REFERENCES t_account (account_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    @Override
    public long insertRun(Connection connection, SimulationRun run) throws SQLException {
        String sql = "INSERT INTO t_simulation_run (run_code, scenario_code, scenario_name, status, speed, "
                + "requested_event_count, admin_user_id, config_json, summary) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, run.getRunCode());
            statement.setString(2, run.getScenarioCode());
            statement.setString(3, run.getScenarioName());
            statement.setString(4, run.getStatus());
            statement.setString(5, run.getSpeed());
            statement.setInt(6, intValue(run.getRequestedEventCount()));
            if (run.getAdminUserId() == null) {
                statement.setNull(7, java.sql.Types.BIGINT);
            } else {
                statement.setLong(7, run.getAdminUserId());
            }
            statement.setString(8, run.getConfigJson());
            statement.setString(9, run.getSummary());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No generated key returned for simulation run");
    }

    @Override
    public void updateRunResult(Connection connection, SimulationRun run) throws SQLException {
        String sql = "UPDATE t_simulation_run SET status = ?, success_event_count = ?, failure_event_count = ?, "
                + "risk_event_count = ?, summary = ?, error_message = ?, completed_at = ? WHERE run_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, run.getStatus());
            statement.setInt(2, intValue(run.getSuccessEventCount()));
            statement.setInt(3, intValue(run.getFailureEventCount()));
            statement.setInt(4, intValue(run.getRiskEventCount()));
            statement.setString(5, run.getSummary());
            statement.setString(6, run.getErrorMessage());
            statement.setTimestamp(7, run.getCompletedAt());
            statement.setLong(8, run.getRunId());
            statement.executeUpdate();
        }
    }

    @Override
    public void insertEvent(Connection connection, SimulationEvent event) throws SQLException {
        String sql = "INSERT INTO t_simulation_event (run_id, event_sequence, event_type, business_type, "
                + "business_id, customer_id, account_id, amount, status, message, payload_json) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, event.getRunId());
            statement.setInt(2, intValue(event.getEventSequence()));
            statement.setString(3, event.getEventType());
            statement.setString(4, event.getBusinessType());
            statement.setString(5, event.getBusinessId());
            setNullableLong(statement, 6, event.getCustomerId());
            setNullableLong(statement, 7, event.getAccountId());
            statement.setBigDecimal(8, event.getAmount());
            statement.setString(9, event.getStatus());
            statement.setString(10, event.getMessage());
            statement.setString(11, event.getPayloadJson());
            statement.executeUpdate();
        }
    }

    @Override
    public SimulationRun findLatestRun(Connection connection) throws SQLException {
        String sql = "SELECT run_id, run_code, scenario_code, scenario_name, status, speed, requested_event_count, "
                + "success_event_count, failure_event_count, risk_event_count, admin_user_id, config_json, summary, "
                + "error_message, started_at, completed_at FROM t_simulation_run ORDER BY started_at DESC, run_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return mapRun(resultSet);
            }
        }
        return null;
    }

    @Override
    public SimulationRun findRunById(Connection connection, long runId) throws SQLException {
        String sql = "SELECT run_id, run_code, scenario_code, scenario_name, status, speed, requested_event_count, "
                + "success_event_count, failure_event_count, risk_event_count, admin_user_id, config_json, summary, "
                + "error_message, started_at, completed_at FROM t_simulation_run WHERE run_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, runId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRun(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<SimulationRun> findRecentRuns(Connection connection, int limit) throws SQLException {
        String sql = "SELECT run_id, run_code, scenario_code, scenario_name, status, speed, requested_event_count, "
                + "success_event_count, failure_event_count, risk_event_count, admin_user_id, config_json, summary, "
                + "error_message, started_at, completed_at FROM t_simulation_run "
                + "ORDER BY started_at DESC, run_id DESC LIMIT ?";
        List<SimulationRun> runs = new ArrayList<SimulationRun>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    runs.add(mapRun(resultSet));
                }
            }
        }
        return runs;
    }

    @Override
    public List<SimulationEvent> findRecentEvents(Connection connection, int limit) throws SQLException {
        String sql = eventSelectSql()
                + "ORDER BY e.created_at DESC, e.event_id DESC LIMIT ?";
        return queryEvents(connection, sql, null, limit);
    }

    @Override
    public List<SimulationEvent> findEventsByRun(Connection connection, long runId, int limit) throws SQLException {
        String sql = eventSelectSql()
                + "WHERE e.run_id = ? ORDER BY e.event_sequence ASC, e.event_id ASC LIMIT ?";
        return queryEvents(connection, sql, runId, limit);
    }

    @Override
    public List<SimulationAccountCandidate> findAccountCandidates(Connection connection, int limit) throws SQLException {
        String sql = "SELECT u.user_id, c.customer_id, c.full_name, c.risk_level, a.account_id, a.account_no, "
                + "a.available_balance FROM t_customer c "
                + "JOIN t_user u ON c.user_id = u.user_id "
                + "JOIN t_account a ON c.customer_id = a.customer_id "
                + "WHERE u.role = 'CUSTOMER' AND u.status = 'NORMAL' AND a.status = 'NORMAL' "
                + "ORDER BY RAND() LIMIT ?";
        List<SimulationAccountCandidate> candidates = new ArrayList<SimulationAccountCandidate>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SimulationAccountCandidate candidate = new SimulationAccountCandidate();
                    candidate.setUserId(resultSet.getLong("user_id"));
                    candidate.setCustomerId(resultSet.getLong("customer_id"));
                    candidate.setCustomerName(resultSet.getString("full_name"));
                    candidate.setRiskLevel(resultSet.getString("risk_level"));
                    candidate.setAccountId(resultSet.getLong("account_id"));
                    candidate.setAccountNo(resultSet.getString("account_no"));
                    candidate.setAvailableBalance(resultSet.getBigDecimal("available_balance"));
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    @Override
    public int maxEventSequence(Connection connection, long runId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(event_sequence), 0) FROM t_simulation_event WHERE run_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, runId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countEventsByRun(Connection connection, long runId) throws SQLException {
        return countByRun(connection, "SELECT COUNT(*) FROM t_simulation_event WHERE run_id = ?", runId);
    }

    @Override
    public int countSuccessfulEventsByRun(Connection connection, long runId) throws SQLException {
        return countByRun(connection, "SELECT COUNT(*) FROM t_simulation_event "
                + "WHERE run_id = ? AND status IN ('SUCCESS', 'RECORDED')", runId);
    }

    @Override
    public int countFailedEventsByRun(Connection connection, long runId) throws SQLException {
        return countByRun(connection, "SELECT COUNT(*) FROM t_simulation_event "
                + "WHERE run_id = ? AND status IN ('FAILED', 'BLOCKED')", runId);
    }

    @Override
    public int countRiskWarningEventsByRun(Connection connection, long runId) throws SQLException {
        return countByRun(connection, "SELECT COUNT(*) FROM t_simulation_event "
                + "WHERE run_id = ? AND (status IN ('WARNING', 'BLOCKED') OR event_type LIKE 'RISK_%')", runId);
    }

    @Override
    public int countRuns(Connection connection) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM t_simulation_run");
    }

    @Override
    public int countEvents(Connection connection) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM t_simulation_event");
    }

    @Override
    public int countBusinessSuccessEvents(Connection connection) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM t_simulation_event "
                + "WHERE business_type = 'TRANSACTION' AND status = 'SUCCESS'");
    }

    @Override
    public int countRiskWarningEvents(Connection connection) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM t_simulation_event "
                + "WHERE status IN ('WARNING', 'BLOCKED') OR event_type LIKE 'RISK_%'");
    }

    private String eventSelectSql() {
        return "SELECT e.event_id, e.run_id, e.event_sequence, e.event_type, e.business_type, e.business_id, "
                + "e.customer_id, c.full_name AS customer_name, e.account_id, a.account_no, e.amount, e.status, "
                + "e.message, e.payload_json, e.created_at FROM t_simulation_event e "
                + "LEFT JOIN t_customer c ON e.customer_id = c.customer_id "
                + "LEFT JOIN t_account a ON e.account_id = a.account_id ";
    }

    private List<SimulationEvent> queryEvents(Connection connection, String sql, Long runId, int limit)
            throws SQLException {
        List<SimulationEvent> events = new ArrayList<SimulationEvent>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            if (runId != null) {
                statement.setLong(index++, runId);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(mapEvent(resultSet));
                }
            }
        }
        return events;
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    private int countByRun(Connection connection, String sql, long runId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, runId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private SimulationRun mapRun(ResultSet resultSet) throws SQLException {
        SimulationRun run = new SimulationRun();
        run.setRunId(resultSet.getLong("run_id"));
        run.setRunCode(resultSet.getString("run_code"));
        run.setScenarioCode(resultSet.getString("scenario_code"));
        run.setScenarioName(resultSet.getString("scenario_name"));
        run.setStatus(resultSet.getString("status"));
        run.setSpeed(resultSet.getString("speed"));
        run.setRequestedEventCount(resultSet.getInt("requested_event_count"));
        run.setSuccessEventCount(resultSet.getInt("success_event_count"));
        run.setFailureEventCount(resultSet.getInt("failure_event_count"));
        run.setRiskEventCount(resultSet.getInt("risk_event_count"));
        long adminUserId = resultSet.getLong("admin_user_id");
        run.setAdminUserId(resultSet.wasNull() ? null : adminUserId);
        run.setConfigJson(resultSet.getString("config_json"));
        run.setSummary(resultSet.getString("summary"));
        run.setErrorMessage(resultSet.getString("error_message"));
        run.setStartedAt(resultSet.getTimestamp("started_at"));
        run.setCompletedAt(resultSet.getTimestamp("completed_at"));
        return run;
    }

    private SimulationEvent mapEvent(ResultSet resultSet) throws SQLException {
        SimulationEvent event = new SimulationEvent();
        event.setEventId(resultSet.getLong("event_id"));
        event.setRunId(resultSet.getLong("run_id"));
        event.setEventSequence(resultSet.getInt("event_sequence"));
        event.setEventType(resultSet.getString("event_type"));
        event.setBusinessType(resultSet.getString("business_type"));
        event.setBusinessId(resultSet.getString("business_id"));
        long customerId = resultSet.getLong("customer_id");
        event.setCustomerId(resultSet.wasNull() ? null : customerId);
        event.setCustomerName(resultSet.getString("customer_name"));
        long accountId = resultSet.getLong("account_id");
        event.setAccountId(resultSet.wasNull() ? null : accountId);
        event.setAccountNo(resultSet.getString("account_no"));
        event.setAmount(resultSet.getBigDecimal("amount"));
        event.setStatus(resultSet.getString("status"));
        event.setMessage(resultSet.getString("message"));
        event.setPayloadJson(resultSet.getString("payload_json"));
        event.setCreatedAt(resultSet.getTimestamp("created_at"));
        return event;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value.longValue());
        }
    }
}
