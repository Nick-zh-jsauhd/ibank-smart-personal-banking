package com.bank.tool;

import com.bank.util.DBUtil;
import com.bank.util.PasswordUtil;

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
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaySimImporter {
    private static final String DATASET = "PAYSIM";
    private static final String DEFAULT_PASSWORD = "RiskBrain123";
    private static final String DEFAULT_PASSWORD_HASH = PasswordUtil.hash(DEFAULT_PASSWORD);
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final int DEFAULT_LIMIT = 50000;
    private static final int DEFAULT_COMMIT_SIZE = 500;

    private final Map<String, EntityRef> entityCache = new HashMap<String, EntityRef>();

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

        PaySimImporter importer = new PaySimImporter();
        try {
            ImportStats stats = importer.importFile(options);
            System.out.println("PaySim import completed.");
            System.out.println("Batch code: " + options.batchCode);
            System.out.println("Read rows: " + stats.readRows);
            System.out.println("Imported rows: " + stats.importedRows);
            System.out.println("Skipped rows: " + stats.skippedRows);
            System.out.println("Fraud rows: " + stats.fraudRows);
            System.out.println("Flagged rows: " + stats.flaggedRows);
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
            throw new IllegalArgumentException("PaySim CSV not found: " + csvPath);
        }

        ImportStats stats = new ImportStats();
        if (options.dryRun) {
            dryRun(csvPath, options, stats);
            return stats;
        }

        Connection connection = null;
        boolean oldAutoCommit = true;
        long batchId = 0L;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            ensureRiskBrainTables(connection);
            batchId = createBatch(connection, options, csvPath);
            connection.commit();

            connection.setAutoCommit(false);
            readAndImport(connection, batchId, csvPath, options, stats);
            updateBatchSuccess(connection, batchId, stats);
            connection.commit();
            return stats;
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    if (batchId > 0) {
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

    private void dryRun(Path csvPath, Options options, ImportStats stats) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }
            Header header = Header.from(headerLine);
            String line;
            int rowNo = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (options.limit > 0 && stats.readRows >= options.limit) {
                    break;
                }
                try {
                    PaySimRow row = PaySimRow.parse(rowNo, line, header);
                    stats.readRows++;
                    stats.importedRows++;
                    if (row.fraud) {
                        stats.fraudRows++;
                    }
                    if (row.flaggedFraud) {
                        stats.flaggedRows++;
                    }
                } catch (RuntimeException e) {
                    stats.skippedRows++;
                }
            }
        }
    }

    private void readAndImport(Connection connection, long batchId, Path csvPath, Options options, ImportStats stats)
            throws IOException, SQLException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }
            Header header = Header.from(headerLine);
            String line;
            int rowNo = 0;
            int pending = 0;
            while ((line = reader.readLine()) != null) {
                rowNo++;
                if (options.limit > 0 && stats.readRows >= options.limit) {
                    break;
                }
                try {
                    PaySimRow row = PaySimRow.parse(rowNo, line, header);
                    stats.readRows++;
                    boolean imported = importRow(connection, batchId, row, options);
                    if (imported) {
                        stats.importedRows++;
                        if (row.fraud) {
                            stats.fraudRows++;
                        }
                        if (row.flaggedFraud) {
                            stats.flaggedRows++;
                        }
                    } else {
                        stats.skippedRows++;
                    }
                } catch (RuntimeException e) {
                    stats.skippedRows++;
                    System.err.println("Skip PaySim row " + rowNo + ": " + e.getMessage());
                }
                pending++;
                if (pending >= options.commitSize) {
                    connection.commit();
                    pending = 0;
                }
            }
        }
    }

    private boolean importRow(Connection connection, long batchId, PaySimRow row, Options options)
            throws SQLException {
        if (trainingSampleExists(connection, batchId, row.rowNo)) {
            return false;
        }

        String txnType = mapTxnType(row.type);
        Timestamp eventTime = Timestamp.valueOf(options.startDate.atStartOfDay()
                .plusHours(Math.max(0, row.step - 1))
                .plusSeconds(row.rowNo % 3600));

        BigDecimal originSeed = originSeedBalance(row);
        BigDecimal destSeed = row.oldBalanceDest == null ? ZERO : row.oldBalanceDest;
        EntityRef origin = findOrCreateEntity(connection, row.nameOrig, "CUSTOMER", originSeed);
        EntityRef dest = findOrCreateEntity(connection, row.nameDest, entityType(row.nameDest), destSeed);

        Long fromAccountId = fromAccountId(txnType, origin);
        Long toAccountId = toAccountId(txnType, origin, dest);
        String transactionNo = transactionNo(options.batchCode, row.rowNo);

        TransactionRef transaction = insertBusinessTransaction(connection, transactionNo, origin.customerId,
                fromAccountId, toAccountId, txnType, row.amount, row, eventTime);
        if (transaction.created) {
            insertBusinessLedger(connection, transaction.transactionId, txnType, origin, dest, row.amount, eventTime);
            if ("PAYMENT".equals(txnType)) {
                insertBillPayment(connection, transaction.transactionId, origin, row, eventTime);
            }
            if (row.fraud || row.flaggedFraud) {
                insertRiskEvent(connection, transaction.transactionId, transactionNo, origin, txnType, row, eventTime);
            }
        }

        insertTrainingSample(connection, batchId, row, transaction.transactionId, transactionNo, origin, dest, txnType,
                eventTime);
        return true;
    }

    private BigDecimal originSeedBalance(PaySimRow row) {
        BigDecimal oldBalance = row.oldBalanceOrigin == null ? ZERO : row.oldBalanceOrigin;
        if (isOutgoingPaySimType(row.type) && oldBalance.compareTo(row.amount) < 0) {
            return row.amount.multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP);
        }
        return oldBalance.max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isOutgoingPaySimType(String type) {
        return "TRANSFER".equals(type) || "CASH_OUT".equals(type)
                || "PAYMENT".equals(type) || "DEBIT".equals(type);
    }

    private Long fromAccountId(String txnType, EntityRef origin) {
        if ("DEPOSIT".equals(txnType)) {
            return null;
        }
        return origin.accountId;
    }

    private Long toAccountId(String txnType, EntityRef origin, EntityRef dest) {
        if ("DEPOSIT".equals(txnType)) {
            return origin.accountId;
        }
        if ("WITHDRAW".equals(txnType) || "TRANSFER_INNER".equals(txnType) || "PAYMENT".equals(txnType)) {
            return dest.accountId;
        }
        return null;
    }

    private String mapTxnType(String paysimType) {
        if ("CASH_IN".equals(paysimType)) {
            return "DEPOSIT";
        }
        if ("CASH_OUT".equals(paysimType)) {
            return "WITHDRAW";
        }
        if ("TRANSFER".equals(paysimType)) {
            return "TRANSFER_INNER";
        }
        if ("PAYMENT".equals(paysimType) || "DEBIT".equals(paysimType)) {
            return "PAYMENT";
        }
        throw new IllegalArgumentException("Unsupported PaySim type: " + paysimType);
    }

    private String entityType(String externalId) {
        if (externalId != null && externalId.startsWith("M")) {
            return "MERCHANT";
        }
        return "CUSTOMER";
    }

    private EntityRef findOrCreateEntity(Connection connection, String externalId, String entityType,
                                         BigDecimal initialBalance) throws SQLException {
        String key = DATASET + ":" + externalId;
        EntityRef cached = entityCache.get(key);
        if (cached != null) {
            return cached;
        }
        EntityRef existing = findMappedEntity(connection, externalId);
        if (existing != null) {
            entityCache.put(key, existing);
            return existing;
        }

        String username = usernameFor(externalId);
        String phone = phoneFor(externalId);
        long userId = upsertUser(connection, username, phone);
        long customerId = upsertCustomer(connection, userId, externalId, entityType, phone);
        updateUserCustomerId(connection, userId, customerId);
        String accountNo = accountNoFor(externalId);
        long accountId = upsertAccount(connection, customerId, accountNo, entityType, initialBalance);
        EntityRef ref = new EntityRef(userId, customerId, accountId, accountNo);
        upsertEntityMap(connection, externalId, entityType, ref);
        entityCache.put(key, ref);
        return ref;
    }

    private EntityRef findMappedEntity(Connection connection, String externalId) throws SQLException {
        String sql = "SELECT user_id, customer_id, account_id, account_no FROM t_risk_external_entity_map "
                + "WHERE dataset_name = ? AND external_entity_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DATASET);
            statement.setString(2, externalId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new EntityRef(rs.getLong("user_id"), rs.getLong("customer_id"),
                            rs.getLong("account_id"), rs.getString("account_no"));
                }
            }
        }
        return null;
    }

    private long upsertUser(Connection connection, String username, String phone) throws SQLException {
        String sql = "INSERT INTO t_user (username, phone, password_hash, pay_password_hash, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'CUSTOMER', 'NORMAL', NOW()) "
                + "ON DUPLICATE KEY UPDATE user_id = LAST_INSERT_ID(user_id)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, phone);
            statement.setString(3, DEFAULT_PASSWORD_HASH);
            statement.setString(4, DEFAULT_PASSWORD_HASH);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private long upsertCustomer(Connection connection, long userId, String externalId, String entityType, String phone)
            throws SQLException {
        String fullName = "PaySim " + entityType + " " + externalId;
        String sql = "INSERT INTO t_customer (user_id, full_name, phone, email, address, risk_level, "
                + "risk_level_source, risk_level_updated_at, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PAYSIM_IMPORT', NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE customer_id = LAST_INSERT_ID(customer_id)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, fullName);
            statement.setString(3, phone);
            statement.setString(4, usernameFor(externalId) + "@paysim.ibank.local");
            statement.setString(5, "PaySim synthetic entity " + externalId);
            statement.setString(6, "MERCHANT".equals(entityType) ? "C3" : "C2");
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void updateUserCustomerId(Connection connection, long userId, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE t_user SET customer_id = ? WHERE user_id = ?")) {
            statement.setLong(1, customerId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private long upsertAccount(Connection connection, long customerId, String accountNo, String entityType,
                               BigDecimal initialBalance) throws SQLException {
        String sql = "INSERT INTO t_account (customer_id, account_no, account_type, branch_name, "
                + "available_balance, frozen_balance, status, default_flag, opened_at) "
                + "VALUES (?, ?, ?, ?, ?, 0.00, 'NORMAL', 1, NOW()) "
                + "ON DUPLICATE KEY UPDATE account_id = LAST_INSERT_ID(account_id)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setString(2, accountNo);
            statement.setString(3, "MERCHANT".equals(entityType) ? "MERCHANT" : "SAVING");
            statement.setString(4, "iBank PaySim Import");
            statement.setBigDecimal(5, safeMoney(initialBalance));
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void upsertEntityMap(Connection connection, String externalId, String entityType, EntityRef ref)
            throws SQLException {
        String sql = "INSERT INTO t_risk_external_entity_map "
                + "(dataset_name, external_entity_id, entity_type, user_id, customer_id, account_id, account_no) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE map_id = LAST_INSERT_ID(map_id), user_id = VALUES(user_id), "
                + "customer_id = VALUES(customer_id), account_id = VALUES(account_id), account_no = VALUES(account_no)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DATASET);
            statement.setString(2, externalId);
            statement.setString(3, entityType);
            statement.setLong(4, ref.userId);
            statement.setLong(5, ref.customerId);
            statement.setLong(6, ref.accountId);
            statement.setString(7, ref.accountNo);
            statement.executeUpdate();
        }
    }

    private TransactionRef insertBusinessTransaction(Connection connection, String transactionNo, long customerId,
                                                     Long fromAccountId, Long toAccountId, String txnType,
                                                     BigDecimal amount, PaySimRow row, Timestamp eventTime)
            throws SQLException {
        Long existing = findTransactionId(connection, transactionNo);
        if (existing != null) {
            return new TransactionRef(existing, false);
        }
        int riskScore = row.fraud ? 95 : (row.flaggedFraud ? 80 : 0);
        String sql = "INSERT INTO t_transaction (transaction_no, customer_id, from_account_id, to_account_id, "
                + "txn_type, amount, status, risk_score, remark, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'SUCCESS', ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transactionNo);
            statement.setLong(2, customerId);
            setNullableLong(statement, 3, fromAccountId);
            setNullableLong(statement, 4, toAccountId);
            statement.setString(5, txnType);
            statement.setBigDecimal(6, safeMoney(amount));
            statement.setInt(7, riskScore);
            statement.setString(8, "PaySim " + row.type + " import row " + row.rowNo);
            statement.setTimestamp(9, eventTime);
            statement.setTimestamp(10, eventTime);
            statement.executeUpdate();
            return new TransactionRef(generatedId(statement), true);
        }
    }

    private Long findTransactionId(Connection connection, String transactionNo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT transaction_id FROM t_transaction WHERE transaction_no = ?")) {
            statement.setString(1, transactionNo);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void insertBusinessLedger(Connection connection, long transactionId, String txnType, EntityRef origin,
                                      EntityRef dest, BigDecimal amount, Timestamp eventTime) throws SQLException {
        if ("DEPOSIT".equals(txnType)) {
            applyLedger(connection, transactionId, origin.accountId, "IN", amount,
                    "PaySim 入账模拟", eventTime);
        } else if ("WITHDRAW".equals(txnType)) {
            applyLedger(connection, transactionId, origin.accountId, "OUT", amount,
                    "PaySim 取款模拟至 " + dest.accountNo, eventTime);
        } else if ("PAYMENT".equals(txnType)) {
            applyLedger(connection, transactionId, origin.accountId, "OUT", amount,
                    "PaySim 生活缴费/扣款模拟", eventTime);
        } else if ("TRANSFER_INNER".equals(txnType)) {
            applyLedger(connection, transactionId, origin.accountId, "OUT", amount,
                    "PaySim 转出至 " + dest.accountNo, eventTime);
            applyLedger(connection, transactionId, dest.accountId, "IN", amount,
                    "PaySim 入账自 " + origin.accountNo, eventTime);
        }
    }

    private void applyLedger(Connection connection, long transactionId, long accountId, String direction,
                             BigDecimal amount, String summary, Timestamp eventTime) throws SQLException {
        BigDecimal current = currentBalanceForUpdate(connection, accountId);
        BigDecimal next = "IN".equals(direction) ? current.add(amount) : current.subtract(amount);
        next = safeMoney(next);
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE t_account SET available_balance = ? WHERE account_id = ?")) {
            update.setBigDecimal(1, next);
            update.setLong(2, accountId);
            update.executeUpdate();
        }
        String sql = "INSERT INTO t_ledger_entry (transaction_id, account_id, direction, amount, balance_after, "
                + "summary, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, accountId);
            statement.setString(3, direction);
            statement.setBigDecimal(4, safeMoney(amount));
            statement.setBigDecimal(5, next);
            statement.setString(6, summary);
            statement.setTimestamp(7, eventTime);
            statement.executeUpdate();
        }
    }

    private BigDecimal currentBalanceForUpdate(Connection connection, long accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT available_balance FROM t_account WHERE account_id = ? FOR UPDATE")) {
            statement.setLong(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return safeMoney(rs.getBigDecimal(1));
                }
            }
        }
        throw new SQLException("Account not found: " + accountId);
    }

    private void insertBillPayment(Connection connection, long transactionId, EntityRef origin, PaySimRow row,
                                   Timestamp eventTime) throws SQLException {
        String sql = "INSERT INTO t_bill_payment (transaction_id, customer_id, account_id, payment_type, "
                + "institution_name, payer_no, billing_month, amount, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, origin.customerId);
            statement.setLong(3, origin.accountId);
            statement.setString(4, paymentType(row));
            statement.setString(5, "PaySim Synthetic Billing");
            statement.setString(6, row.nameDest);
            statement.setString(7, eventTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            statement.setBigDecimal(8, safeMoney(row.amount));
            statement.setTimestamp(9, eventTime);
            statement.executeUpdate();
        }
    }

    private String paymentType(PaySimRow row) {
        if ("DEBIT".equals(row.type)) {
            return "DEBIT";
        }
        return "PAYSIM_PAYMENT";
    }

    private void insertRiskEvent(Connection connection, long transactionId, String transactionNo, EntityRef origin,
                                 String txnType, PaySimRow row, Timestamp eventTime) throws SQLException {
        if (riskEventExists(connection, transactionNo)) {
            return;
        }
        int score = row.fraud ? 95 : 80;
        String decision = row.fraud ? "BLOCK" : "WARN";
        String hitRules = row.fraud ? "PAYSIM_LABEL_FRAUD" : "PAYSIM_FLAGGED_FRAUD";
        String reason = row.fraud
                ? "PaySim 标签标记为欺诈交易，用于 RiskBrain 训练样本。"
                : "PaySim 规则标记为高风险交易，用于 RiskBrain 训练样本。";
        String sql = "INSERT INTO t_risk_event (customer_id, account_id, transaction_no, txn_type, amount, "
                + "risk_score, risk_level, decision, hit_rules, reason, ip_address, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PAYSIM', ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, origin.customerId);
            statement.setLong(2, origin.accountId);
            statement.setString(3, transactionNo);
            statement.setString(4, txnType);
            statement.setBigDecimal(5, safeMoney(row.amount));
            statement.setInt(6, score);
            statement.setString(7, row.fraud ? "HIGH" : "MEDIUM");
            statement.setString(8, decision);
            statement.setString(9, hitRules);
            statement.setString(10, reason);
            statement.setTimestamp(11, eventTime);
            statement.executeUpdate();
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE t_transaction SET risk_score = ? WHERE transaction_id = ?")) {
            update.setInt(1, score);
            update.setLong(2, transactionId);
            update.executeUpdate();
        }
    }

    private boolean riskEventExists(Connection connection, String transactionNo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM t_risk_event WHERE transaction_no = ? LIMIT 1")) {
            statement.setString(1, transactionNo);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertTrainingSample(Connection connection, long batchId, PaySimRow row, long transactionId,
                                      String transactionNo, EntityRef origin, EntityRef dest, String txnType,
                                      Timestamp eventTime) throws SQLException {
        String sql = "INSERT INTO t_risk_training_sample (batch_id, source_dataset, source_row_no, source_step, "
                + "source_type, external_origin_id, external_dest_id, transaction_id, transaction_no, customer_id, "
                + "from_account_id, to_account_id, txn_type, amount, event_time, old_balance_origin, "
                + "new_balance_origin, old_balance_dest, new_balance_dest, label_fraud, label_flagged_rule, "
                + "label_source, feature_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            statement.setString(2, DATASET);
            statement.setInt(3, row.rowNo);
            statement.setInt(4, row.step);
            statement.setString(5, row.type);
            statement.setString(6, row.nameOrig);
            statement.setString(7, row.nameDest);
            statement.setLong(8, transactionId);
            statement.setString(9, transactionNo);
            statement.setLong(10, origin.customerId);
            setNullableLong(statement, 11, fromAccountId(txnType, origin));
            setNullableLong(statement, 12, toAccountId(txnType, origin, dest));
            statement.setString(13, txnType);
            statement.setBigDecimal(14, safeMoney(row.amount));
            statement.setTimestamp(15, eventTime);
            setNullableMoney(statement, 16, row.oldBalanceOrigin);
            setNullableMoney(statement, 17, row.newBalanceOrigin);
            setNullableMoney(statement, 18, row.oldBalanceDest);
            setNullableMoney(statement, 19, row.newBalanceDest);
            statement.setBoolean(20, row.fraud);
            statement.setBoolean(21, row.flaggedFraud);
            statement.setString(22, DATASET);
            statement.setString(23, featureJson(row, txnType, origin, dest));
            statement.executeUpdate();
        }
    }

    private boolean trainingSampleExists(Connection connection, long batchId, int rowNo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM t_risk_training_sample WHERE batch_id = ? AND source_row_no = ?")) {
            statement.setLong(1, batchId);
            statement.setInt(2, rowNo);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String featureJson(PaySimRow row, String txnType, EntityRef origin, EntityRef dest) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendJson(json, "source_type", row.type).append(",");
        appendJson(json, "txn_type", txnType).append(",");
        appendJson(json, "amount", safeMoney(row.amount).toPlainString()).append(",");
        appendJson(json, "origin_old_balance", moneyText(row.oldBalanceOrigin)).append(",");
        appendJson(json, "origin_new_balance", moneyText(row.newBalanceOrigin)).append(",");
        appendJson(json, "dest_old_balance", moneyText(row.oldBalanceDest)).append(",");
        appendJson(json, "dest_new_balance", moneyText(row.newBalanceDest)).append(",");
        appendJson(json, "origin_prefix", prefix(row.nameOrig)).append(",");
        appendJson(json, "dest_prefix", prefix(row.nameDest)).append(",");
        appendJson(json, "dest_is_merchant", Boolean.toString(row.nameDest.startsWith("M"))).append(",");
        appendJson(json, "origin_account_no", origin.accountNo).append(",");
        appendJson(json, "dest_account_no", dest.accountNo).append(",");
        appendJson(json, "label_fraud", Boolean.toString(row.fraud)).append(",");
        appendJson(json, "label_flagged_rule", Boolean.toString(row.flaggedFraud));
        json.append("}");
        return json.toString();
    }

    private StringBuilder appendJson(StringBuilder builder, String key, String value) {
        builder.append("\"").append(jsonEscape(key)).append("\":\"").append(jsonEscape(value)).append("\"");
        return builder;
    }

    private String prefix(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        return value.substring(0, 1);
    }

    private String moneyText(BigDecimal value) {
        return value == null ? "" : safeMoney(value).toPlainString();
    }

    private long createBatch(Connection connection, Options options, Path csvPath) throws SQLException {
        String sql = "INSERT INTO t_risk_dataset_batch (batch_code, dataset_name, source_file, import_mode, "
                + "row_limit, status, metadata_json, started_at) VALUES (?, ?, ?, ?, ?, 'RUNNING', ?, NOW()) "
                + "ON DUPLICATE KEY UPDATE batch_id = LAST_INSERT_ID(batch_id), status = 'RUNNING', "
                + "error_message = NULL, completed_at = NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, options.batchCode);
            statement.setString(2, DATASET);
            statement.setString(3, csvPath.toString());
            statement.setString(4, "BUSINESS_AND_TRAINING");
            if (options.limit > 0) {
                statement.setInt(5, options.limit);
            } else {
                statement.setNull(5, Types.INTEGER);
            }
            statement.setString(6, "{\"start_date\":\"" + options.startDate + "\",\"commit_size\":"
                    + options.commitSize + "}");
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void updateBatchSuccess(Connection connection, long batchId, ImportStats stats) throws SQLException {
        BatchTotals totals = batchTotals(connection, batchId);
        String sql = "UPDATE t_risk_dataset_batch SET imported_rows = ?, skipped_rows = ?, fraud_rows = ?, "
                + "flagged_rows = ?, status = 'SUCCESS', completed_at = NOW() WHERE batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, totals.importedRows);
            statement.setInt(2, stats.skippedRows);
            statement.setInt(3, totals.fraudRows);
            statement.setInt(4, totals.flaggedRows);
            statement.setLong(5, batchId);
            statement.executeUpdate();
        }
    }

    private BatchTotals batchTotals(Connection connection, long batchId) throws SQLException {
        String sql = "SELECT COUNT(*) AS imported_rows, "
                + "SUM(CASE WHEN label_fraud = 1 THEN 1 ELSE 0 END) AS fraud_rows, "
                + "SUM(CASE WHEN label_flagged_rule = 1 THEN 1 ELSE 0 END) AS flagged_rows "
                + "FROM t_risk_training_sample WHERE batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, batchId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BatchTotals totals = new BatchTotals();
                    totals.importedRows = rs.getInt("imported_rows");
                    totals.fraudRows = rs.getInt("fraud_rows");
                    totals.flaggedRows = rs.getInt("flagged_rows");
                    return totals;
                }
            }
        }
        return new BatchTotals();
    }

    private void updateBatchFailure(Connection connection, long batchId, Exception e) throws SQLException {
        String sql = "UPDATE t_risk_dataset_batch SET status = 'FAILED', error_message = ?, completed_at = NOW() "
                + "WHERE batch_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, abbreviate(e.getMessage(), 1000));
            statement.setLong(2, batchId);
            statement.executeUpdate();
        }
    }

    private void ensureRiskBrainTables(Connection connection) throws SQLException {
        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_dataset_batch ("
                + "batch_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "batch_code VARCHAR(80) NOT NULL,"
                + "dataset_name VARCHAR(60) NOT NULL,"
                + "source_file VARCHAR(500) NOT NULL,"
                + "import_mode VARCHAR(40) NOT NULL DEFAULT 'BUSINESS_AND_TRAINING',"
                + "row_limit INT NULL,"
                + "imported_rows INT NOT NULL DEFAULT 0,"
                + "skipped_rows INT NOT NULL DEFAULT 0,"
                + "fraud_rows INT NOT NULL DEFAULT 0,"
                + "flagged_rows INT NOT NULL DEFAULT 0,"
                + "status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',"
                + "error_message VARCHAR(1000) NULL,"
                + "metadata_json TEXT NULL,"
                + "started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "completed_at DATETIME NULL,"
                + "UNIQUE KEY uk_risk_dataset_batch_code (batch_code),"
                + "KEY idx_risk_dataset_batch_name (dataset_name),"
                + "KEY idx_risk_dataset_batch_status (status)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_external_entity_map ("
                + "map_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "dataset_name VARCHAR(60) NOT NULL,"
                + "external_entity_id VARCHAR(80) NOT NULL,"
                + "entity_type VARCHAR(30) NOT NULL,"
                + "user_id BIGINT NOT NULL,"
                + "customer_id BIGINT NOT NULL,"
                + "account_id BIGINT NOT NULL,"
                + "account_no VARCHAR(32) NOT NULL,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uk_risk_external_entity (dataset_name, external_entity_id),"
                + "KEY idx_risk_external_customer (customer_id),"
                + "KEY idx_risk_external_account (account_id),"
                + "CONSTRAINT fk_risk_external_user FOREIGN KEY (user_id) REFERENCES t_user (user_id),"
                + "CONSTRAINT fk_risk_external_customer FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),"
                + "CONSTRAINT fk_risk_external_account FOREIGN KEY (account_id) REFERENCES t_account (account_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_training_sample ("
                + "sample_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "batch_id BIGINT NOT NULL,"
                + "source_dataset VARCHAR(60) NOT NULL,"
                + "source_row_no INT NOT NULL,"
                + "source_step INT NOT NULL,"
                + "source_type VARCHAR(30) NOT NULL,"
                + "external_origin_id VARCHAR(80) NOT NULL,"
                + "external_dest_id VARCHAR(80) NOT NULL,"
                + "transaction_id BIGINT NULL,"
                + "transaction_no VARCHAR(40) NULL,"
                + "customer_id BIGINT NULL,"
                + "from_account_id BIGINT NULL,"
                + "to_account_id BIGINT NULL,"
                + "txn_type VARCHAR(30) NOT NULL,"
                + "amount DECIMAL(18,2) NOT NULL,"
                + "event_time DATETIME NOT NULL,"
                + "old_balance_origin DECIMAL(18,2) NULL,"
                + "new_balance_origin DECIMAL(18,2) NULL,"
                + "old_balance_dest DECIMAL(18,2) NULL,"
                + "new_balance_dest DECIMAL(18,2) NULL,"
                + "label_fraud TINYINT(1) NOT NULL DEFAULT 0,"
                + "label_flagged_rule TINYINT(1) NOT NULL DEFAULT 0,"
                + "label_source VARCHAR(30) NOT NULL DEFAULT 'PAYSIM',"
                + "feature_json TEXT NULL,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uk_risk_training_batch_row (batch_id, source_row_no),"
                + "KEY idx_risk_training_transaction (transaction_id),"
                + "KEY idx_risk_training_customer (customer_id),"
                + "KEY idx_risk_training_label (label_fraud, label_flagged_rule),"
                + "KEY idx_risk_training_time (event_time),"
                + "CONSTRAINT fk_risk_training_batch FOREIGN KEY (batch_id) REFERENCES t_risk_dataset_batch (batch_id),"
                + "CONSTRAINT fk_risk_training_transaction FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id),"
                + "CONSTRAINT fk_risk_training_customer FOREIGN KEY (customer_id) REFERENCES t_customer (customer_id),"
                + "CONSTRAINT fk_risk_training_from_account FOREIGN KEY (from_account_id) REFERENCES t_account (account_id),"
                + "CONSTRAINT fk_risk_training_to_account FOREIGN KEY (to_account_id) REFERENCES t_account (account_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeDdl(connection, "CREATE TABLE IF NOT EXISTS t_risk_model_score ("
                + "score_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "sample_id BIGINT NULL,"
                + "transaction_id BIGINT NULL,"
                + "transaction_no VARCHAR(40) NULL,"
                + "model_version VARCHAR(60) NOT NULL,"
                + "feature_version VARCHAR(60) NOT NULL,"
                + "risk_score INT NOT NULL,"
                + "risk_probability DECIMAL(10,8) NULL,"
                + "decision VARCHAR(30) NOT NULL,"
                + "reason_json TEXT NULL,"
                + "scored_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "KEY idx_risk_model_sample (sample_id),"
                + "KEY idx_risk_model_transaction (transaction_id),"
                + "KEY idx_risk_model_version (model_version),"
                + "CONSTRAINT fk_risk_model_sample FOREIGN KEY (sample_id) REFERENCES t_risk_training_sample (sample_id),"
                + "CONSTRAINT fk_risk_model_transaction FOREIGN KEY (transaction_id) REFERENCES t_transaction (transaction_id)"
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

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private void setNullableMoney(PreparedStatement statement, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, safeMoney(value));
        }
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String transactionNo(String batchCode, int rowNo) {
        return "PS" + sha256(batchCode).substring(0, 8).toUpperCase(Locale.ROOT)
                + String.format(Locale.ROOT, "%010d", rowNo);
    }

    private String usernameFor(String externalId) {
        String cleaned = cleaned(externalId).toLowerCase(Locale.ROOT);
        String username = "paysim_" + cleaned;
        if (username.length() <= 50) {
            return username;
        }
        return "paysim_" + sha256(externalId).substring(0, 43);
    }

    private String phoneFor(String externalId) {
        return "PS" + sha256(externalId).substring(0, 18);
    }

    private String accountNoFor(String externalId) {
        String accountNo = "PS" + cleaned(externalId).toUpperCase(Locale.ROOT);
        if (accountNo.length() <= 32) {
            return accountNo;
        }
        return "PS" + sha256(externalId).substring(0, 30).toUpperCase(Locale.ROOT);
    }

    private String cleaned(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                builder.append(ch);
            }
        }
        return builder.length() == 0 ? sha256(value).substring(0, 12) : builder.toString();
    }

    private String sha256(String value) {
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

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static final class Options {
        private String file;
        private int limit = DEFAULT_LIMIT;
        private int commitSize = DEFAULT_COMMIT_SIZE;
        private String batchCode = "PAYSIM_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        private LocalDate startDate = LocalDate.of(2026, 1, 1);
        private boolean dryRun;

        private static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--file".equals(arg)) {
                    options.file = requireValue(args, ++i, arg);
                } else if ("--limit".equals(arg)) {
                    options.limit = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--commit-size".equals(arg)) {
                    options.commitSize = Integer.parseInt(requireValue(args, ++i, arg));
                } else if ("--batch-code".equals(arg)) {
                    options.batchCode = requireValue(args, ++i, arg);
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

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value.");
            }
            return args[index];
        }

        private static void printUsage() {
            System.out.println("Usage:");
            System.out.println("  java -cp \"target/classes;target/dependency/*\" com.bank.tool.PaySimImporter "
                    + "--file <PaySim.csv> [--limit 50000] [--batch-code PAYSIM_YYYYMMDDHHMMSS] "
                    + "[--start-date 2026-01-01] [--commit-size 500] [--dry-run]");
            System.out.println();
            System.out.println("Notes:");
            System.out.println("  --limit 0 imports all rows. Keep a smaller limit for the first local run.");
            System.out.println("  The importer writes iBank business tables and RiskBrain training tables.");
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
                indexes.put(values.get(i).trim(), i);
            }
            require(indexes, "step");
            require(indexes, "type");
            require(indexes, "amount");
            require(indexes, "nameOrig");
            require(indexes, "oldbalanceOrg");
            require(indexes, "newbalanceOrig");
            require(indexes, "nameDest");
            require(indexes, "oldbalanceDest");
            require(indexes, "newbalanceDest");
            require(indexes, "isFraud");
            require(indexes, "isFlaggedFraud");
            return new Header(indexes);
        }

        private static void require(Map<String, Integer> indexes, String name) {
            if (!indexes.containsKey(name)) {
                throw new IllegalArgumentException("PaySim CSV missing column: " + name);
            }
        }

        private String value(List<String> row, String column) {
            Integer index = indexes.get(column);
            if (index == null || index >= row.size()) {
                return "";
            }
            return row.get(index);
        }
    }

    private static final class PaySimRow {
        private int rowNo;
        private int step;
        private String type;
        private BigDecimal amount;
        private String nameOrig;
        private BigDecimal oldBalanceOrigin;
        private BigDecimal newBalanceOrigin;
        private String nameDest;
        private BigDecimal oldBalanceDest;
        private BigDecimal newBalanceDest;
        private boolean fraud;
        private boolean flaggedFraud;

        private static PaySimRow parse(int rowNo, String line, Header header) {
            List<String> values = parseCsvLine(line);
            PaySimRow row = new PaySimRow();
            row.rowNo = rowNo;
            row.step = Integer.parseInt(header.value(values, "step").trim());
            row.type = header.value(values, "type").trim().toUpperCase(Locale.ROOT);
            row.amount = money(header.value(values, "amount"));
            row.nameOrig = header.value(values, "nameOrig").trim();
            row.oldBalanceOrigin = money(header.value(values, "oldbalanceOrg"));
            row.newBalanceOrigin = money(header.value(values, "newbalanceOrig"));
            row.nameDest = header.value(values, "nameDest").trim();
            row.oldBalanceDest = money(header.value(values, "oldbalanceDest"));
            row.newBalanceDest = money(header.value(values, "newbalanceDest"));
            row.fraud = "1".equals(header.value(values, "isFraud").trim());
            row.flaggedFraud = "1".equals(header.value(values, "isFlaggedFraud").trim());
            if (row.nameOrig.length() == 0 || row.nameDest.length() == 0) {
                throw new IllegalArgumentException("missing origin or destination account");
            }
            return row;
        }

        private static BigDecimal money(String value) {
            if (value == null || value.trim().length() == 0) {
                return ZERO;
            }
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
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

    private static final class EntityRef {
        private final long userId;
        private final long customerId;
        private final long accountId;
        private final String accountNo;

        private EntityRef(long userId, long customerId, long accountId, String accountNo) {
            this.userId = userId;
            this.customerId = customerId;
            this.accountId = accountId;
            this.accountNo = accountNo;
        }
    }

    private static final class TransactionRef {
        private final long transactionId;
        private final boolean created;

        private TransactionRef(long transactionId, boolean created) {
            this.transactionId = transactionId;
            this.created = created;
        }
    }

    private static final class ImportStats {
        private int readRows;
        private int importedRows;
        private int skippedRows;
        private int fraudRows;
        private int flaggedRows;
    }

    private static final class BatchTotals {
        private int importedRows;
        private int fraudRows;
        private int flaggedRows;
    }
}
