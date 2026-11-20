package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.OrderItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for individual Order Items.
 */
public class OrderItemDAO {

    public List<OrderItem> findByOrderId(int orderId) {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT oi.*, s.service_name FROM order_items oi "
                   + "JOIN services s ON oi.service_id = s.service_id "
                   + "WHERE oi.order_id = ? ORDER BY oi.order_item_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setServiceId(rs.getInt("service_id"));
                    item.setServiceName(rs.getString("service_name"));
                    item.setItemDescription(rs.getString("item_description"));
                    item.setQuantity(rs.getBigDecimal("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding items for order ID: " + orderId, e);
        }
        return list;
    }

    public void saveItems(int orderId, List<OrderItem> items, Connection conn) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, service_id, item_description, quantity, unit_price, subtotal) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OrderItem item : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, item.getServiceId());
                ps.setString(3, item.getItemDescription());
                ps.setBigDecimal(4, item.getQuantity());
                ps.setBigDecimal(5, item.getUnitPrice());
                ps.setBigDecimal(6, item.getSubtotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
