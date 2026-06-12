package com.bank.dao;

import com.bank.bean.WealthHolding;
import com.bank.bean.WealthOrder;
import com.bank.bean.WealthOrderConfirm;
import com.bank.bean.WealthProduct;
import com.bank.dto.WealthOrderView;
import com.bank.dto.WealthOrderConfirmView;
import com.bank.dto.WealthHoldingView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface WealthDao {
    List<WealthProduct> findProducts(Connection connection, String status) throws SQLException;

    WealthProduct findProductById(Connection connection, long productId) throws SQLException;

    long insertOrder(Connection connection, WealthOrder order) throws SQLException;

    WealthOrder findOrderByIdForUpdate(Connection connection, long orderId) throws SQLException;

    List<WealthOrder> findOrdersByStatusForUpdate(Connection connection, String orderType, String status, int limit)
            throws SQLException;

    List<WealthOrderView> findRecentOrderViews(Connection connection, String status, int limit) throws SQLException;

    int countOrdersByStatus(Connection connection, String orderType, String status) throws SQLException;

    void markBuyOrderConfirmed(Connection connection, long orderId, long holdingId, Timestamp confirmTime,
                               java.sql.Date valueDate, java.sql.Date maturityDate) throws SQLException;

    void markRedeemOrderCompleted(Connection connection, long orderId, long transactionId,
                                  Timestamp completedTime) throws SQLException;

    long insertHolding(Connection connection, WealthHolding holding) throws SQLException;

    long insertOrderConfirm(Connection connection, WealthOrderConfirm confirm) throws SQLException;

    WealthHolding findHoldingByIdForUpdate(Connection connection, long holdingId) throws SQLException;

    WealthHoldingView findHoldingViewById(Connection connection, long holdingId) throws SQLException;

    List<WealthHoldingView> findHoldingsByCustomer(Connection connection, long customerId) throws SQLException;

    List<WealthOrderConfirmView> findOrderConfirmsByCustomer(Connection connection, long customerId, int limit)
            throws SQLException;

    void markRedeemed(Connection connection, long holdingId, long redeemTransactionId,
                      BigDecimal currentIncome, Timestamp redeemTime) throws SQLException;

    void markRedeeming(Connection connection, long holdingId, long redeemOrderId) throws SQLException;
}
