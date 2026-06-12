package com.bank.dao.impl;

import com.bank.bean.BillPayment;
import com.bank.dao.BillPaymentDao;
import com.bank.dto.BillPaymentView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BillPaymentDaoImpl implements BillPaymentDao {
    @Override
    public long insert(Connection connection, BillPayment billPayment) throws SQLException {
        String sql = "INSERT INTO t_bill_payment (transaction_id, customer_id, account_id, payment_type, "
                + "institution_name, payer_no, billing_month, amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, billPayment.getTransactionId());
            statement.setLong(2, billPayment.getCustomerId());
            statement.setLong(3, billPayment.getAccountId());
            statement.setString(4, billPayment.getPaymentType());
            statement.setString(5, billPayment.getInstitutionName());
            statement.setString(6, billPayment.getPayerNo());
            statement.setString(7, billPayment.getBillingMonth());
            statement.setBigDecimal(8, billPayment.getAmount());
            statement.setString(9, billPayment.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("No generated key returned");
    }

    @Override
    public List<BillPaymentView> findByCustomer(Connection connection, long customerId, String paymentType,
                                                int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.payment_id, t.transaction_no, a.account_no, p.payment_type, ");
        sql.append("p.institution_name, p.payer_no, p.billing_month, p.amount, p.status, p.created_at ");
        sql.append("FROM t_bill_payment p ");
        sql.append("JOIN t_transaction t ON p.transaction_id = t.transaction_id ");
        sql.append("JOIN t_account a ON p.account_id = a.account_id ");
        sql.append("WHERE p.customer_id = ? ");
        if (paymentType != null && paymentType.length() > 0) {
            sql.append("AND p.payment_type = ? ");
        }
        sql.append("ORDER BY p.created_at DESC, p.payment_id DESC LIMIT ?");

        List<BillPaymentView> records = new ArrayList<BillPaymentView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setLong(index++, customerId);
            if (paymentType != null && paymentType.length() > 0) {
                statement.setString(index++, paymentType);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapView(resultSet));
                }
            }
        }
        return records;
    }

    private BillPaymentView mapView(ResultSet resultSet) throws SQLException {
        BillPaymentView view = new BillPaymentView();
        view.setPaymentId(resultSet.getLong("payment_id"));
        view.setTransactionNo(resultSet.getString("transaction_no"));
        view.setAccountNo(resultSet.getString("account_no"));
        view.setPaymentType(resultSet.getString("payment_type"));
        view.setInstitutionName(resultSet.getString("institution_name"));
        view.setPayerNo(resultSet.getString("payer_no"));
        view.setBillingMonth(resultSet.getString("billing_month"));
        view.setAmount(resultSet.getBigDecimal("amount"));
        view.setStatus(resultSet.getString("status"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        return view;
    }
}
