package com.bank.dao.impl;

import com.bank.bean.WealthHolding;
import com.bank.bean.WealthOrder;
import com.bank.bean.WealthOrderConfirm;
import com.bank.bean.WealthProduct;
import com.bank.dao.WealthDao;
import com.bank.dto.WealthHoldingView;
import com.bank.dto.WealthOrderView;
import com.bank.dto.WealthOrderConfirmView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WealthDaoImpl implements WealthDao {
    @Override
    public List<WealthProduct> findProducts(Connection connection, String status) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT product_id, product_code, product_name, risk_level, product_type, expected_rate, ");
        sql.append("period_days, confirm_days, arrival_days, allow_early_redeem, ");
        sql.append("min_amount, max_amount, status, description, created_at, updated_at FROM t_wealth_product ");
        if (status != null && status.length() > 0) {
            sql.append("WHERE status = ? ");
        }
        sql.append("ORDER BY risk_level ASC, expected_rate ASC");
        List<WealthProduct> products = new ArrayList<WealthProduct>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            if (status != null && status.length() > 0) {
                statement.setString(1, status);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapProduct(resultSet));
                }
            }
        }
        return products;
    }

    @Override
    public WealthProduct findProductById(Connection connection, long productId) throws SQLException {
        String sql = "SELECT product_id, product_code, product_name, risk_level, product_type, expected_rate, "
                + "period_days, confirm_days, arrival_days, allow_early_redeem, "
                + "min_amount, max_amount, status, description, created_at, updated_at "
                + "FROM t_wealth_product WHERE product_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapProduct(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public long insertOrder(Connection connection, WealthOrder order) throws SQLException {
        String sql = "INSERT INTO t_wealth_order (order_no, customer_id, account_id, product_id, holding_id, "
                + "transaction_id, order_type, amount, confirmed_amount, income_amount, status, submit_time, "
                + "confirm_time, value_date, maturity_date, expected_arrival_date, completed_time, fail_reason) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, order.getOrderNo());
            statement.setLong(2, order.getCustomerId());
            statement.setLong(3, order.getAccountId());
            statement.setLong(4, order.getProductId());
            setNullableLong(statement, 5, order.getHoldingId());
            setNullableLong(statement, 6, order.getTransactionId());
            statement.setString(7, order.getOrderType());
            statement.setBigDecimal(8, order.getAmount());
            statement.setBigDecimal(9, order.getConfirmedAmount());
            statement.setBigDecimal(10, order.getIncomeAmount());
            statement.setString(11, order.getStatus());
            statement.setTimestamp(12, order.getSubmitTime());
            statement.setTimestamp(13, order.getConfirmTime());
            statement.setDate(14, order.getValueDate());
            statement.setDate(15, order.getMaturityDate());
            statement.setDate(16, order.getExpectedArrivalDate());
            statement.setTimestamp(17, order.getCompletedTime());
            statement.setString(18, order.getFailReason());
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
    public WealthOrder findOrderByIdForUpdate(Connection connection, long orderId) throws SQLException {
        String sql = wealthOrderSelectSql() + " WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapOrder(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<WealthOrder> findOrdersByStatusForUpdate(Connection connection, String orderType, String status,
                                                         int limit) throws SQLException {
        String sql = wealthOrderSelectSql()
                + " WHERE order_type = ? AND status = ? ORDER BY submit_time ASC, order_id ASC LIMIT ? FOR UPDATE";
        List<WealthOrder> orders = new ArrayList<WealthOrder>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderType);
            statement.setString(2, status);
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(mapOrder(resultSet));
                }
            }
        }
        return orders;
    }

    @Override
    public List<WealthOrderView> findRecentOrderViews(Connection connection, String status, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT o.order_id, o.order_no, c.full_name, a.account_no, p.product_code, p.product_name, ");
        sql.append("o.order_type, o.amount, o.confirmed_amount, o.income_amount, o.status, ");
        sql.append("o.submit_time, o.confirm_time, o.value_date, o.maturity_date, ");
        sql.append("o.expected_arrival_date, o.completed_time ");
        sql.append("FROM t_wealth_order o ");
        sql.append("JOIN t_customer c ON o.customer_id = c.customer_id ");
        sql.append("JOIN t_account a ON o.account_id = a.account_id ");
        sql.append("JOIN t_wealth_product p ON o.product_id = p.product_id WHERE 1 = 1 ");
        if (status != null && status.length() > 0) {
            sql.append("AND o.status = ? ");
        }
        sql.append("ORDER BY o.updated_at DESC, o.order_id DESC LIMIT ?");

        List<WealthOrderView> orders = new ArrayList<WealthOrderView>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(mapOrderView(resultSet));
                }
            }
        }
        return orders;
    }

    @Override
    public int countOrdersByStatus(Connection connection, String orderType, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM t_wealth_order WHERE order_type = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderType);
            statement.setString(2, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    @Override
    public void markBuyOrderConfirmed(Connection connection, long orderId, long holdingId, Timestamp confirmTime,
                                      java.sql.Date valueDate, java.sql.Date maturityDate) throws SQLException {
        String sql = "UPDATE t_wealth_order SET holding_id = ?, confirmed_amount = amount, status = 'BUY_CONFIRMED', "
                + "confirm_time = ?, value_date = ?, maturity_date = ?, completed_time = ? WHERE order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, holdingId);
            statement.setTimestamp(2, confirmTime);
            statement.setDate(3, valueDate);
            statement.setDate(4, maturityDate);
            statement.setTimestamp(5, confirmTime);
            statement.setLong(6, orderId);
            statement.executeUpdate();
        }
    }

    @Override
    public void markRedeemOrderCompleted(Connection connection, long orderId, long transactionId,
                                         Timestamp completedTime) throws SQLException {
        String sql = "UPDATE t_wealth_order SET transaction_id = ?, status = 'REDEEM_COMPLETED', "
                + "completed_time = ? WHERE order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transactionId);
            statement.setTimestamp(2, completedTime);
            statement.setLong(3, orderId);
            statement.executeUpdate();
        }
    }

    @Override
    public long insertHolding(Connection connection, WealthHolding holding) throws SQLException {
        String sql = "INSERT INTO t_wealth_holding (customer_id, account_id, product_id, buy_transaction_id, "
                + "buy_order_id, principal, expected_rate, buy_time, value_date, maturity_date, current_income, "
                + "estimated_value, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, holding.getCustomerId());
            statement.setLong(2, holding.getAccountId());
            statement.setLong(3, holding.getProductId());
            statement.setLong(4, holding.getBuyTransactionId());
            setNullableLong(statement, 5, holding.getBuyOrderId());
            statement.setBigDecimal(6, holding.getPrincipal());
            statement.setBigDecimal(7, holding.getExpectedRate());
            statement.setTimestamp(8, holding.getBuyTime());
            statement.setDate(9, holding.getValueDate());
            statement.setDate(10, holding.getMaturityDate());
            statement.setBigDecimal(11, holding.getCurrentIncome());
            statement.setBigDecimal(12, holding.getEstimatedValue());
            statement.setString(13, holding.getStatus());
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
    public long insertOrderConfirm(Connection connection, WealthOrderConfirm confirm) throws SQLException {
        String sql = "INSERT INTO t_wealth_order_confirm (transaction_id, customer_id, account_id, product_id, "
                + "amount, customer_risk_level, product_risk_level, match_result, disclosure_version, "
                + "product_disclosure_checked, non_deposit_checked, yield_not_guaranteed_checked, "
                + "account_confirmed, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, confirm.getTransactionId());
            statement.setLong(2, confirm.getCustomerId());
            statement.setLong(3, confirm.getAccountId());
            statement.setLong(4, confirm.getProductId());
            statement.setBigDecimal(5, confirm.getAmount());
            statement.setString(6, confirm.getCustomerRiskLevel());
            statement.setString(7, confirm.getProductRiskLevel());
            statement.setString(8, confirm.getMatchResult());
            statement.setString(9, confirm.getDisclosureVersion());
            statement.setBoolean(10, confirm.isProductDisclosureChecked());
            statement.setBoolean(11, confirm.isNonDepositChecked());
            statement.setBoolean(12, confirm.isYieldNotGuaranteedChecked());
            statement.setBoolean(13, confirm.isAccountConfirmed());
            statement.setString(14, confirm.getIpAddress());
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
    public WealthHolding findHoldingByIdForUpdate(Connection connection, long holdingId) throws SQLException {
        String sql = "SELECT holding_id, customer_id, account_id, product_id, buy_transaction_id, "
                + "redeem_transaction_id, buy_order_id, redeem_order_id, principal, expected_rate, buy_time, "
                + "value_date, maturity_date, redeem_time, current_income, estimated_value, status "
                + "FROM t_wealth_holding WHERE holding_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, holdingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapHolding(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public WealthHoldingView findHoldingViewById(Connection connection, long holdingId) throws SQLException {
        String sql = holdingViewSql() + " WHERE h.holding_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, holdingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapHoldingView(resultSet);
                }
            }
        }
        return null;
    }

    @Override
    public List<WealthHoldingView> findHoldingsByCustomer(Connection connection, long customerId)
            throws SQLException {
        String sql = holdingViewSql() + " WHERE h.customer_id = ? ORDER BY h.status ASC, h.buy_time DESC";
        List<WealthHoldingView> holdings = new ArrayList<WealthHoldingView>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    holdings.add(mapHoldingView(resultSet));
                }
            }
        }
        return holdings;
    }

    @Override
    public List<WealthOrderConfirmView> findOrderConfirmsByCustomer(Connection connection, long customerId, int limit)
            throws SQLException {
        String sql = "SELECT c.confirm_id, t.transaction_no, a.account_no, p.product_code, p.product_name, "
                + "c.amount, c.customer_risk_level, c.product_risk_level, c.match_result, "
                + "c.disclosure_version, c.ip_address, c.created_at "
                + "FROM t_wealth_order_confirm c "
                + "JOIN t_transaction t ON c.transaction_id = t.transaction_id "
                + "JOIN t_account a ON c.account_id = a.account_id "
                + "JOIN t_wealth_product p ON c.product_id = p.product_id "
                + "WHERE c.customer_id = ? ORDER BY c.created_at DESC, c.confirm_id DESC LIMIT ?";
        List<WealthOrderConfirmView> confirms = new ArrayList<WealthOrderConfirmView>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    confirms.add(mapOrderConfirmView(resultSet));
                }
            }
        }
        return confirms;
    }

    @Override
    public void markRedeemed(Connection connection, long holdingId, long redeemTransactionId,
                             BigDecimal currentIncome, Timestamp redeemTime) throws SQLException {
        String sql = "UPDATE t_wealth_holding SET redeem_transaction_id = ?, current_income = ?, "
                + "estimated_value = principal + ?, redeem_time = ?, status = 'REDEEMED' WHERE holding_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, redeemTransactionId);
            statement.setBigDecimal(2, currentIncome);
            statement.setBigDecimal(3, currentIncome);
            statement.setTimestamp(4, redeemTime);
            statement.setLong(5, holdingId);
            statement.executeUpdate();
        }
    }

    @Override
    public void markRedeeming(Connection connection, long holdingId, long redeemOrderId) throws SQLException {
        String sql = "UPDATE t_wealth_holding SET redeem_order_id = ?, status = 'REDEEMING' WHERE holding_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, redeemOrderId);
            statement.setLong(2, holdingId);
            statement.executeUpdate();
        }
    }

    private String holdingViewSql() {
        return "SELECT h.holding_id, h.account_id, a.account_no, h.product_id, p.product_code, p.product_name, "
                + "p.risk_level, p.product_type, p.allow_early_redeem, p.period_days, h.principal, "
                + "h.expected_rate, h.buy_time, h.value_date, h.maturity_date, ro.expected_arrival_date, "
                + "h.redeem_time, h.current_income, h.estimated_value, h.status "
                + "FROM t_wealth_holding h "
                + "JOIN t_account a ON h.account_id = a.account_id "
                + "JOIN t_wealth_product p ON h.product_id = p.product_id "
                + "LEFT JOIN t_wealth_order ro ON h.redeem_order_id = ro.order_id";
    }

    private WealthProduct mapProduct(ResultSet resultSet) throws SQLException {
        WealthProduct product = new WealthProduct();
        product.setProductId(resultSet.getLong("product_id"));
        product.setProductCode(resultSet.getString("product_code"));
        product.setProductName(resultSet.getString("product_name"));
        product.setRiskLevel(resultSet.getString("risk_level"));
        product.setProductType(resultSet.getString("product_type"));
        product.setExpectedRate(resultSet.getBigDecimal("expected_rate"));
        product.setPeriodDays(resultSet.getInt("period_days"));
        product.setConfirmDays(resultSet.getInt("confirm_days"));
        product.setArrivalDays(resultSet.getInt("arrival_days"));
        product.setAllowEarlyRedeem(resultSet.getBoolean("allow_early_redeem"));
        product.setMinAmount(resultSet.getBigDecimal("min_amount"));
        product.setMaxAmount(resultSet.getBigDecimal("max_amount"));
        product.setStatus(resultSet.getString("status"));
        product.setDescription(resultSet.getString("description"));
        product.setCreatedAt(resultSet.getTimestamp("created_at"));
        product.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return product;
    }

    private WealthHolding mapHolding(ResultSet resultSet) throws SQLException {
        WealthHolding holding = new WealthHolding();
        holding.setHoldingId(resultSet.getLong("holding_id"));
        holding.setCustomerId(resultSet.getLong("customer_id"));
        holding.setAccountId(resultSet.getLong("account_id"));
        holding.setProductId(resultSet.getLong("product_id"));
        holding.setBuyTransactionId(resultSet.getLong("buy_transaction_id"));
        long redeemTransactionId = resultSet.getLong("redeem_transaction_id");
        holding.setRedeemTransactionId(resultSet.wasNull() ? null : redeemTransactionId);
        long buyOrderId = resultSet.getLong("buy_order_id");
        holding.setBuyOrderId(resultSet.wasNull() ? null : buyOrderId);
        long redeemOrderId = resultSet.getLong("redeem_order_id");
        holding.setRedeemOrderId(resultSet.wasNull() ? null : redeemOrderId);
        holding.setPrincipal(resultSet.getBigDecimal("principal"));
        holding.setExpectedRate(resultSet.getBigDecimal("expected_rate"));
        holding.setBuyTime(resultSet.getTimestamp("buy_time"));
        holding.setValueDate(resultSet.getDate("value_date"));
        holding.setMaturityDate(resultSet.getDate("maturity_date"));
        holding.setRedeemTime(resultSet.getTimestamp("redeem_time"));
        holding.setCurrentIncome(resultSet.getBigDecimal("current_income"));
        holding.setEstimatedValue(resultSet.getBigDecimal("estimated_value"));
        holding.setStatus(resultSet.getString("status"));
        return holding;
    }

    private WealthHoldingView mapHoldingView(ResultSet resultSet) throws SQLException {
        WealthHoldingView view = new WealthHoldingView();
        view.setHoldingId(resultSet.getLong("holding_id"));
        view.setAccountId(resultSet.getLong("account_id"));
        view.setAccountNo(resultSet.getString("account_no"));
        view.setProductId(resultSet.getLong("product_id"));
        view.setProductCode(resultSet.getString("product_code"));
        view.setProductName(resultSet.getString("product_name"));
        view.setRiskLevel(resultSet.getString("risk_level"));
        view.setProductType(resultSet.getString("product_type"));
        view.setAllowEarlyRedeem(resultSet.getBoolean("allow_early_redeem"));
        view.setPeriodDays(resultSet.getInt("period_days"));
        view.setPrincipal(resultSet.getBigDecimal("principal"));
        view.setExpectedRate(resultSet.getBigDecimal("expected_rate"));
        view.setBuyTime(resultSet.getTimestamp("buy_time"));
        view.setValueDate(resultSet.getDate("value_date"));
        view.setMaturityDate(resultSet.getDate("maturity_date"));
        view.setExpectedArrivalDate(resultSet.getDate("expected_arrival_date"));
        view.setRedeemTime(resultSet.getTimestamp("redeem_time"));
        view.setCurrentIncome(resultSet.getBigDecimal("current_income"));
        view.setEstimatedValue(resultSet.getBigDecimal("estimated_value"));
        view.setStatus(resultSet.getString("status"));
        return view;
    }

    private WealthOrder mapOrder(ResultSet resultSet) throws SQLException {
        WealthOrder order = new WealthOrder();
        order.setOrderId(resultSet.getLong("order_id"));
        order.setOrderNo(resultSet.getString("order_no"));
        order.setCustomerId(resultSet.getLong("customer_id"));
        order.setAccountId(resultSet.getLong("account_id"));
        order.setProductId(resultSet.getLong("product_id"));
        long holdingId = resultSet.getLong("holding_id");
        order.setHoldingId(resultSet.wasNull() ? null : holdingId);
        long transactionId = resultSet.getLong("transaction_id");
        order.setTransactionId(resultSet.wasNull() ? null : transactionId);
        order.setOrderType(resultSet.getString("order_type"));
        order.setAmount(resultSet.getBigDecimal("amount"));
        order.setConfirmedAmount(resultSet.getBigDecimal("confirmed_amount"));
        order.setIncomeAmount(resultSet.getBigDecimal("income_amount"));
        order.setStatus(resultSet.getString("status"));
        order.setSubmitTime(resultSet.getTimestamp("submit_time"));
        order.setConfirmTime(resultSet.getTimestamp("confirm_time"));
        order.setValueDate(resultSet.getDate("value_date"));
        order.setMaturityDate(resultSet.getDate("maturity_date"));
        order.setExpectedArrivalDate(resultSet.getDate("expected_arrival_date"));
        order.setCompletedTime(resultSet.getTimestamp("completed_time"));
        order.setFailReason(resultSet.getString("fail_reason"));
        order.setCreatedAt(resultSet.getTimestamp("created_at"));
        order.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return order;
    }

    private WealthOrderView mapOrderView(ResultSet resultSet) throws SQLException {
        WealthOrderView view = new WealthOrderView();
        view.setOrderId(resultSet.getLong("order_id"));
        view.setOrderNo(resultSet.getString("order_no"));
        view.setFullName(resultSet.getString("full_name"));
        view.setAccountNo(resultSet.getString("account_no"));
        view.setProductCode(resultSet.getString("product_code"));
        view.setProductName(resultSet.getString("product_name"));
        view.setOrderType(resultSet.getString("order_type"));
        view.setAmount(resultSet.getBigDecimal("amount"));
        view.setConfirmedAmount(resultSet.getBigDecimal("confirmed_amount"));
        view.setIncomeAmount(resultSet.getBigDecimal("income_amount"));
        view.setStatus(resultSet.getString("status"));
        view.setSubmitTime(resultSet.getTimestamp("submit_time"));
        view.setConfirmTime(resultSet.getTimestamp("confirm_time"));
        view.setValueDate(resultSet.getDate("value_date"));
        view.setMaturityDate(resultSet.getDate("maturity_date"));
        view.setExpectedArrivalDate(resultSet.getDate("expected_arrival_date"));
        view.setCompletedTime(resultSet.getTimestamp("completed_time"));
        return view;
    }

    private WealthOrderConfirmView mapOrderConfirmView(ResultSet resultSet) throws SQLException {
        WealthOrderConfirmView view = new WealthOrderConfirmView();
        view.setConfirmId(resultSet.getLong("confirm_id"));
        view.setTransactionNo(resultSet.getString("transaction_no"));
        view.setAccountNo(resultSet.getString("account_no"));
        view.setProductCode(resultSet.getString("product_code"));
        view.setProductName(resultSet.getString("product_name"));
        view.setAmount(resultSet.getBigDecimal("amount"));
        view.setCustomerRiskLevel(resultSet.getString("customer_risk_level"));
        view.setProductRiskLevel(resultSet.getString("product_risk_level"));
        view.setMatchResult(resultSet.getString("match_result"));
        view.setDisclosureVersion(resultSet.getString("disclosure_version"));
        view.setIpAddress(resultSet.getString("ip_address"));
        view.setCreatedAt(resultSet.getTimestamp("created_at"));
        return view;
    }

    private String wealthOrderSelectSql() {
        return "SELECT order_id, order_no, customer_id, account_id, product_id, holding_id, transaction_id, "
                + "order_type, amount, confirmed_amount, income_amount, status, submit_time, confirm_time, "
                + "value_date, maturity_date, expected_arrival_date, completed_time, fail_reason, "
                + "created_at, updated_at FROM t_wealth_order";
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
