package com.bank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DemoDataSeeder {
    private static final String LOGIN_PASSWORD = "123456";
    private static final String PAY_PASSWORD = "123456";
    private static final int CUSTOMER_COUNT = 10;
    private static final int MONTHS_BACK = 5;

    private final Random random = new Random(20260517L);
    private final List<DemoCustomer> customers = new ArrayList<DemoCustomer>();
    private final List<DemoAccount> accounts = new ArrayList<DemoAccount>();

    public static void main(String[] args) {
        DemoDataSeeder seeder = new DemoDataSeeder();
        try {
            seeder.seed();
            System.out.println("Demo data generated successfully.");
            System.out.println("Demo login password: " + LOGIN_PASSWORD);
            System.out.println("Demo pay password: " + PAY_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void seed() throws SQLException {
        Connection connection = null;
        boolean oldAutoCommit = true;
        try {
            connection = DBUtil.getConnection();
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            String batch = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            createCustomers(connection, batch);
            createAccounts(connection);
            createLedgerData(connection);

            connection.commit();
            printSummary(batch);
        } catch (SQLException e) {
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

    private void createCustomers(Connection connection, String batch) throws SQLException {
        String[] names = {
                "陈明", "李佳", "王磊", "赵悦", "刘洋",
                "孙晨", "周宁", "吴昊", "郑琳", "何雨"
        };
        String[] riskLevels = {"C1", "C2", "C2", "C3", "C3", "C4", "C2", "C3", "C4", "C5"};

        for (int i = 0; i < CUSTOMER_COUNT; i++) {
            String username = "demo_" + batch + "_" + String.format("%02d", i + 1);
            String phone = "139" + batch.substring(6, 12) + String.format("%02d", i + 1);
            long userId = insertUser(connection, username, phone);
            long customerId = insertCustomer(connection, userId, names[i], phone, riskLevels[i]);
            updateUserCustomerId(connection, userId, customerId);

            DemoCustomer customer = new DemoCustomer();
            customer.userId = userId;
            customer.customerId = customerId;
            customer.username = username;
            customer.fullName = names[i];
            customers.add(customer);
        }
    }

    private void createAccounts(Connection connection) throws SQLException {
        for (DemoCustomer customer : customers) {
            DemoAccount primary = insertAccount(connection, customer.customerId, true, randomMoney(3000, 18000));
            DemoAccount secondary = insertAccount(connection, customer.customerId, false, randomMoney(500, 8000));
            accounts.add(primary);
            accounts.add(secondary);
            customer.primaryAccount = primary;
            customer.secondaryAccount = secondary;
        }
    }

    private void createLedgerData(Connection connection) throws SQLException {
        LocalDate start = LocalDate.now().minusMonths(MONTHS_BACK).withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        LocalDate cursor = start;

        while (!cursor.isAfter(end)) {
            for (DemoCustomer customer : customers) {
                if (cursor.getDayOfMonth() == 5) {
                    deposit(connection, customer, customer.primaryAccount, randomMoney(6500, 18000),
                            "工资收入", at(cursor, 9, 30));
                }
                if (cursor.getDayOfMonth() == 12 && random.nextInt(100) < 65) {
                    withdraw(connection, customer, customer.primaryAccount, randomMoney(200, 1200),
                            "日常现金取款", at(cursor, 16, 15));
                }
                if (cursor.getDayOfMonth() == 18 && random.nextInt(100) < 70) {
                    payment(connection, customer, customer.primaryAccount, "WATER",
                            "户号" + customer.customerId + "01", cursor, randomMoney(40, 180),
                            at(cursor, 10, 20));
                }
                if (cursor.getDayOfMonth() == 20 && random.nextInt(100) < 75) {
                    payment(connection, customer, customer.primaryAccount, "ELECTRICITY",
                            "户号" + customer.customerId + "02", cursor, randomMoney(80, 420),
                            at(cursor, 11, 10));
                }
                if (cursor.getDayOfMonth() == 23 && random.nextInt(100) < 55) {
                    payment(connection, customer, customer.primaryAccount, "MOBILE",
                            "1390000" + String.format("%04d", (int) (customer.customerId % 10000)),
                            cursor, randomMoney(50, 300), at(cursor, 14, 5));
                }
                if (cursor.getDayOfMonth() == 26 && random.nextInt(100) < 60) {
                    DemoCustomer receiver = customers.get(random.nextInt(customers.size()));
                    if (receiver.customerId != customer.customerId) {
                        transfer(connection, customer, customer.primaryAccount, receiver.primaryAccount,
                                randomMoney(200, 2600), "本行转账", at(cursor, 19, 35));
                    }
                }
            }
            cursor = cursor.plusDays(1);
        }

        for (int i = 0; i < 60; i++) {
            DemoCustomer customer = customers.get(random.nextInt(customers.size()));
            LocalDate randomDate = start.plusDays(random.nextInt((int) (end.toEpochDay() - start.toEpochDay() + 1)));
            int kind = random.nextInt(4);
            if (kind == 0) {
                deposit(connection, customer, customer.secondaryAccount, randomMoney(200, 3000),
                        "临时入账", randomTime(randomDate));
            } else if (kind == 1) {
                withdraw(connection, customer, customer.primaryAccount, randomMoney(100, 900),
                        "零星取款", randomTime(randomDate));
            } else if (kind == 2) {
                payment(connection, customer, customer.primaryAccount, "GAS",
                        "户号" + customer.customerId + "03", randomDate, randomMoney(60, 260),
                        randomTime(randomDate));
            } else {
                DemoCustomer receiver = customers.get(random.nextInt(customers.size()));
                if (receiver.customerId != customer.customerId) {
                    transfer(connection, customer, customer.secondaryAccount, receiver.secondaryAccount,
                            randomMoney(100, 1800), "朋友转账", randomTime(randomDate));
                }
            }
        }

        for (DemoAccount account : accounts) {
            updateAccountBalance(connection, account);
        }
    }

    private long insertUser(Connection connection, String username, String phone) throws SQLException {
        String sql = "INSERT INTO t_user (username, phone, password_hash, pay_password_hash, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'CUSTOMER', 'NORMAL', ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, phone);
            statement.setString(3, PasswordUtil.hash(LOGIN_PASSWORD));
            statement.setString(4, PasswordUtil.hash(PAY_PASSWORD));
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().minusMonths(MONTHS_BACK)));
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private long insertCustomer(Connection connection, long userId, String fullName, String phone, String riskLevel)
            throws SQLException {
        String sql = "INSERT INTO t_customer (user_id, full_name, phone, email, address, risk_level, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, fullName);
            statement.setString(3, phone);
            statement.setString(4, "demo" + userId + "@ibank.local");
            statement.setString(5, "iBank 演示数据地址 " + userId + " 号");
            statement.setString(6, riskLevel);
            statement.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now().minusMonths(MONTHS_BACK)));
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

    private DemoAccount insertAccount(Connection connection, long customerId, boolean defaultFlag,
                                      BigDecimal initialBalance) throws SQLException {
        DemoAccount account = new DemoAccount();
        account.customerId = customerId;
        account.accountNo = uniqueAccountNo(connection);
        account.balance = initialBalance.setScale(2, RoundingMode.HALF_UP);

        String sql = "INSERT INTO t_account (customer_id, account_no, account_type, branch_name, "
                + "available_balance, frozen_balance, status, default_flag, opened_at) "
                + "VALUES (?, ?, 'SAVING', 'iBank Demo Branch', ?, 0.00, 'NORMAL', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setString(2, account.accountNo);
            statement.setBigDecimal(3, account.balance);
            statement.setBoolean(4, defaultFlag);
            statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().minusMonths(MONTHS_BACK)));
            statement.executeUpdate();
            account.accountId = generatedId(statement);
        }
        return account;
    }

    private void deposit(Connection connection, DemoCustomer customer, DemoAccount account, BigDecimal amount,
                         String remark, Timestamp createdAt) throws SQLException {
        account.balance = account.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        long transactionId = insertTransaction(connection, TransactionNoGenerator.generate(), customer.customerId,
                null, account.accountId, "DEPOSIT", amount, remark, createdAt);
        insertLedger(connection, transactionId, account.accountId, "IN", amount, account.balance,
                remark, createdAt);
    }

    private void withdraw(Connection connection, DemoCustomer customer, DemoAccount account, BigDecimal amount,
                          String remark, Timestamp createdAt) throws SQLException {
        if (account.balance.compareTo(amount) < 0) {
            return;
        }
        account.balance = account.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        long transactionId = insertTransaction(connection, TransactionNoGenerator.generate(), customer.customerId,
                account.accountId, null, "WITHDRAW", amount, remark, createdAt);
        insertLedger(connection, transactionId, account.accountId, "OUT", amount, account.balance,
                remark, createdAt);
    }

    private void transfer(Connection connection, DemoCustomer fromCustomer, DemoAccount fromAccount,
                          DemoAccount toAccount, BigDecimal amount, String remark, Timestamp createdAt)
            throws SQLException {
        if (fromAccount.balance.compareTo(amount) < 0) {
            return;
        }
        fromAccount.balance = fromAccount.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        toAccount.balance = toAccount.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        String transactionNo = TransactionNoGenerator.generate();
        long transactionId = insertTransaction(connection, transactionNo, fromCustomer.customerId,
                fromAccount.accountId, toAccount.accountId, "TRANSFER_INNER", amount, remark, createdAt);
        insertLedger(connection, transactionId, fromAccount.accountId, "OUT", amount, fromAccount.balance,
                "本行转账转出至 " + toAccount.accountNo, createdAt);
        insertLedger(connection, transactionId, toAccount.accountId, "IN", amount, toAccount.balance,
                "本行转账入账自 " + fromAccount.accountNo, createdAt);
    }

    private void payment(Connection connection, DemoCustomer customer, DemoAccount account, String paymentType,
                         String payerNo, LocalDate billingDate, BigDecimal amount, Timestamp createdAt)
            throws SQLException {
        if (account.balance.compareTo(amount) < 0) {
            return;
        }
        account.balance = account.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        String transactionNo = TransactionNoGenerator.generate();
        long transactionId = insertTransaction(connection, transactionNo, customer.customerId,
                account.accountId, null, "PAYMENT", amount, paymentTypeName(paymentType) + "缴费", createdAt);
        insertLedger(connection, transactionId, account.accountId, "OUT", amount, account.balance,
                paymentTypeName(paymentType) + "缴费", createdAt);
        insertBillPayment(connection, transactionId, customer.customerId, account.accountId, paymentType,
                payerNo, billingDate.format(DateTimeFormatter.ofPattern("yyyy-MM")), amount, createdAt);
    }

    private long insertTransaction(Connection connection, String transactionNo, long customerId,
                                   Long fromAccountId, Long toAccountId, String txnType, BigDecimal amount,
                                   String remark, Timestamp createdAt) throws SQLException {
        String sql = "INSERT INTO t_transaction (transaction_no, customer_id, from_account_id, to_account_id, "
                + "txn_type, amount, status, risk_score, remark, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'SUCCESS', 0, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transactionNo);
            statement.setLong(2, customerId);
            if (fromAccountId == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, fromAccountId);
            }
            if (toAccountId == null) {
                statement.setNull(4, java.sql.Types.BIGINT);
            } else {
                statement.setLong(4, toAccountId);
            }
            statement.setString(5, txnType);
            statement.setBigDecimal(6, amount);
            statement.setString(7, remark);
            statement.setTimestamp(8, createdAt);
            statement.setTimestamp(9, createdAt);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private void insertLedger(Connection connection, long transactionId, long accountId, String direction,
                              BigDecimal amount, BigDecimal balanceAfter, String summary, Timestamp createdAt)
            throws SQLException {
        String sql = "INSERT INTO t_ledger_entry (transaction_id, account_id, direction, amount, balance_after, "
                + "summary, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, accountId);
            statement.setString(3, direction);
            statement.setBigDecimal(4, amount);
            statement.setBigDecimal(5, balanceAfter);
            statement.setString(6, summary);
            statement.setTimestamp(7, createdAt);
            statement.executeUpdate();
        }
    }

    private void insertBillPayment(Connection connection, long transactionId, long customerId, long accountId,
                                   String paymentType, String payerNo, String billingMonth, BigDecimal amount,
                                   Timestamp createdAt) throws SQLException {
        String sql = "INSERT INTO t_bill_payment (transaction_id, customer_id, account_id, payment_type, "
                + "institution_name, payer_no, billing_month, amount, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS', ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setLong(2, customerId);
            statement.setLong(3, accountId);
            statement.setString(4, paymentType);
            statement.setString(5, institutionNameFor(paymentType));
            statement.setString(6, payerNo);
            statement.setString(7, billingMonth);
            statement.setBigDecimal(8, amount);
            statement.setTimestamp(9, createdAt);
            statement.executeUpdate();
        }
    }

    private void updateAccountBalance(Connection connection, DemoAccount account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE t_account SET available_balance = ? WHERE account_id = ?")) {
            statement.setBigDecimal(1, account.balance);
            statement.setLong(2, account.accountId);
            statement.executeUpdate();
        }
    }

    private String uniqueAccountNo(Connection connection) throws SQLException {
        for (int i = 0; i < 20; i++) {
            String accountNo = AccountNoGenerator.generate();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM t_account WHERE account_no = ?")) {
                statement.setString(1, accountNo);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return accountNo;
                    }
                }
            }
        }
        throw new SQLException("Unable to generate unique account number");
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("No generated key returned");
    }

    private BigDecimal randomMoney(int min, int max) {
        int cents = (min * 100) + random.nextInt((max - min + 1) * 100);
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private Timestamp at(LocalDate date, int hour, int minute) {
        return Timestamp.valueOf(LocalDateTime.of(date, LocalTime.of(hour, minute))
                .plusMinutes(random.nextInt(45)));
    }

    private Timestamp randomTime(LocalDate date) {
        return Timestamp.valueOf(LocalDateTime.of(date, LocalTime.of(8 + random.nextInt(14), random.nextInt(60))));
    }

    private String paymentTypeName(String paymentType) {
        if ("WATER".equals(paymentType)) {
            return "水费";
        }
        if ("ELECTRICITY".equals(paymentType)) {
            return "电费";
        }
        if ("GAS".equals(paymentType)) {
            return "燃气费";
        }
        if ("MOBILE".equals(paymentType)) {
            return "话费";
        }
        return "生活";
    }

    private String institutionNameFor(String paymentType) {
        if ("WATER".equals(paymentType)) {
            return "iBank 城市供水模拟机构";
        }
        if ("ELECTRICITY".equals(paymentType)) {
            return "iBank 电力缴费模拟机构";
        }
        if ("GAS".equals(paymentType)) {
            return "iBank 燃气服务模拟机构";
        }
        if ("MOBILE".equals(paymentType)) {
            return "iBank 通信充值模拟机构";
        }
        return "iBank 生活缴费模拟机构";
    }

    private void printSummary(String batch) {
        System.out.println("Batch: " + batch);
        System.out.println("Created customers: " + customers.size());
        System.out.println("Created accounts: " + accounts.size());
        if (!customers.isEmpty()) {
            DemoCustomer first = customers.get(0);
            System.out.println("First demo username: " + first.username);
            System.out.println("First demo customer: " + first.fullName);
            System.out.println("First demo account: " + first.primaryAccount.accountNo);
        }
    }

    private static class DemoCustomer {
        long userId;
        long customerId;
        String username;
        String fullName;
        DemoAccount primaryAccount;
        DemoAccount secondaryAccount;
    }

    private static class DemoAccount {
        long accountId;
        long customerId;
        String accountNo;
        BigDecimal balance;
    }
}
