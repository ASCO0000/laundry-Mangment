package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import com.laundry.model.OrderStatusHistory;
import com.laundry.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for Orders, including atomic transactions for order saving.
 */
public class OrderDAO {

    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final OrderStatusHistoryDAO historyDAO = new OrderStatusHistoryDAO();

    /**
     * Atomically saves an order along with its line items and initial status record.
     */
    public Order saveOrderWithItems(Order order) {
        String sql = "INSERT INTO orders (customer_id, order_date, expected_delivery_date, status, "
                   + "subtotal, discount, total_amount, special_instructions, created_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, order.getCustomerId());
                    ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now()));
                    ps.setDate(3, Date.valueOf(order.getExpectedDeliveryDate()));
                    ps.setString(4, order.getStatus().getDisplayName());
                    ps.setBigDecimal(5, order.getSubtotal());
                    ps.setBigDecimal(6, order.getDiscount() != null ? order.getDiscount() : BigDecimal.ZERO);
                    ps.setBigDecimal(7, order.getTotalAmount());
                    ps.setString(8, order.getSpecialInstructions());
                    ps.setInt(9, order.getCreatedBy());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            order.setOrderId(rs.getInt(1));
                        }
                    }
                }

                // Save items
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    orderItemDAO.saveItems(order.getOrderId(), order.getItems(), conn);
                }

                // Initial status entry
                OrderStatusHistory initialHistory = new OrderStatusHistory();
                initialHistory.setOrderId(order.getOrderId());
                initialHistory.setOldStatus(null);
                initialHistory.setNewStatus(order.getStatus().getDisplayName());
                initialHistory.setChangedBy(order.getCreatedBy());
                initialHistory.setChangedAt(LocalDateTime.now());
                initialHistory.setRemarks("Order created");
                historyDAO.save(initialHistory, conn);

                conn.commit();
                return order;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Transaction rolled back while creating order: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error in order creation: " + e.getMessage(), e);
        }
    }

    public Order findById(int orderId) {
        String sql = "SELECT o.*, c.full_name AS customer_name, c.phone AS customer_phone, "
                   + "u.full_name AS staff_name, "
                   + "(SELECT COALESCE(SUM(amount), 0) FROM payments WHERE order_id = o.order_id) AS paid_amount "
                   + "FROM orders o "
                   + "JOIN customers c ON o.customer_id = c.customer_id "
                   + "JOIN users u ON o.created_by = u.user_id "
                   + "WHERE o.order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapRow(rs);
                    order.setItems(orderItemDAO.findByOrderId(orderId));
                    return order;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding order by ID: " + orderId, e);
        }
        return null;
    }

    public List<Order> findAll(String statusFilter, String searchKeyword) {
        List<Order> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT o.*, c.full_name AS customer_name, c.phone AS customer_phone, "
              + "u.full_name AS staff_name, "
              + "(SELECT COALESCE(SUM(amount), 0) FROM payments WHERE order_id = o.order_id) AS paid_amount "
              + "FROM orders o "
              + "JOIN customers c ON o.customer_id = c.customer_id "
              + "JOIN users u ON o.created_by = u.user_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.equalsIgnoreCase("All")) {
            sql.append("AND o.status = ? ");
            params.add(statusFilter);
        }

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append("AND (CAST(o.order_id AS CHAR) LIKE ? OR c.full_name LIKE ? OR c.phone LIKE ?) ");
            String pat = "%" + searchKeyword.trim() + "%";
            params.add(pat);
            params.add(pat);
            params.add(pat);
        }

        sql.append("ORDER BY o.order_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed fetching orders list", e);
        }
        return list;
    }

    /**
     * Updates order status and logs the historical change within an atomic transaction.
     */
    public void updateStatus(int orderId, OrderStatus newStatus, int changedByUserId, String remarks) {
        String queryCurrent = "SELECT status FROM orders WHERE order_id = ?";
        String updateSql = "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String oldStatus = null;
                try (PreparedStatement ps = conn.prepareStatement(queryCurrent)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("status");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, newStatus.getDisplayName());
                    ps.setInt(2, orderId);
                    ps.executeUpdate();
                }

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrderId(orderId);
                history.setOldStatus(oldStatus);
                history.setNewStatus(newStatus.getDisplayName());
                history.setChangedBy(changedByUserId);
                history.setChangedAt(LocalDateTime.now());
                history.setRemarks(remarks);
                historyDAO.save(history, conn);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Transaction rolled back updating order status: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating order status", e);
        }
    }

    public List<Order> findDelayedOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, c.full_name AS customer_name, c.phone AS customer_phone, u.full_name AS staff_name, "
                   + "(SELECT COALESCE(SUM(amount), 0) FROM payments WHERE order_id = o.order_id) AS paid_amount "
                   + "FROM orders o "
                   + "JOIN customers c ON o.customer_id = c.customer_id "
                   + "JOIN users u ON o.created_by = u.user_id "
                   + "WHERE o.expected_delivery_date < CURRENT_DATE AND o.status NOT IN ('Delivered', 'Cancelled') "
                   + "ORDER BY o.expected_delivery_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed fetching delayed orders", e);
        }
        return list;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setCustomerId(rs.getInt("customer_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerPhone(rs.getString("customer_phone"));
        Timestamp ots = rs.getTimestamp("order_date");
        if (ots != null) {
            o.setOrderDate(ots.toLocalDateTime());
        }
        Date ed = rs.getDate("expected_delivery_date");
        if (ed != null) {
            o.setExpectedDeliveryDate(ed.toLocalDate());
        }
        o.setStatus(OrderStatus.fromString(rs.getString("status")));
        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setDiscount(rs.getBigDecimal("discount"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setSpecialInstructions(rs.getString("special_instructions"));
        o.setCreatedBy(rs.getInt("created_by"));
        o.setCreatedByName(rs.getString("staff_name"));

        BigDecimal paid = rs.getBigDecimal("paid_amount");
        o.setPaidAmount(paid != null ? paid : BigDecimal.ZERO);
        o.setBalanceDue(o.getTotalAmount().subtract(o.getPaidAmount()).max(BigDecimal.ZERO));
        return o;
    }
}
