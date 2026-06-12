package com.bank.tool;

import com.bank.util.DBUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RiskGraphExporter {
    private static final DateTimeFormatter DEFAULT_DIR_TIME =
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

        RiskGraphExporter exporter = new RiskGraphExporter();
        try {
            ExportStats stats = exporter.export(options);
            System.out.println("Risk graph export completed.");
            System.out.println("Batch code: " + options.batchCode);
            System.out.println("Output dir: " + options.outputDir.toAbsolutePath().normalize());
            System.out.println("Nodes: " + stats.nodes);
            System.out.println("Edges: " + stats.edges);
            System.out.println("Labels: " + stats.labels);
            System.out.println("V2 edge features: " + stats.edgeFeaturesV2);
            System.out.println("Feedback labels: " + stats.feedbackLabels);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private ExportStats export(Options options) throws SQLException, IOException {
        Files.createDirectories(options.outputDir);
        ExportStats stats = new ExportStats();
        try (Connection connection = DBUtil.getConnection()) {
            long batchId = findBatchId(connection, options.batchCode);
            if (options.feedbackModelVersion == null) {
                options.feedbackModelVersion = findLatestFeedbackModelVersion(connection, batchId);
            }
            exportNodes(connection, batchId, options.outputDir.resolve("nodes.csv"), stats);
            exportEdges(connection, batchId, options.outputDir.resolve("edges.csv"), stats);
            exportLabels(connection, batchId, options.outputDir.resolve("labels.csv"), stats);
            exportEdgeFeaturesV2(connection, batchId, options.feedbackModelVersion,
                    options.outputDir.resolve("edge_features_v2.csv"), stats);
            exportFeedbackLabels(connection, batchId, options.feedbackModelVersion,
                    options.outputDir.resolve("feedback_labels.csv"), stats);
            exportMeta(options.outputDir.resolve("graph_meta.json"), options, stats);
        }
        return stats;
    }

    private long findBatchId(Connection connection, String batchCode) throws SQLException {
        String sql = "SELECT graph_batch_id FROM t_risk_graph_dataset_batch WHERE batch_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, batchCode);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("graph_batch_id");
                }
            }
        }
        throw new IllegalArgumentException("Graph batch not found: " + batchCode);
    }

    private String findLatestFeedbackModelVersion(Connection connection, long batchId) throws SQLException {
        String sql = "SELECT rc.model_version FROM t_risk_graph_review_case rc "
                + "JOIN t_risk_graph_edge e ON rc.graph_edge_id = e.graph_edge_id "
                + "WHERE e.batch_id = ? GROUP BY rc.model_version ORDER BY MAX(rc.updated_at) DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("model_version");
                }
            }
        }
        return null;
    }

    private void exportNodes(Connection connection, long batchId, Path outputFile, ExportStats stats)
            throws SQLException, IOException {
        String sql = "SELECT n.graph_node_id, n.dataset_name, n.external_node_id, n.node_type, "
                + "n.in_degree, n.out_degree, n.fraud_in_degree, n.fraud_out_degree, "
                + "n.total_in_amount, n.total_out_amount "
                + "FROM t_risk_graph_node n JOIN ("
                + "  SELECT from_node_id AS node_id FROM t_risk_graph_edge WHERE batch_id = ? "
                + "  UNION SELECT to_node_id AS node_id FROM t_risk_graph_edge WHERE batch_id = ?"
                + ") x ON n.graph_node_id = x.node_id "
                + "ORDER BY n.graph_node_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            statement.setLong(1, batchId);
            statement.setLong(2, batchId);
            writer.write("node_id,dataset_name,external_node_id,node_type,in_degree,out_degree,"
                    + "fraud_in_degree,fraud_out_degree,total_in_amount,total_out_amount");
            writer.newLine();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    writer.write(csv(rs.getLong("graph_node_id")));
                    writer.write(',');
                    writer.write(csv(rs.getString("dataset_name")));
                    writer.write(',');
                    writer.write(csv(rs.getString("external_node_id")));
                    writer.write(',');
                    writer.write(csv(rs.getString("node_type")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("in_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("out_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("fraud_in_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("fraud_out_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("total_in_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("total_out_amount")));
                    writer.newLine();
                    stats.nodes++;
                }
            }
        }
    }

    private void exportEdges(Connection connection, long batchId, Path outputFile, ExportStats stats)
            throws SQLException, IOException {
        String sql = "SELECT e.graph_edge_id, b.batch_code, e.dataset_name, e.source_row_no, "
                + "e.from_node_id, e.to_node_id, e.edge_type, e.amount, e.currency, e.event_time, "
                + "e.source_step, e.label_fraud, e.label_rule, e.label_source, e.typology "
                + "FROM t_risk_graph_edge e JOIN t_risk_graph_dataset_batch b ON e.batch_id = b.graph_batch_id "
                + "WHERE e.batch_id = ? ORDER BY e.graph_edge_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            statement.setLong(1, batchId);
            writer.write("edge_id,batch_code,dataset_name,source_row_no,from_node_id,to_node_id,edge_type,"
                    + "amount,currency,event_time,source_step,label_fraud,label_rule,label_source,typology");
            writer.newLine();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    writer.write(csv(rs.getLong("graph_edge_id")));
                    writer.write(',');
                    writer.write(csv(rs.getString("batch_code")));
                    writer.write(',');
                    writer.write(csv(rs.getString("dataset_name")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("source_row_no")));
                    writer.write(',');
                    writer.write(csv(rs.getLong("from_node_id")));
                    writer.write(',');
                    writer.write(csv(rs.getLong("to_node_id")));
                    writer.write(',');
                    writer.write(csv(rs.getString("edge_type")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("amount")));
                    writer.write(',');
                    writer.write(csv(rs.getString("currency")));
                    writer.write(',');
                    writer.write(csv(rs.getTimestamp("event_time")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("source_step")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("label_fraud")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("label_rule")));
                    writer.write(',');
                    writer.write(csv(rs.getString("label_source")));
                    writer.write(',');
                    writer.write(csv(rs.getString("typology")));
                    writer.newLine();
                    stats.edges++;
                }
            }
        }
    }

    private void exportLabels(Connection connection, long batchId, Path outputFile, ExportStats stats)
            throws SQLException, IOException {
        String sql = "SELECT graph_edge_id, label_fraud, label_rule, label_source "
                + "FROM t_risk_graph_edge WHERE batch_id = ? ORDER BY graph_edge_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            statement.setLong(1, batchId);
            writer.write("edge_id,label_fraud,label_rule,label_source");
            writer.newLine();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    writer.write(csv(rs.getLong("graph_edge_id")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("label_fraud")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("label_rule")));
                    writer.write(',');
                    writer.write(csv(rs.getString("label_source")));
                    writer.newLine();
                    stats.labels++;
                }
            }
        }
    }

    private void exportEdgeFeaturesV2(Connection connection, long batchId, String feedbackModelVersion,
                                      Path outputFile, ExportStats stats) throws SQLException, IOException {
        String sql = "SELECT e.graph_edge_id, "
                + "src.in_degree AS source_in_degree, src.out_degree AS source_out_degree, "
                + "src.total_in_amount AS source_total_in_amount, src.total_out_amount AS source_total_out_amount, "
                + "dst.in_degree AS target_in_degree, dst.out_degree AS target_out_degree, "
                + "dst.total_in_amount AS target_total_in_amount, dst.total_out_amount AS target_total_out_amount, "
                + "COALESCE(src_batch.edge_count, 0) AS source_batch_edge_count, "
                + "COALESCE(src_batch.total_amount, 0) AS source_batch_total_amount, "
                + "COALESCE(src_batch.avg_amount, 0) AS source_batch_avg_amount, "
                + "COALESCE(dst_batch.edge_count, 0) AS target_batch_edge_count, "
                + "COALESCE(dst_batch.total_amount, 0) AS target_batch_total_amount, "
                + "COALESCE(dst_batch.avg_amount, 0) AS target_batch_avg_amount, "
                + "CASE WHEN e.from_node_id = e.to_node_id THEN 1 ELSE 0 END AS self_loop, "
                + "HOUR(e.event_time) AS hour_of_day, DAYOFWEEK(e.event_time) AS day_of_week, "
                + "CASE WHEN DAYOFWEEK(e.event_time) IN (1, 7) THEN 1 ELSE 0 END AS is_weekend, "
                + "CASE WHEN HOUR(e.event_time) >= 23 OR HOUR(e.event_time) < 6 THEN 1 ELSE 0 END AS is_night, "
                + "rc.review_result, "
                + "CASE WHEN rc.review_result = 'CONFIRMED_RISK' THEN 1 "
                + "WHEN rc.review_result = 'FALSE_POSITIVE' THEN 0 ELSE NULL END AS feedback_label, "
                + "CASE WHEN rc.review_result = 'CONFIRMED_RISK' THEN 3.0 "
                + "WHEN rc.review_result = 'FALSE_POSITIVE' THEN 3.0 "
                + "WHEN rc.review_result = 'NEED_MORE_DATA' THEN 1.5 "
                + "WHEN rc.review_result = 'IGNORE' THEN 0.0 ELSE 1.0 END AS feedback_weight "
                + "FROM t_risk_graph_edge e "
                + "JOIN t_risk_graph_node src ON e.from_node_id = src.graph_node_id "
                + "JOIN t_risk_graph_node dst ON e.to_node_id = dst.graph_node_id "
                + "LEFT JOIN ("
                + "  SELECT node_id, COUNT(*) AS edge_count, SUM(amount) AS total_amount, AVG(amount) AS avg_amount "
                + "  FROM ("
                + "    SELECT from_node_id AS node_id, amount FROM t_risk_graph_edge WHERE batch_id = ? "
                + "    UNION ALL SELECT to_node_id AS node_id, amount FROM t_risk_graph_edge WHERE batch_id = ?"
                + "  ) node_edges GROUP BY node_id"
                + ") src_batch ON e.from_node_id = src_batch.node_id "
                + "LEFT JOIN ("
                + "  SELECT node_id, COUNT(*) AS edge_count, SUM(amount) AS total_amount, AVG(amount) AS avg_amount "
                + "  FROM ("
                + "    SELECT from_node_id AS node_id, amount FROM t_risk_graph_edge WHERE batch_id = ? "
                + "    UNION ALL SELECT to_node_id AS node_id, amount FROM t_risk_graph_edge WHERE batch_id = ?"
                + "  ) node_edges GROUP BY node_id"
                + ") dst_batch ON e.to_node_id = dst_batch.node_id "
                + "LEFT JOIN t_risk_graph_review_case rc ON e.graph_edge_id = rc.graph_edge_id "
                + "AND (? IS NULL OR rc.model_version = ?) "
                + "WHERE e.batch_id = ? ORDER BY e.graph_edge_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            statement.setLong(1, batchId);
            statement.setLong(2, batchId);
            statement.setLong(3, batchId);
            statement.setLong(4, batchId);
            statement.setString(5, feedbackModelVersion);
            statement.setString(6, feedbackModelVersion);
            statement.setLong(7, batchId);
            writer.write("edge_id,source_in_degree,source_out_degree,source_total_in_amount,"
                    + "source_total_out_amount,target_in_degree,target_out_degree,target_total_in_amount,"
                    + "target_total_out_amount,source_batch_edge_count,source_batch_total_amount,"
                    + "source_batch_avg_amount,target_batch_edge_count,target_batch_total_amount,"
                    + "target_batch_avg_amount,self_loop,hour_of_day,day_of_week,is_weekend,is_night,"
                    + "review_result,feedback_label,feedback_weight");
            writer.newLine();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    writer.write(csv(rs.getLong("graph_edge_id")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("source_in_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("source_out_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("source_total_in_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("source_total_out_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("target_in_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("target_out_degree")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("target_total_in_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("target_total_out_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("source_batch_edge_count")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("source_batch_total_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("source_batch_avg_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("target_batch_edge_count")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("target_batch_total_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getBigDecimal("target_batch_avg_amount")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("self_loop")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("hour_of_day")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("day_of_week")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("is_weekend")));
                    writer.write(',');
                    writer.write(csv(rs.getInt("is_night")));
                    writer.write(',');
                    writer.write(csv(rs.getString("review_result")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("feedback_label")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("feedback_weight")));
                    writer.newLine();
                    stats.edgeFeaturesV2++;
                }
            }
        }
    }

    private void exportFeedbackLabels(Connection connection, long batchId, String feedbackModelVersion,
                                      Path outputFile, ExportStats stats) throws SQLException, IOException {
        String sql = "SELECT rc.graph_edge_id, rc.model_version, rc.case_type, rc.case_status, "
                + "rc.review_result, rc.review_note, rc.reviewed_at, "
                + "CASE WHEN rc.review_result = 'CONFIRMED_RISK' THEN 1 "
                + "WHEN rc.review_result = 'FALSE_POSITIVE' THEN 0 ELSE NULL END AS feedback_label, "
                + "CASE WHEN rc.review_result = 'CONFIRMED_RISK' THEN 3.0 "
                + "WHEN rc.review_result = 'FALSE_POSITIVE' THEN 3.0 "
                + "WHEN rc.review_result = 'NEED_MORE_DATA' THEN 1.5 "
                + "WHEN rc.review_result = 'IGNORE' THEN 0.0 ELSE 1.0 END AS feedback_weight "
                + "FROM t_risk_graph_review_case rc "
                + "JOIN t_risk_graph_edge e ON rc.graph_edge_id = e.graph_edge_id "
                + "WHERE e.batch_id = ? AND (? IS NULL OR rc.model_version = ?) "
                + "ORDER BY rc.graph_edge_id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            statement.setLong(1, batchId);
            statement.setString(2, feedbackModelVersion);
            statement.setString(3, feedbackModelVersion);
            writer.write("edge_id,model_version,case_type,case_status,review_result,feedback_label,"
                    + "feedback_weight,reviewed_at,review_note");
            writer.newLine();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    writer.write(csv(rs.getLong("graph_edge_id")));
                    writer.write(',');
                    writer.write(csv(rs.getString("model_version")));
                    writer.write(',');
                    writer.write(csv(rs.getString("case_type")));
                    writer.write(',');
                    writer.write(csv(rs.getString("case_status")));
                    writer.write(',');
                    writer.write(csv(rs.getString("review_result")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("feedback_label")));
                    writer.write(',');
                    writer.write(csv(rs.getObject("feedback_weight")));
                    writer.write(',');
                    writer.write(csv(rs.getTimestamp("reviewed_at")));
                    writer.write(',');
                    writer.write(csv(rs.getString("review_note")));
                    writer.newLine();
                    stats.feedbackLabels++;
                }
            }
        }
    }

    private void exportMeta(Path outputFile, Options options, ExportStats stats) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"batch_code\": \"" + json(options.batchCode) + "\",\n");
            writer.write("  \"nodes\": " + stats.nodes + ",\n");
            writer.write("  \"edges\": " + stats.edges + ",\n");
            writer.write("  \"labels\": " + stats.labels + ",\n");
            writer.write("  \"edge_features_v2\": " + stats.edgeFeaturesV2 + ",\n");
            writer.write("  \"feedback_labels\": " + stats.feedbackLabels + ",\n");
            writer.write("  \"feedback_model_version\": \"" + json(options.feedbackModelVersion) + "\"\n");
            writer.write("}\n");
        }
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp) {
            return csv(value.toString());
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        String text = String.valueOf(value);
        boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0;
        if (!quote) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class Options {
        private String batchCode;
        private Path outputDir;
        private String feedbackModelVersion;

        private static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--batch-code".equals(arg)) {
                    options.batchCode = requireValue(args, ++i, arg);
                } else if ("--output-dir".equals(arg)) {
                    options.outputDir = Paths.get(requireValue(args, ++i, arg));
                } else if ("--feedback-model-version".equals(arg)) {
                    options.feedbackModelVersion = requireValue(args, ++i, arg);
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsage();
                    System.exit(0);
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (options.batchCode == null || options.batchCode.trim().length() == 0) {
                throw new IllegalArgumentException("--batch-code is required.");
            }
            if (options.outputDir == null) {
                options.outputDir = Paths.get("target", "risk-graph-export",
                        options.batchCode + "_" + LocalDateTime.now().format(DEFAULT_DIR_TIME));
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
            System.out.println("  java -cp \"target/classes;target/dependency/*\" com.bank.tool.RiskGraphExporter "
                    + "--batch-code IBM_AML_HI_SMALL_100K_V1 [--output-dir target/risk-graph-export/IBM_AML] "
                    + "[--feedback-model-version riskbrain-graphsage-ibm-aml-v1]");
        }
    }

    private static final class ExportStats {
        private int nodes;
        private int edges;
        private int labels;
        private int edgeFeaturesV2;
        private int feedbackLabels;
    }
}
