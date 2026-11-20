package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.Delivery;
import com.laundry.model.enums.DeliveryStatus;
import com.laundry.model.enums.DeliveryType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for Pickups and Deliveries.
 */
public class DeliveryDAO {

    public Delivery save(Delivery delivery) {
        String sql = "INSERT INTO deliveries (order_id, delivery_type, address, scheduled_datetime, assigned_staff_id, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, delivery.getOrderId());
            ps.setString(2, delivery.getDeliveryType().getDisplayName());
            ps.setString(3, delivery.getAddress());
            ps.setTimestamp(4, Timestamp.valueOf(delivery.getScheduledDatetime() != null ? delivery.getScheduledDatetime() : LocalDateTime.now()));
            if (delivery.getAssignedStaffId() != null) {
                ps.setInt(5, delivery.getAssignedStaffId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, delivery.getStatus().getDisplayName());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    delivery.setDeliveryId(rs.getInt(1));
                }
            }
            return delivery;
        } catch (SQLException e) {
            throw new DatabaseException("Failed scheduling pickup/delivery: " + e.getMessage(), e);
        }
    }

    public void update(Delivery delivery) {
        String sql = "UPDATE deliveries SET delivery_type = ?, address = ?, scheduled_datetime = ?, "
                   + "assigned_staff_id = ?, status = ? WHERE delivery_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, delivery.getDeliveryType().getDisplayName());
            ps.setString(2, delivery.getAddress());
            ps.setTimestamp(3, Timestamp.valueOf(delivery.getScheduledDatetime()));
            if (delivery.getAssignedStaffId() != null) {
                ps.setInt(4, delivery.getAssignedStaffId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, delivery.getStatus().getDisplayName());
            ps.setInt(6, delivery.getDeliveryId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating pickup/delivery record: " + e.getMessage(), e);
        }
    }

    public void updateStatus(int deliveryId, DeliveryStatus status) {
        String sql = "UPDATE deliveries SET status = ? WHERE delivery_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.getDisplayName());
            ps.setInt(2, deliveryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating delivery status", e);
        }
    }

    public Delivery findById(int deliveryId) {
        String sql = "SELECT d.*, c.full_name AS customer_name, c.phone AS customer_phone, u.full_name AS staff_name "
                   + "FROM deliveries d "
                   + "JOIN orders o ON d.order_id = o.order_id "
                   + "JOIN customers c ON o.customer_id = c.customer_id "
                   + "LEFT JOIN users u ON d.assigned_staff_id = u.user_id "
                   + "WHERE d.delivery_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding delivery by ID: " + deliveryId, e);
        }
        return null;
    }

    public List<Delivery> findAll(String typeFilter, String statusFilter) {
        List<Delivery> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT d.*, c.full_name AS customer_name, c.phone AS customer_phone, u.full_name AS staff_name "
              + "FROM deliveries d "
              + "JOIN orders o ON d.order_id = o.order_id "
              + "JOIN customers c ON o.customer_id = c.customer_id "
              + "LEFT JOIN users u ON d.assigned_staff_id = u.user_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (typeFilter != null && !typeFilter.equalsIgnoreCase("All")) {
            sql.append("AND d.delivery_type = ? ");
            params.add(typeFilter);
        }
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("All")) {
            sql.append("AND d.status = ? ");
            params.add(statusFilter);
        }
        sql.append("ORDER BY d.scheduled_datetime ASC");

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
            throw new DatabaseException("Failed loading deliveries list", e);
        }
        return list;
    }

    private Delivery mapRow(ResultSet rs) throws SQLException {
        Delivery d = new Delivery();
        d.setDeliveryId(rs.getInt("delivery_id"));
        d.setOrderId(rs.getInt("order_id"));
        d.setCustomerName(rs.getString("customer_name"));
        d.setCustomerPhone(rs.getString("customer_phone"));
        d.setDeliveryType(DeliveryType.fromString(rs.getString("delivery_type")));
        d.setAddress(rs.getString("address"));
        Timestamp ts = rs.getTimestamp("scheduled_datetime");
        if (ts != null) {
            d.setScheduledDatetime(ts.toLocalDateTime());
        }
        int staffId = rs.getInt("assigned_staff_id");
        if (!rs.wasNull()) {
            d.setAssignedStaffId(staffId);
        }
        d.setAssignedStaffName(rs.getString("staff_name"));
        d.setStatus(DeliveryStatus.fromString(rs.getString("status")));
        return d;
    }
}
