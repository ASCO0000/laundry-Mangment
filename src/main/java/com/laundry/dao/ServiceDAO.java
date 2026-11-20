package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.Service;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for Laundry Services.
 */
public class ServiceDAO {

    public Service save(Service service) {
        String sql = "INSERT INTO services (service_name, description, pricing_unit, price, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, service.getServiceName());
            ps.setString(2, service.getDescription());
            ps.setString(3, service.getPricingUnit());
            ps.setBigDecimal(4, service.getPrice());
            ps.setBoolean(5, service.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    service.setServiceId(rs.getInt(1));
                }
            }
            return service;
        } catch (SQLException e) {
            throw new DatabaseException("Failed adding laundry service: " + e.getMessage(), e);
        }
    }

    public void update(Service service) {
        String sql = "UPDATE services SET service_name = ?, description = ?, pricing_unit = ?, price = ?, active = ? WHERE service_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service.getServiceName());
            ps.setString(2, service.getDescription());
            ps.setString(3, service.getPricingUnit());
            ps.setBigDecimal(4, service.getPrice());
            ps.setBoolean(5, service.isActive());
            ps.setInt(6, service.getServiceId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating laundry service: " + e.getMessage(), e);
        }
    }

    public Service findById(int serviceId) {
        String sql = "SELECT * FROM services WHERE service_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding service with ID: " + serviceId, e);
        }
        return null;
    }

    public List<Service> findAll(boolean onlyActive) {
        List<Service> list = new ArrayList<>();
        String sql = onlyActive ? "SELECT * FROM services WHERE active = TRUE ORDER BY service_name ASC"
                                : "SELECT * FROM services ORDER BY service_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed retrieving laundry services", e);
        }
        return list;
    }

    public List<Service> search(String keyword) {
        List<Service> list = new ArrayList<>();
        String sql = "SELECT * FROM services WHERE service_name LIKE ? OR description LIKE ? ORDER BY service_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed searching services with keyword: " + keyword, e);
        }
        return list;
    }

    public void delete(int serviceId) {
        String sql = "DELETE FROM services WHERE service_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                throw new DatabaseException("Cannot delete service referenced in historical orders. Deactivate it instead.", e);
            }
            throw new DatabaseException("Failed deleting service: " + e.getMessage(), e);
        }
    }

    public java.util.Map<String, Object> getServiceSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalServices", 0);
        summary.put("activeServices", 0);
        summary.put("inactiveServices", 0);
        summary.put("averagePrice", java.math.BigDecimal.ZERO);

        String sql = "SELECT "
                   + "COUNT(*) AS total, "
                   + "COALESCE(SUM(CASE WHEN active = TRUE THEN 1 ELSE 0 END), 0) AS active_cnt, "
                   + "COALESCE(SUM(CASE WHEN active = FALSE THEN 1 ELSE 0 END), 0) AS inactive_cnt, "
                   + "COALESCE(AVG(price), 0) AS avg_price "
                   + "FROM services";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                summary.put("totalServices", rs.getInt("total"));
                summary.put("activeServices", rs.getInt("active_cnt"));
                summary.put("inactiveServices", rs.getInt("inactive_cnt"));
                java.math.BigDecimal avg = rs.getBigDecimal("avg_price");
                summary.put("averagePrice", avg != null ? avg.setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed fetching service summary: " + e.getMessage(), e);
        }
        return summary;
    }

    private Service mapRow(ResultSet rs) throws SQLException {
        Service s = new Service();
        s.setServiceId(rs.getInt("service_id"));
        s.setServiceName(rs.getString("service_name"));
        s.setDescription(rs.getString("description"));
        s.setPricingUnit(rs.getString("pricing_unit"));
        s.setPrice(rs.getBigDecimal("price"));
        s.setActive(rs.getBoolean("active"));
        return s;
    }
}
