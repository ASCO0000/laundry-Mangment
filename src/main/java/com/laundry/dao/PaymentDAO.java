package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.Payment;
import com.laundry.model.enums.PaymentMethod;
import com.laundry.model.enums.PaymentStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for payments and revenue tracking.
 */
public class PaymentDAO {

    public Payment save(Payment payment) {
        String sql = "INSERT INTO payments (order_id, payment_date, amount, payment_method, transaction_reference, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, payment.getOrderId());
            ps.setTimestamp(2, Timestamp.valueOf(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDateTime.now()));
            ps.setBigDecimal(3, payment.getAmount());
            ps.setString(4, payment.getPaymentMethod().getDisplayName());
            ps.setString(5, payment.getTransactionReference());
            ps.setString(6, payment.getStatus().getDisplayName());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    payment.setPaymentId(rs.getInt(1));
                }
            }
            return payment;
        } catch (SQLException e) {
            throw new DatabaseException("Failed recording payment: " + e.getMessage(), e);
        }
    }

    public List<Payment> findByOrderId(int orderId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY payment_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving payments for order ID: " + orderId, e);
        }
        return list;
    }

    public BigDecimal getTotalPaidForOrder(int orderId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE order_id = ? AND status = 'Completed'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed calculating paid balance for order: " + orderId, e);
        }
        return BigDecimal.ZERO;
    }

    public List<Payment> findAllPayments() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, o.customer_id, c.full_name AS customer_name "
                   + "FROM payments p "
                   + "JOIN orders o ON p.order_id = o.order_id "
                   + "JOIN customers c ON o.customer_id = c.customer_id "
                   + "ORDER BY p.payment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving all payments", e);
        }
        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setOrderId(rs.getInt("order_id"));
        Timestamp ts = rs.getTimestamp("payment_date");
        if (ts != null) {
            p.setPaymentDate(ts.toLocalDateTime());
        }
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(PaymentMethod.fromString(rs.getString("payment_method")));
        p.setTransactionReference(rs.getString("transaction_reference"));
        p.setStatus(PaymentStatus.fromString(rs.getString("status")));
        return p;
    }
}
