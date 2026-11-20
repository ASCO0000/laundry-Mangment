package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.OrderStatusHistory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Audit trail operations for tracking order status movements.
 */
public class OrderStatusHistoryDAO {

    public void save(OrderStatusHistory history, Connection conn) throws SQLException {
        String sql = "INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, changed_at, remarks) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, history.getOrderId());
            if (history.getOldStatus() != null) {
                ps.setString(2, history.getOldStatus());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, history.getNewStatus());
            if (history.getChangedBy() != null) {
                ps.setInt(4, history.getChangedBy());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setTimestamp(5, Timestamp.valueOf(history.getChangedAt()));
            ps.setString(6, history.getRemarks());
            ps.executeUpdate();
        }
    }

    public List<OrderStatusHistory> findByOrderId(int orderId) {
        List<OrderStatusHistory> list = new ArrayList<>();
        String sql = "SELECT h.*, u.full_name AS staff_name FROM order_status_history h "
                   + "LEFT JOIN users u ON h.changed_by = u.user_id "
                   + "WHERE h.order_id = ? ORDER BY h.history_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatusHistory h = new OrderStatusHistory();
                    h.setHistoryId(rs.getInt("history_id"));
                    h.setOrderId(rs.getInt("order_id"));
                    h.setOldStatus(rs.getString("old_status"));
                    h.setNewStatus(rs.getString("new_status"));
                    int uid = rs.getInt("changed_by");
                    if (!rs.wasNull()) {
                        h.setChangedBy(uid);
                    }
                    h.setChangedByName(rs.getString("staff_name"));
                    Timestamp ts = rs.getTimestamp("changed_at");
                    if (ts != null) {
                        h.setChangedAt(ts.toLocalDateTime());
                    }
                    h.setRemarks(rs.getString("remarks"));
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding status history for order: " + orderId, e);
        }
        return list;
    }
}
