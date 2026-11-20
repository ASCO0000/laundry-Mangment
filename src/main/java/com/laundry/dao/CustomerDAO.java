package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.Customer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for Customer entities.
 */
public class CustomerDAO {

    public Customer save(Customer customer) {
        String sql = "INSERT INTO customers (full_name, phone, email, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    customer.setCustomerId(rs.getInt(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            throw new DatabaseException("Failed saving customer: " + e.getMessage(), e);
        }
    }

    public void update(Customer customer) {
        String sql = "UPDATE customers SET full_name = ?, phone = ?, email = ?, address = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getPhone());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setInt(5, customer.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating customer: " + e.getMessage(), e);
        }
    }

    public void delete(int customerId) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                throw new DatabaseException("Cannot delete customer with existing orders. Archive or clear orders first.", e);
            }
            throw new DatabaseException("Failed deleting customer: " + e.getMessage(), e);
        }
    }

    public Customer findById(int customerId) {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding customer by ID: " + customerId, e);
        }
        return null;
    }

    public Customer findByPhone(String phone) {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding customer by phone: " + phone, e);
        }
        return null;
    }

    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed listing customers", e);
        }
        return list;
    }

    public List<Customer> search(String keyword) {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE full_name LIKE ? OR phone LIKE ? OR CAST(customer_id AS CHAR) LIKE ? ORDER BY customer_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed searching customers with keyword: " + keyword, e);
        }
        return list;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getInt("customer_id"));
        c.setFullName(rs.getString("full_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }
}
