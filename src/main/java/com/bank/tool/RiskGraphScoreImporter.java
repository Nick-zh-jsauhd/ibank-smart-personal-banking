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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiskGraphScoreImporter {
    private static final int DEFAULT_COMMIT_SIZE = 1000;
    private static final DateTimeFormatter DEFAULT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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

        RiskGraphScoreImporter importer = new RiskGraphScoreImporter();
        try {
            ImportStats stats = importer.importFile(options);
            System.out.println("Risk graph score import completed.");
            System.out.println("File: " + options.file);
            System.out.println("Model version override: " + text(options.modelVersion));
            System.out.println("Feature version override: " + text(options.featureVersion));
            System.out.println("Read rows: " + stats.readRows);
            System.out.println("Imported rows: " + stats.importedRows);
            System.out.println("Skipped rows: " + stats.skippedRows);
            System.out.println("PASS rows: " + stats.passRows);
            System.out.println("REVIEW rows: " + stats.reviewRows);
            System.out.println("BLOCK rows: " + stats.blockRows);
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
            throw new IllegalArgumentException("Score CSV not found: " + csvPath);
        }

        ImportStats stats = new ImportStats();
        if (options.dryRun) {
            dryRun(csvPath, options, stats);
            return stats;
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            ensureRiskGraphScoreTable(connection);
            readAndImport(connection, csvPath, options, stats);
            connection.commit();
            return stats;
        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (connection != null) {
                connection.setAutoCommit(oldAutoCommit);
                connection.close();
            }
        }
    }

    private void dryRun(Path csvPath, Options options, ImportStats stats) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }
            Header header = Header.from(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (options.limit > 0 && stats.readRows >= options.limit) {
                    break;
                }
                stats.readRows++;
                try {
                    ScoreRow row = ScoreRow.parse(line, header, options);
                    stats.importedRows++;
                    stats.countDecision(row.decision);
                } catch (RuntimeException e) {
                    stats.skippedRows++;
                }
            }
        }
    }

    private void readAndImport(Connection connection, Path csvPath, Options options, ImportStats stats)
            throws IOException, SQLException {
        String sql = "INSERT INTO t_risk_graph_model_score (graph_edge_id, model_version, feature_version, "
                + "risk_score, risk_probability, decision, review_threshold, block_threshold, reason_json, "
                + "scored_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE feature_version = VALUES(feature_version), "
                + "risk_score = VALUES(risk_score), risk_probability = VALUES(risk_probability), "
                + "decision = VALUES(decision), review_threshold = VALUES(review_threshold), "
                + "block_threshold = VALUES(block_threshold), reason_json = VALUES(reason_json), "
                + "scored_at = NOW(), updated_at = NOW()";

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }
            Header header = Header.from(headerLine);
            String line;
            int pending = 0;
            while ((line = reader.readLine()) != null) {
                if (options.limit > 0 && stats.readRows >= options.limit) {
                    break;
                }
                stats.readRows++;
                try {
                    ScoreRow row = ScoreRow.parse(line, header, options);
                    bind(statement, row);
                    statement.addBatch();
                    stats.importedRows++;
                    stats.countDecision(row.decision);
                    pending++;
                } catch (RuntimeException e) {
                    stats.skippedRows++;
                    System.err.println("Skip score row " + stats.readRows + ": " + e.getMessage());
                }
                if (pending >= options.commitSize) {
                    statement.executeBatch();
                    connection.commit();
                    pending = 0;
                }
            }
            if (pending > 0) {
                statement.executeBatch();
            }
        }
    }

    private void bind(PreparedStatement statement, ScoreRow row) throws SQLException {
        statement.setLong(1, row.graphEdgeId);
        statement.setString(2, row.modelVersion);
        statement.setString(3, row.featureVersion);
        statement.setInt(4, row.riskScore);
        statement.setBigDecimal(5, row.riskProbability);
        statement.setString(6, row.decision);
        setNullableDecimal(statement, 7, row.reviewThreshold);
        setNullableDecimal(statement, 8, row.blockThreshold);
        statement.setString(9, row.reasonJson);
    }

    private void ensureRiskGraphScoreTable(Connection connection) throws SQLException {
        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_graph_model_score ("
                + "score_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "graph_edge_id BIGINT NOT NULL,"
                + "model_version VARCHAR(80) NOT NULL,"
                + "feature_version VARCHAR(80) NOT NULL,"
                + "risk_score INT NOT NULL,"
                + "risk_probability DECIMAL(10,8) NOT NULL,"
                + "decision VARCHAR(30) NOT NULL,"
                + "review_threshold DECIMAL(10,8) NULL,"
                + "block_threshold DECIMAL(10,8) NULL,"
                + "reason_json TEXT NULL,"
                + "scored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uk_risk_graph_model_edge_version (graph_edge_id, model_version),"
                + "KEY idx_risk_graph_model_version_decision (model_version, decision),"
                + "KEY idx_risk_graph_model_score (model_version, risk_score),"
                + "CONSTRAINT fk_risk_graph_model_edge "
                + "FOREIGN KEY (graph_edge_id) REFERENCES t_risk_graph_edge (graph_edge_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void executeDdl(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void setNullableDecimal(PreparedStatement statement, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value.setScale(8, RoundingMode.HALF_UP));
        }
    }

    private static String text(String value) {
        return value == null ? "(from CSV)" : value;
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

    private static final class Options {
        private String file;
        private String modelVersion;
        private String featureVersion;
        private int limit;
        private int commitSize = DEFAULT_COMMIT_SIZE;
        private boolean dryRun;

        private static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--file".equals(arg)) {
                    options.file = requireValue(args, ++i, arg);
                } else if ("--model-version".equals(arg)) {
                    options.modelVersion = requireValue(args, ++i, arg);
                } else if ("--feature-version".equals(arg)) {
                    options.featureVersion = requireValue(args, ++i, arg);
                } else if ("--limit".equals(arg)) {
                    options.limit = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--commit-size".equals(arg)) {
                    options.commitSize = Integer.parseInt(requireValue(args, ++i, arg));
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
            if (options.modelVersion == null) {
                options.modelVersion = null;
            }
            if (options.limit < 0) {
                throw new IllegalArgumentException("--limit must be >= 0. Use 0 to import all rows.");
            }
            if (options.commitSize <= 0) {
                throw new IllegalArgumentException("--commit-size must be > 0.");
            }
            return options;
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value.");
            }
            return args[index];
        }

        private static void printUsage() {
            System.out.println("Usage:");
            System.out.println("  java -cp \"target/classes;target/dependency/*\" com.bank.tool.RiskGraphScoreImporter "
                    + "--file target/riskbrain-server-results/graphsage_full_v2/edge_scores.csv "
                    + "[--model-version riskbrain-graphsage-ibm-aml-v1] "
                    + "[--feature-version riskgraph-v1] [--limit 0] [--commit-size 1000] [--dry-run]");
            System.out.println("Default model version is read from CSV. Use --model-version to override.");
            System.out.println("Run id suggestion: riskbrain-graphscore-" + LocalDateTime.now().format(DEFAULT_TIME_FORMATTER));
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
            for (int i = 0; i < values.size(); i++) {
                indexes.put(values.get(i).trim().toLowerCase(Locale.ROOT), i);
            }
            require(indexes, "graph_edge_id");
            require(indexes, "model_version");
            require(indexes, "feature_version");
            require(indexes, "risk_probability");
            require(indexes, "risk_score");
            require(indexes, "decision");
            return new Header(indexes);
        }

        private static void require(Map<String, Integer> indexes, String name) {
            if (!indexes.containsKey(name)) {
                throw new IllegalArgumentException("Score CSV missing column: " + name);
            }
        }

        private String value(List<String> row, String column) {
            Integer index = indexes.get(column.toLowerCase(Locale.ROOT));
            if (index == null || index >= row.size()) {
                return "";
            }
            return row.get(index);
        }
    }

    private static final class ScoreRow {
        private long graphEdgeId;
        private String modelVersion;
        private String featureVersion;
        private int riskScore;
        private BigDecimal riskProbability;
        private String decision;
        private BigDecimal reviewThreshold;
        private BigDecimal blockThreshold;
        private String reasonJson;

        private static ScoreRow parse(String line, Header header, Options options) {
            List<String> values = parseCsvLine(line);
            ScoreRow row = new ScoreRow();
            row.graphEdgeId = Long.parseLong(header.value(values, "graph_edge_id").trim());
            row.modelVersion = nonEmpty(options.modelVersion, header.value(values, "model_version"));
            row.featureVersion = nonEmpty(options.featureVersion, header.value(values, "feature_version"));
            row.riskProbability = decimal(header.value(values, "risk_probability"), 8);
            row.riskScore = Integer.parseInt(header.value(values, "risk_score").trim());
            row.decision = nonEmpty(null, header.value(values, "decision")).toUpperCase(Locale.ROOT);
            row.reviewThreshold = optionalDecimal(header.value(values, "review_threshold"));
            row.blockThreshold = optionalDecimal(header.value(values, "block_threshold"));
            row.reasonJson = header.value(values, "reason_json");
            if (row.modelVersion.length() == 0 || row.featureVersion.length() == 0 || row.decision.length() == 0) {
                throw new IllegalArgumentException("missing model_version, feature_version or decision");
            }
            return row;
        }

        private static String nonEmpty(String override, String fallback) {
            if (override != null && override.trim().length() > 0) {
                return override.trim();
            }
            return fallback == null ? "" : fallback.trim();
        }

        private static BigDecimal optionalDecimal(String value) {
            if (value == null || value.trim().length() == 0) {
                return null;
            }
            return decimal(value, 8);
        }

        private static BigDecimal decimal(String value, int scale) {
            return new BigDecimal(value.trim()).setScale(scale, RoundingMode.HALF_UP);
        }
    }

    private static final class ImportStats {
        private int readRows;
        private int importedRows;
        private int skippedRows;
        private int passRows;
        private int reviewRows;
        private int blockRows;

        private void countDecision(String decision) {
            if ("PASS".equals(decision)) {
                passRows++;
            } else if ("REVIEW".equals(decision)) {
                reviewRows++;
            } else if ("BLOCK".equals(decision)) {
                blockRows++;
            }
        }
    }
}
