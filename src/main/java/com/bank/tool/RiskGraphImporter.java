package com.bank.tool;

import com.bank.util.DBUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RiskGraphImporter {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int DEFAULT_LIMIT = 100000;
    private static final int DEFAULT_COMMIT_SIZE = 1000;
    private static final DateTimeFormatter BATCH_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm")
    };

    private final Map<String, NodeRef> nodeCache = new HashMap<String, NodeRef>();

    public static void main(String[] args) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            Options.printUsage();
            System.exit(2);
            return;
        }

        RiskGraphImporter importer = new RiskGraphImporter();
        try {
            ImportStats stats = importer.importFile(options);
            System.out.println("Risk graph import completed.");
            System.out.println("Batch code: " + options.batchCode);
            System.out.println("Dataset name: " + options.datasetName);
            System.out.println("Source format: " + options.sourceFormat.name());
            System.out.println("Read rows: " + stats.readRows);
            System.out.println("Imported edges: " + stats.importedEdges);
            System.out.println("Skipped rows: " + stats.skippedRows);
            System.out.println("Fraud edges: " + stats.fraudEdges);
            System.out.println("Flagged edges: " + stats.flaggedEdges);
            System.out.println("Touched nodes: " + stats.touchedNodes);
            if (options.dryRun) {
                System.out.println("Dry run only. Database was not modified.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private ImportStats importFile(Options options) throws Exception {
        Path csvPath = Paths.get(options.file).toAbsolutePath().normalize();
        if (!Files.isRegularFile(csvPath)) {
            throw new IllegalArgumentException("CSV not found: " + csvPath);
        }

        Header header = readHeader(csvPath);
        options.resolveRuntimeDefaults(header);

        ImportStats stats = new ImportStats();
        if (options.dryRun) {
            dryRun(csvPath, header, options, stats);
            return stats;
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        long batchId = 0L;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            ensureRiskGraphTables(connection);
            batchId = createBatch(connection, options, csvPath);
            connection.commit();

            connection.setAutoCommit(false);
            readAndImport(connection, batchId, csvPath, header, options, stats);
            updateBatchSuccess(connection, batchId, stats);
            connection.commit();
            return stats;
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    if (batchId > 0L) {
                        updateBatchFailure(connection, batchId, e);
                        connection.commit();
                    }
                } catch (SQLException ignored) {
                    // Keep the original exception.
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                connection.setAutoCommit(oldAutoCommit);
                connection.close();
            }
        }
    }

    private Header readHeader(Path csvPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV is empty: " + csvPath);
            }
            return Header.from(headerLine);
        }
    }

    private void dryRun(Path csvPath, Header header, Options options, ImportStats stats) throws IOException {
        Set<String> touchedNodes = new HashSet<String>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (options.limit > 0 && stats.readRows >= options.limit) {
                    break;
                }
                stats.readRows++;
                try {
                    GraphRow row = GraphRow.parse(rowNo, line, header, options);
                    stats.importedEdges++;
                    touchedNodes.add(row.fromExternalId);
                    touchedNodes.add(row.toExternalId);
                    if (row.fraud) {
                        stats.fraudEdges++;
                    }
                    if (row.flagged) {
                        stats.flaggedEdges++;
                    }
                } catch (RuntimeException e) {
                    stats.skippedRows++;
                }
            }
        }
        stats.touchedNodes = touchedNodes.size();
    }

    private void readAndImport(Connection connection, long batchId, Path csvPath, Header header,
                               Options options, ImportStats stats) throws IOException, SQLException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            int rowNo = 0;
            int pending = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (options.limit > 0 && stats.readRows >= options.limit) {
                    break;
                }
                stats.readRows++;
                try {
                    GraphRow row = GraphRow.parse(rowNo, line, header, options);
                    boolean imported = importRow(connection, batchId, row, options);
                    if (imported) {
                        stats.importedEdges++;
                        if (row.fraud) {
                            stats.fraudEdges++;
                        }
                        if (row.flagged) {
                            stats.flaggedEdges++;
                        }
                    } else {
                        stats.skippedRows++;
                    }
                } catch (RuntimeException e) {
                    stats.skippedRows++;
                    System.err.println("Skip graph row " + rowNo + ": " + e.getMessage());
                }
                pending++;
                if (pending >= options.commitSize) {
                    connection.commit();
                    pending = 0;
                }
            }
        }
    }

    private boolean importRow(Connection connection, long batchId, GraphRow row, Options options)
            throws SQLException {
        if (graphEdgeExists(connection, batchId, row.rowNo)) {
            return false;
        }

        NodeRef from = findOrCreateNode(connection, batchId, options.datasetName, row.fromExternalId,
                row.fromNodeType, row.fromDisplayName, row.fromFeatureJson);
        NodeRef to = findOrCreateNode(connection, batchId, options.datasetName, row.toExternalId,
                row.toNodeType, row.toDisplayName, row.toFeatureJson);

        insertEdge(connection, batchId, row, options, from, to);
        updateNodeStats(connection, from.nodeId, true, row.amount, row.fraud);
        updateNodeStats(connection, to.nodeId, false, row.amount, row.fraud);
        return true;
    }

    private boolean graphEdgeExists(Connection connection, long batchId, int sourceRowNo) throws SQLException {
        String sql = "SELECT 1 FROM t_risk_graph_edge WHERE batch_id = ? AND source_row_no = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            statement.setInt(2, sourceRowNo);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private NodeRef findOrCreateNode(Connection connection, long batchId, String datasetName, String externalNodeId,
                                     String nodeType, String displayName, String featureJson) throws SQLException {
        String cacheKey = datasetName + "\n" + externalNodeId;
        NodeRef cached = nodeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        NodeRef existing = findNode(connection, datasetName, externalNodeId);
        if (existing != null) {
            nodeCache.put(cacheKey, existing);
            return existing;
        }

        String sql = "INSERT INTO t_risk_graph_node (dataset_name, external_node_id, node_type, display_name, "
                + "first_batch_id, feature_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, datasetName);
            statement.setString(2, externalNodeId);
            statement.setString(3, abbreviate(nodeType, 30));
            statement.setString(4, abbreviate(displayName, 180));
            statement.setLong(5, batchId);
            statement.setString(6, featureJson);
            statement.executeUpdate();
            NodeRef created = new NodeRef(generatedId(statement));
            nodeCache.put(cacheKey, created);
            return created;
        } catch (SQLIntegrityConstraintViolationException e) {
            NodeRef createdByOther = findNode(connection, datasetName, externalNodeId);
            if (createdByOther == null) {
                throw e;
            }
            nodeCache.put(cacheKey, createdByOther);
            return createdByOther;
        }
    }

    private NodeRef findNode(Connection connection, String datasetName, String externalNodeId) throws SQLException {
        String sql = "SELECT graph_node_id FROM t_risk_graph_node WHERE dataset_name = ? AND external_node_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, datasetName);
            statement.setString(2, externalNodeId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new NodeRef(rs.getLong("graph_node_id"));
                }
            }
        }
        return null;
    }

    private void insertEdge(Connection connection, long batchId, GraphRow row, Options options, NodeRef from, NodeRef to)
            throws SQLException {
        String sql = "INSERT INTO t_risk_graph_edge (batch_id, dataset_name, source_row_no, source_edge_id, "
                + "from_node_id, to_node_id, from_external_id, to_external_id, edge_type, amount, currency, "
                + "paid_amount, paid_currency, received_amount, received_currency, event_time, source_step, "
                + "label_fraud, label_rule, label_source, typology, feature_json, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            statement.setString(2, options.datasetName);
            statement.setInt(3, row.rowNo);
            statement.setString(4, sourceEdgeId(options.batchCode, row));
            statement.setLong(5, from.nodeId);
            statement.setLong(6, to.nodeId);
            statement.setString(7, abbreviate(row.fromExternalId, 180));
            statement.setString(8, abbreviate(row.toExternalId, 180));
            statement.setString(9, abbreviate(row.edgeType, 60));
            statement.setBigDecimal(10, safeMoney(row.amount));
            statement.setString(11, abbreviate(row.currency, 12));
            setNullableMoney(statement, 12, row.paidAmount);
            statement.setString(13, abbreviate(row.paidCurrency, 12));
            setNullableMoney(statement, 14, row.receivedAmount);
            statement.setString(15, abbreviate(row.receivedCurrency, 12));
            setNullableTimestamp(statement, 16, row.eventTime);
            setNullableInt(statement, 17, row.step);
            statement.setInt(18, row.fraud ? 1 : 0);
            statement.setInt(19, row.flagged ? 1 : 0);
            statement.setString(20, abbreviate(row.labelSource, 40));
            statement.setString(21, abbreviate(row.typology, 80));
            statement.setString(22, row.featureJson);
            statement.executeUpdate();
        }
    }

    private void updateNodeStats(Connection connection, long nodeId, boolean outgoing, BigDecimal amount, boolean fraud)
            throws SQLException {
        String degreeColumn = outgoing ? "out_degree" : "in_degree";
        String fraudColumn = outgoing ? "fraud_out_degree" : "fraud_in_degree";
        String amountColumn = outgoing ? "total_out_amount" : "total_in_amount";
        String sql = "UPDATE t_risk_graph_node SET " + degreeColumn + " = " + degreeColumn + " + 1, "
                + fraudColumn + " = " + fraudColumn + " + ?, "
                + amountColumn + " = " + amountColumn + " + ?, updated_at = NOW() WHERE graph_node_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fraud ? 1 : 0);
            statement.setBigDecimal(2, safeMoney(amount));
            statement.setLong(3, nodeId);
            statement.executeUpdate();
        }
    }

    private long createBatch(Connection connection, Options options, Path csvPath) throws SQLException {
        String sql = "INSERT INTO t_risk_graph_dataset_batch (batch_code, dataset_name, source_file, source_format, "
                + "row_limit, status, metadata_json, started_at) VALUES (?, ?, ?, ?, ?, 'RUNNING', ?, NOW()) "
                + "ON DUPLICATE KEY UPDATE graph_batch_id = LAST_INSERT_ID(graph_batch_id), status = 'RUNNING', "
                + "error_message = NULL, completed_at = NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, options.batchCode);
            statement.setString(2, options.datasetName);
            statement.setString(3, csvPath.toString());
            statement.setString(4, options.sourceFormat.name());
            if (options.limit > 0) {
                statement.setInt(5, options.limit);
            } else {
                statement.setNull(5, Types.INTEGER);
            }
            statement.setString(6, "{\"commit_size\":" + options.commitSize
                    + ",\"start_date\":\"" + options.startDate + "\"}");
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void updateBatchSuccess(Connection connection, long batchId, ImportStats stats) throws SQLException {
        BatchTotals totals = batchTotals(connection, batchId);
        stats.touchedNodes = totals.nodeRows;
        String sql = "UPDATE t_risk_graph_dataset_batch SET node_rows = ?, edge_rows = ?, skipped_rows = ?, "
                + "normal_edges = ?, fraud_edges = ?, flagged_edges = ?, status = 'SUCCESS', completed_at = NOW() "
                + "WHERE graph_batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, totals.nodeRows);
            statement.setInt(2, totals.edgeRows);
            statement.setInt(3, stats.skippedRows);
            statement.setInt(4, totals.normalEdges);
            statement.setInt(5, totals.fraudEdges);
            statement.setInt(6, totals.flaggedEdges);
            statement.setLong(7, batchId);
            statement.executeUpdate();
        }
    }

    private BatchTotals batchTotals(Connection connection, long batchId) throws SQLException {
        BatchTotals totals = new BatchTotals();
        String edgeSql = "SELECT COUNT(*) AS edge_rows, "
                + "SUM(CASE WHEN label_fraud = 0 AND label_rule = 0 THEN 1 ELSE 0 END) AS normal_edges, "
                + "SUM(CASE WHEN label_fraud = 1 THEN 1 ELSE 0 END) AS fraud_edges, "
                + "SUM(CASE WHEN label_rule = 1 THEN 1 ELSE 0 END) AS flagged_edges "
                + "FROM t_risk_graph_edge WHERE batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(edgeSql)) {
            statement.setLong(1, batchId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    totals.edgeRows = rs.getInt("edge_rows");
                    totals.normalEdges = rs.getInt("normal_edges");
                    totals.fraudEdges = rs.getInt("fraud_edges");
                    totals.flaggedEdges = rs.getInt("flagged_edges");
                }
            }
        }

        String nodeSql = "SELECT COUNT(*) AS node_rows FROM ("
                + "SELECT from_node_id AS node_id FROM t_risk_graph_edge WHERE batch_id = ? "
                + "UNION SELECT to_node_id AS node_id FROM t_risk_graph_edge WHERE batch_id = ?"
                + ") x";
        try (PreparedStatement statement = connection.prepareStatement(nodeSql)) {
            statement.setLong(1, batchId);
            statement.setLong(2, batchId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    totals.nodeRows = rs.getInt("node_rows");
                }
            }
        }
        return totals;
    }

    private void updateBatchFailure(Connection connection, long batchId, Exception e) throws SQLException {
        String sql = "UPDATE t_risk_graph_dataset_batch SET status = 'FAILED', error_message = ?, "
                + "completed_at = NOW() WHERE graph_batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, abbreviate(e.getMessage(), 1000));
            statement.setLong(2, batchId);
            statement.executeUpdate();
        }
    }

    private void ensureRiskGraphTables(Connection connection) throws SQLException {
        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_graph_dataset_batch ("
                + "graph_batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "batch_code VARCHAR(80) NOT NULL,"
                + "dataset_name VARCHAR(60) NOT NULL,"
                + "source_file VARCHAR(500) NOT NULL,"
                + "source_format VARCHAR(30) NOT NULL,"
                + "row_limit INT NULL,"
                + "node_rows INT NOT NULL DEFAULT 0,"
                + "edge_rows INT NOT NULL DEFAULT 0,"
                + "skipped_rows INT NOT NULL DEFAULT 0,"
                + "normal_edges INT NOT NULL DEFAULT 0,"
                + "fraud_edges INT NOT NULL DEFAULT 0,"
                + "flagged_edges INT NOT NULL DEFAULT 0,"
                + "status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',"
                + "error_message VARCHAR(1000) NULL,"
                + "metadata_json TEXT NULL,"
                + "started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "completed_at DATETIME NULL,"
                + "UNIQUE KEY uk_risk_graph_batch_code (batch_code),"
                + "KEY idx_risk_graph_batch_dataset (dataset_name),"
                + "KEY idx_risk_graph_batch_status (status)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_graph_node ("
                + "graph_node_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "dataset_name VARCHAR(60) NOT NULL,"
                + "external_node_id VARCHAR(180) NOT NULL,"
                + "node_type VARCHAR(30) NOT NULL,"
                + "display_name VARCHAR(180) NULL,"
                + "first_batch_id BIGINT NULL,"
                + "in_degree INT NOT NULL DEFAULT 0,"
                + "out_degree INT NOT NULL DEFAULT 0,"
                + "fraud_in_degree INT NOT NULL DEFAULT 0,"
                + "fraud_out_degree INT NOT NULL DEFAULT 0,"
                + "total_in_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,"
                + "total_out_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,"
                + "feature_json TEXT NULL,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uk_risk_graph_node_dataset_external (dataset_name, external_node_id),"
                + "KEY idx_risk_graph_node_dataset_type (dataset_name, node_type),"
                + "KEY idx_risk_graph_node_in_degree (in_degree),"
                + "KEY idx_risk_graph_node_out_degree (out_degree),"
                + "CONSTRAINT fk_risk_graph_node_first_batch "
                + "FOREIGN KEY (first_batch_id) REFERENCES t_risk_graph_dataset_batch (graph_batch_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_graph_edge ("
                + "graph_edge_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "batch_id BIGINT NOT NULL,"
                + "dataset_name VARCHAR(60) NOT NULL,"
                + "source_row_no INT NOT NULL,"
                + "source_edge_id VARCHAR(120) NOT NULL,"
                + "from_node_id BIGINT NOT NULL,"
                + "to_node_id BIGINT NOT NULL,"
                + "from_external_id VARCHAR(180) NOT NULL,"
                + "to_external_id VARCHAR(180) NOT NULL,"
                + "edge_type VARCHAR(60) NOT NULL,"
                + "amount DECIMAL(20,2) NOT NULL,"
                + "currency VARCHAR(12) NULL,"
                + "paid_amount DECIMAL(20,2) NULL,"
                + "paid_currency VARCHAR(12) NULL,"
                + "received_amount DECIMAL(20,2) NULL,"
                + "received_currency VARCHAR(12) NULL,"
                + "event_time DATETIME NULL,"
                + "source_step INT NULL,"
                + "label_fraud TINYINT(1) NOT NULL DEFAULT 0,"
                + "label_rule TINYINT(1) NOT NULL DEFAULT 0,"
                + "label_source VARCHAR(40) NOT NULL,"
                + "typology VARCHAR(80) NULL,"
                + "feature_json TEXT NULL,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uk_risk_graph_edge_batch_row (batch_id, source_row_no),"
                + "KEY idx_risk_graph_edge_dataset_label (dataset_name, label_fraud, label_rule),"
                + "KEY idx_risk_graph_edge_from_node (from_node_id),"
                + "KEY idx_risk_graph_edge_to_node (to_node_id),"
                + "KEY idx_risk_graph_edge_time (event_time),"
                + "KEY idx_risk_graph_edge_type (edge_type),"
                + "KEY idx_risk_graph_edge_amount (amount),"
                + "CONSTRAINT fk_risk_graph_edge_batch "
                + "FOREIGN KEY (batch_id) REFERENCES t_risk_graph_dataset_batch (graph_batch_id),"
                + "CONSTRAINT fk_risk_graph_edge_from_node "
                + "FOREIGN KEY (from_node_id) REFERENCES t_risk_graph_node (graph_node_id),"
                + "CONSTRAINT fk_risk_graph_edge_to_node "
                + "FOREIGN KEY (to_node_id) REFERENCES t_risk_graph_node (graph_node_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void executeDdl(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("No generated key returned");
    }

    private String sourceEdgeId(String batchCode, GraphRow row) {
        return "GE" + sha256(batchCode + "|" + row.rowNo + "|" + row.fromExternalId + "|"
                + row.toExternalId + "|" + safeMoney(row.amount).toPlainString())
                .substring(0, 28).toUpperCase(Locale.ROOT);
    }

    private static void setNullableMoney(PreparedStatement statement, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, safeMoney(value));
        }
    }

    private static void setNullableTimestamp(PreparedStatement statement, int index, Timestamp value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, value);
        }
    }

    private static void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static BigDecimal safeMoney(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(String value) {
        String cleaned = cleanValue(value).replace(",", "");
        if (cleaned.length() == 0) {
            return ZERO;
        }
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer integerOrNull(String value) {
        String cleaned = cleanValue(value);
        if (cleaned.length() == 0) {
            return null;
        }
        return Integer.valueOf(cleaned);
    }

    private static Timestamp parseTimestamp(String value) {
        String cleaned = cleanValue(value);
        if (cleaned.length() == 0) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return Timestamp.valueOf(LocalDateTime.parse(cleaned, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next common dataset timestamp format.
            }
        }
        try {
            return Timestamp.valueOf(LocalDateTime.of(LocalDate.parse(cleaned), LocalTime.MIDNIGHT));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        while (cleaned.length() >= 2) {
            char first = cleaned.charAt(0);
            char last = cleaned.charAt(cleaned.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
            } else {
                break;
            }
        }
        return cleaned;
    }

    private static boolean truthy(String value) {
        String cleaned = cleanValue(value).toLowerCase(Locale.ROOT);
        return "1".equals(cleaned) || "true".equals(cleaned) || "yes".equals(cleaned)
                || "y".equals(cleaned) || "fraud".equals(cleaned) || "laundering".equals(cleaned);
    }

    private static String sanitizeCode(String value, String fallback, int maxLength) {
        String cleaned = cleanValue(value).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        boolean previousUnderscore = false;
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            boolean alphaNum = (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9');
            if (alphaNum) {
                builder.append(ch);
                previousUnderscore = false;
            } else if (!previousUnderscore && builder.length() > 0) {
                builder.append('_');
                previousUnderscore = true;
            }
            if (builder.length() >= maxLength) {
                break;
            }
        }
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '_') {
            builder.setLength(builder.length() - 1);
        }
        return builder.length() == 0 ? fallback : builder.toString();
    }

    private static String jsonOf(String[][] pairs) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (String[] pair : pairs) {
            if (pair.length < 2 || pair[0] == null || pair[1] == null) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(jsonEscape(pair[0])).append('"').append(':')
                    .append('"').append(jsonEscape(pair[1])).append('"');
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private static String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') {
                builder.append('\\').append(ch);
            } else if (ch == '\n') {
                builder.append("\\n");
            } else if (ch == '\r') {
                builder.append("\\r");
            } else if (ch == '\t') {
                builder.append("\\t");
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private enum SourceFormat {
        AUTO,
        IBM_AML,
        BANKSIM,
        PAYSIM;

        private String defaultDatasetName() {
            if (this == IBM_AML) {
                return "IBM_AML";
            }
            if (this == BANKSIM) {
                return "BANKSIM";
            }
            if (this == PAYSIM) {
                return "PAYSIM_GRAPH";
            }
            return "RISK_GRAPH";
        }

        private static SourceFormat parse(String value) {
            String code = sanitizeCode(value, "AUTO", 30);
            if ("IBM".equals(code) || "IBMAML".equals(code) || "IBM_AML".equals(code)
                    || "AML".equals(code)) {
                return IBM_AML;
            }
            if ("BANKSIM".equals(code) || "BANK_SIM".equals(code)) {
                return BANKSIM;
            }
            if ("PAYSIM".equals(code) || "PAYSIM_GRAPH".equals(code)) {
                return PAYSIM;
            }
            if ("AUTO".equals(code)) {
                return AUTO;
            }
            throw new IllegalArgumentException("Unknown --format: " + value);
        }
    }

    private static final class Options {
        private String file;
        private int limit = DEFAULT_LIMIT;
        private int commitSize = DEFAULT_COMMIT_SIZE;
        private String batchCode;
        private String datasetName;
        private SourceFormat sourceFormat = SourceFormat.AUTO;
        private LocalDate startDate = LocalDate.of(2026, 1, 1);
        private boolean dryRun;

        private static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--file".equals(arg)) {
                    options.file = requireValue(args, ++i, arg);
                } else if ("--format".equals(arg)) {
                    options.sourceFormat = SourceFormat.parse(requireValue(args, ++i, arg));
                } else if ("--dataset-name".equals(arg)) {
                    options.datasetName = sanitizeCode(requireValue(args, ++i, arg), "RISK_GRAPH", 60);
                } else if ("--limit".equals(arg)) {
                    options.limit = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--commit-size".equals(arg)) {
                    options.commitSize = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--batch-code".equals(arg)) {
                    options.batchCode = sanitizeCode(requireValue(args, ++i, arg), "RISK_GRAPH", 80);
                } else if ("--start-date".equals(arg)) {
                    options.startDate = LocalDate.parse(requireValue(args, ++i, arg));
                } else if ("--dry-run".equals(arg)) {
                    options.dryRun = true;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsage();
                    System.exit(0);
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (options.file == null || options.file.trim().length() == 0) {
                throw new IllegalArgumentException("--file is required.");
            }
            if (options.limit < 0) {
                throw new IllegalArgumentException("--limit must be >= 0. Use 0 to import all rows.");
            }
            if (options.commitSize <= 0) {
                throw new IllegalArgumentException("--commit-size must be > 0.");
            }
            return options;
        }

        private void resolveRuntimeDefaults(Header header) {
            if (sourceFormat == SourceFormat.AUTO) {
                sourceFormat = detectFormat(header);
            }
            if (datasetName == null) {
                datasetName = sourceFormat.defaultDatasetName();
            }
            if (batchCode == null) {
                batchCode = datasetName + "_" + LocalDateTime.now().format(BATCH_TIME_FORMATTER);
            }
        }

        private SourceFormat detectFormat(Header header) {
            if (header.hasAny("is laundering") && header.hasAny("from bank", "to bank")) {
                return SourceFormat.IBM_AML;
            }
            if (header.hasAny("merchant") && header.hasAny("fraud") && header.hasAny("customer")) {
                return SourceFormat.BANKSIM;
            }
            if (header.hasAny("nameOrig", "name orig") && header.hasAny("isFraud", "is fraud")) {
                return SourceFormat.PAYSIM;
            }
            throw new IllegalArgumentException("Cannot auto-detect graph dataset format. Use --format IBM_AML, "
                    + "BANKSIM, or PAYSIM.");
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value.");
            }
            return args[index];
        }

        private static void printUsage() {
            System.out.println("Usage:");
            System.out.println("  java -cp \"target/classes;target/dependency/*\" com.bank.tool.RiskGraphImporter "
                    + "--file <dataset.csv> [--format IBM_AML|BANKSIM|PAYSIM|AUTO] "
                    + "[--dataset-name IBM_AML] [--limit 100000] [--batch-code IBM_AML_100K_V1] "
                    + "[--start-date 2026-01-01] [--commit-size 1000] [--dry-run]");
            System.out.println();
            System.out.println("Notes:");
            System.out.println("  IBM_AML columns: Timestamp, From Bank, Account, To Bank, Account.1, "
                    + "Amount Received, Receiving Currency, Amount Paid, Payment Currency, Payment Format, "
                    + "Is Laundering.");
            System.out.println("  BANKSIM columns: step, customer, merchant, category, amount, fraud.");
            System.out.println("  PAYSIM columns: step, type, amount, nameOrig, nameDest, isFraud, isFlaggedFraud.");
            System.out.println("  --limit 0 imports all rows. Keep a smaller limit for the first local run.");
        }
    }

    private static final class Header {
        private final Map<String, Integer> indexes;

        private Header(Map<String, Integer> indexes) {
            this.indexes = indexes;
        }

        private static Header from(String headerLine) {
            List<String> values = parseCsvLine(headerLine);
            Map<String, Integer> indexes = new HashMap<String, Integer>();
            Map<String, Integer> seen = new HashMap<String, Integer>();
            for (int i = 0; i < values.size(); i++) {
                String normalized = normalize(values.get(i));
                int count = seen.containsKey(normalized) ? seen.get(normalized) + 1 : 1;
                seen.put(normalized, count);
                if (count == 1) {
                    indexes.put(normalized, i);
                } else {
                    indexes.put(normalized + count, i);
                }
            }
            return new Header(indexes);
        }

        private String value(List<String> row, String... columns) {
            for (String column : columns) {
                Integer index = indexes.get(normalize(column));
                if (index != null && index < row.size()) {
                    return row.get(index);
                }
            }
            return "";
        }

        private boolean hasAny(String... columns) {
            for (String column : columns) {
                if (indexes.containsKey(normalize(column))) {
                    return true;
                }
            }
            return false;
        }

        private static String normalize(String value) {
            String source = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                    builder.append(ch);
                }
            }
            return builder.toString();
        }
    }

    private static final class GraphRow {
        private int rowNo;
        private Integer step;
        private String fromExternalId;
        private String toExternalId;
        private String fromNodeType;
        private String toNodeType;
        private String fromDisplayName;
        private String toDisplayName;
        private String edgeType;
        private BigDecimal amount;
        private String currency;
        private BigDecimal paidAmount;
        private String paidCurrency;
        private BigDecimal receivedAmount;
        private String receivedCurrency;
        private Timestamp eventTime;
        private boolean fraud;
        private boolean flagged;
        private String labelSource;
        private String typology;
        private String featureJson;
        private String fromFeatureJson;
        private String toFeatureJson;

        private static GraphRow parse(int rowNo, String line, Header header, Options options) {
            List<String> values = parseCsvLine(line);
            if (options.sourceFormat == SourceFormat.IBM_AML) {
                return parseIbmAml(rowNo, values, header);
            }
            if (options.sourceFormat == SourceFormat.BANKSIM) {
                return parseBankSim(rowNo, values, header, options);
            }
            if (options.sourceFormat == SourceFormat.PAYSIM) {
                return parsePaySim(rowNo, values, header, options);
            }
            throw new IllegalArgumentException("Unsupported graph format: " + options.sourceFormat);
        }

        private static GraphRow parseIbmAml(int rowNo, List<String> values, Header header) {
            String timestamp = cleanValue(header.value(values, "Timestamp", "Time"));
            String fromBank = cleanValue(header.value(values, "From Bank", "FromBank", "Source Bank"));
            String fromAccount = cleanValue(header.value(values, "Account", "From Account", "FromAccount"));
            String toBank = cleanValue(header.value(values, "To Bank", "ToBank", "Target Bank"));
            String toAccount = cleanValue(header.value(values, "Account.1", "Account1", "Account2",
                    "To Account", "ToAccount", "account2"));
            String paymentFormat = cleanValue(header.value(values, "Payment Format", "PaymentFormat", "Format"));
            BigDecimal amountReceived = money(header.value(values, "Amount Received", "AmountReceived"));
            String receivingCurrency = cleanValue(header.value(values, "Receiving Currency", "ReceivingCurrency"));
            BigDecimal amountPaid = money(header.value(values, "Amount Paid", "AmountPaid"));
            String paymentCurrency = cleanValue(header.value(values, "Payment Currency", "PaymentCurrency"));
            boolean laundering = truthy(header.value(values, "Is Laundering", "IsLaundering", "Laundering"));

            GraphRow row = base(rowNo);
            row.fromExternalId = requireNodeId("IBM:" + fromBank + ":" + fromAccount, "IBM AML origin");
            row.toExternalId = requireNodeId("IBM:" + toBank + ":" + toAccount, "IBM AML destination");
            row.fromNodeType = "ACCOUNT";
            row.toNodeType = "ACCOUNT";
            row.fromDisplayName = fromBank + ":" + fromAccount;
            row.toDisplayName = toBank + ":" + toAccount;
            row.edgeType = sanitizeCode(paymentFormat, "TRANSFER", 60);
            row.amount = amountPaid.compareTo(ZERO) > 0 ? amountPaid : amountReceived;
            row.currency = paymentCurrency.length() > 0 ? paymentCurrency : receivingCurrency;
            row.paidAmount = amountPaid;
            row.paidCurrency = paymentCurrency;
            row.receivedAmount = amountReceived;
            row.receivedCurrency = receivingCurrency;
            row.eventTime = parseTimestamp(timestamp);
            row.fraud = laundering;
            row.labelSource = "IBM_AML";
            row.typology = laundering ? "MONEY_LAUNDERING" : null;
            row.featureJson = jsonOf(new String[][] {
                    {"timestamp", timestamp},
                    {"from_bank", fromBank},
                    {"to_bank", toBank},
                    {"payment_format", paymentFormat},
                    {"amount_paid", amountPaid.toPlainString()},
                    {"payment_currency", paymentCurrency},
                    {"amount_received", amountReceived.toPlainString()},
                    {"receiving_currency", receivingCurrency}
            });
            row.fromFeatureJson = jsonOf(new String[][] {{"bank", fromBank}, {"account", fromAccount}});
            row.toFeatureJson = jsonOf(new String[][] {{"bank", toBank}, {"account", toAccount}});
            validate(row);
            return row;
        }

        private static GraphRow parseBankSim(int rowNo, List<String> values, Header header, Options options) {
            Integer step = integerOrNull(header.value(values, "step"));
            String customer = cleanValue(header.value(values, "customer"));
            String merchant = cleanValue(header.value(values, "merchant"));
            String age = cleanValue(header.value(values, "age"));
            String gender = cleanValue(header.value(values, "gender"));
            String zipcodeOri = cleanValue(header.value(values, "zipcodeOri", "zipcode_ori"));
            String zipMerchant = cleanValue(header.value(values, "zipMerchant", "zip_merchant"));
            String category = cleanValue(header.value(values, "category"));
            BigDecimal amount = money(header.value(values, "amount"));
            boolean fraud = truthy(header.value(values, "fraud"));

            GraphRow row = base(rowNo);
            row.step = step;
            row.fromExternalId = requireNodeId("BANKSIM:CUSTOMER:" + customer, "BankSim customer");
            row.toExternalId = requireNodeId("BANKSIM:MERCHANT:" + merchant, "BankSim merchant");
            row.fromNodeType = "CUSTOMER";
            row.toNodeType = "MERCHANT";
            row.fromDisplayName = customer;
            row.toDisplayName = merchant;
            row.edgeType = sanitizeCode(category, "PAYMENT", 60);
            row.amount = amount;
            row.currency = "SYN";
            if (step != null) {
                row.eventTime = Timestamp.valueOf(options.startDate.atStartOfDay().plusDays(Math.max(0, step - 1)));
            }
            row.fraud = fraud;
            row.labelSource = "BANKSIM";
            row.typology = category;
            row.featureJson = jsonOf(new String[][] {
                    {"step", step == null ? "" : String.valueOf(step)},
                    {"age", age},
                    {"gender", gender},
                    {"customer_zip", zipcodeOri},
                    {"merchant_zip", zipMerchant},
                    {"category", category}
            });
            row.fromFeatureJson = jsonOf(new String[][] {
                    {"age", age},
                    {"gender", gender},
                    {"zipcode", zipcodeOri}
            });
            row.toFeatureJson = jsonOf(new String[][] {
                    {"zipcode", zipMerchant},
                    {"category", category}
            });
            validate(row);
            return row;
        }

        private static GraphRow parsePaySim(int rowNo, List<String> values, Header header, Options options) {
            Integer step = integerOrNull(header.value(values, "step"));
            String type = cleanValue(header.value(values, "type"));
            BigDecimal amount = money(header.value(values, "amount"));
            String nameOrig = cleanValue(header.value(values, "nameOrig", "name orig"));
            String nameDest = cleanValue(header.value(values, "nameDest", "name dest"));
            BigDecimal oldBalanceOrigin = money(header.value(values, "oldbalanceOrg", "old balance origin"));
            BigDecimal newBalanceOrigin = money(header.value(values, "newbalanceOrig", "new balance origin"));
            BigDecimal oldBalanceDest = money(header.value(values, "oldbalanceDest", "old balance dest"));
            BigDecimal newBalanceDest = money(header.value(values, "newbalanceDest", "new balance dest"));
            boolean fraud = truthy(header.value(values, "isFraud", "is fraud"));
            boolean flagged = truthy(header.value(values, "isFlaggedFraud", "is flagged fraud"));

            GraphRow row = base(rowNo);
            row.step = step;
            row.fromExternalId = requireNodeId("PAYSIM:" + nameOrig, "PaySim origin");
            row.toExternalId = requireNodeId("PAYSIM:" + nameDest, "PaySim destination");
            row.fromNodeType = "ACCOUNT";
            row.toNodeType = nameDest.startsWith("M") ? "MERCHANT" : "ACCOUNT";
            row.fromDisplayName = nameOrig;
            row.toDisplayName = nameDest;
            row.edgeType = sanitizeCode(type, "TRANSFER", 60);
            row.amount = amount;
            row.currency = "SYN";
            if (step != null) {
                row.eventTime = Timestamp.valueOf(options.startDate.atStartOfDay()
                        .plusHours(Math.max(0, step - 1))
                        .plusSeconds(rowNo % 3600));
            }
            row.fraud = fraud;
            row.flagged = flagged;
            row.labelSource = "PAYSIM";
            row.typology = type;
            row.featureJson = jsonOf(new String[][] {
                    {"step", step == null ? "" : String.valueOf(step)},
                    {"type", type},
                    {"old_balance_origin", oldBalanceOrigin.toPlainString()},
                    {"new_balance_origin", newBalanceOrigin.toPlainString()},
                    {"old_balance_dest", oldBalanceDest.toPlainString()},
                    {"new_balance_dest", newBalanceDest.toPlainString()}
            });
            row.fromFeatureJson = jsonOf(new String[][] {
                    {"external_id", nameOrig},
                    {"synthetic_role", "origin"}
            });
            row.toFeatureJson = jsonOf(new String[][] {
                    {"external_id", nameDest},
                    {"synthetic_role", "destination"}
            });
            validate(row);
            return row;
        }

        private static GraphRow base(int rowNo) {
            GraphRow row = new GraphRow();
            row.rowNo = rowNo;
            row.amount = ZERO;
            row.edgeType = "TRANSFER";
            row.fromNodeType = "ACCOUNT";
            row.toNodeType = "ACCOUNT";
            row.currency = null;
            row.labelSource = "EXTERNAL";
            row.featureJson = "{}";
            row.fromFeatureJson = "{}";
            row.toFeatureJson = "{}";
            return row;
        }

        private static String requireNodeId(String value, String label) {
            String cleaned = cleanValue(value);
            if (cleaned.length() == 0 || cleaned.endsWith(":")) {
                throw new IllegalArgumentException(label + " is missing");
            }
            return abbreviate(cleaned, 180);
        }

        private static void validate(GraphRow row) {
            if (row.fromExternalId == null || row.fromExternalId.length() == 0
                    || row.toExternalId == null || row.toExternalId.length() == 0) {
                throw new IllegalArgumentException("missing graph endpoints");
            }
            if (row.amount == null) {
                row.amount = ZERO;
            }
            row.amount = safeMoney(row.amount);
        }
    }

    private static final class NodeRef {
        private final long nodeId;

        private NodeRef(long nodeId) {
            this.nodeId = nodeId;
        }
    }

    private static final class ImportStats {
        private int readRows;
        private int importedEdges;
        private int skippedRows;
        private int fraudEdges;
        private int flaggedEdges;
        private int touchedNodes;
    }

    private static final class BatchTotals {
        private int nodeRows;
        private int edgeRows;
        private int normalEdges;
        private int fraudEdges;
        private int flaggedEdges;
    }
}
