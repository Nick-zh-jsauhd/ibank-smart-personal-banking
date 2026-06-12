package com.bank.dao;

import com.bank.bean.BillPayment;
import com.bank.dto.BillPaymentView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface BillPaymentDao {
    long insert(Connection connection, BillPayment billPayment) throws SQLException;

    List<BillPaymentView> findByCustomer(Connection connection, long customerId, String paymentType,
                                         int limit) throws SQLException;
}
