package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import com.laundry.model.CustomerTicket;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access operations for Customer support tickets, missing item claims,
 * and monetary refund requests.
 */
public class CustomerTicketDAO {

    public CustomerTicket save(CustomerTicket ticket) {
        String sql = "INSERT INTO customer_tickets ("
                   + "ticket_number, ticket_type, order_id, customer_id, customer_name, customer_phone, "
                   + "category, subject, description, item_details, amount_claimed, payout_details, "
                   + "status, priority, admin_notes, created_at, updated_at"
                   + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Generate temporary or preliminary ticket number
            String prefix = "CMP-";
            if ("Missing_Item".equalsIgnoreCase(ticket.getTicketType())) {
                prefix = "MIS-";
            } else if ("Refund_Request".equalsIgnoreCase(ticket.getTicketType())) {
                prefix = "REF-";
            }
            if (ticket.getTicketNumber() == null || ticket.getTicketNumber().trim().isEmpty()) {
                ticket.setTicketNumber(prefix + (System.currentTimeMillis() % 90000 + 10000));
            }

            ps.setString(1, ticket.getTicketNumber());
            ps.setString(2, ticket.getTicketType());
            if (ticket.getOrderId() != null && ticket.getOrderId() > 0 && orderExists(conn, ticket.getOrderId())) {
                ps.setInt(3, ticket.getOrderId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
                if (ticket.getOrderId() != null && ticket.getOrderId() > 0) {
                    if (ticket.getDescription() != null && !ticket.getDescription().contains("ORD-")) {
                        ticket.setDescription("[Order Ref: ORD-" + ticket.getOrderId() + "] " + ticket.getDescription());
                    }
                }
            }
            if (ticket.getCustomerId() != null && ticket.getCustomerId() > 0 && customerExists(conn, ticket.getCustomerId())) {
                ps.setInt(4, ticket.getCustomerId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, ticket.getCustomerName());
            ps.setString(6, ticket.getCustomerPhone());
            ps.setString(7, ticket.getCategory());
            ps.setString(8, ticket.getSubject());
            ps.setString(9, ticket.getDescription());
            ps.setString(10, ticket.getItemDetails());
            ps.setBigDecimal(11, ticket.getAmountClaimed() != null ? ticket.getAmountClaimed() : BigDecimal.ZERO);
            ps.setString(12, ticket.getPayoutDetails());
            ps.setString(13, ticket.getStatus() != null ? ticket.getStatus() : "Open");
            ps.setString(14, ticket.getPriority() != null ? ticket.getPriority() : "Medium");
            ps.setString(15, ticket.getAdminNotes());

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(16, now);
            ps.setTimestamp(17, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    ticket.setTicketId(generatedId);
                    // Standardize ticket number to deterministic identifier: e.g. CMP-1005
                    String standardizedNumber = prefix + (1000 + generatedId);
                    updateTicketNumber(conn, generatedId, standardizedNumber);
                    ticket.setTicketNumber(standardizedNumber);
                }
            }
            return ticket;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save customer ticket: " + e.getMessage(), e);
        }
    }

    private void updateTicketNumber(Connection conn, int ticketId, String ticketNumber) {
        String sql = "UPDATE customer_tickets SET ticket_number = ? WHERE ticket_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketNumber);
            ps.setInt(2, ticketId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public CustomerTicket findById(int ticketId) {
        String sql = "SELECT * FROM customer_tickets WHERE ticket_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding ticket by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public CustomerTicket findByTicketNumber(String ticketNumber) {
        if (ticketNumber == null) return null;
        String sql = "SELECT * FROM customer_tickets WHERE UPPER(ticket_number) = UPPER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ticketNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed finding ticket by number: " + e.getMessage(), e);
        }
        return null;
    }

    public List<CustomerTicket> findByPhone(String phone) {
        List<CustomerTicket> list = new ArrayList<>();
        if (phone == null || phone.trim().isEmpty()) return list;
        String sql = "SELECT * FROM customer_tickets WHERE customer_phone LIKE ? ORDER BY ticket_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + phone.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed querying tickets by phone: " + e.getMessage(), e);
        }
        return list;
    }

    public List<CustomerTicket> findByOrderId(int orderId) {
        List<CustomerTicket> list = new ArrayList<>();
        String sql = "SELECT * FROM customer_tickets WHERE order_id = ? ORDER BY ticket_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed querying tickets by order ID: " + e.getMessage(), e);
        }
        return list;
    }

    public List<CustomerTicket> findAll(String typeFilter, String statusFilter, String searchKeyword) {
        List<CustomerTicket> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM customer_tickets WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (typeFilter != null && !typeFilter.equalsIgnoreCase("All")) {
            sql.append("AND ticket_type = ? ");
            params.add(typeFilter);
        }

        if (statusFilter != null && !statusFilter.equalsIgnoreCase("All")) {
            sql.append("AND status = ? ");
            params.add(statusFilter);
        }

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append("AND (ticket_number LIKE ? OR customer_name LIKE ? OR customer_phone LIKE ? OR CAST(order_id AS CHAR) LIKE ? OR subject LIKE ?) ");
            String pat = "%" + searchKeyword.trim() + "%";
            params.add(pat);
            params.add(pat);
            params.add(pat);
            params.add(pat);
            params.add(pat);
        }

        sql.append("ORDER BY ticket_id DESC");

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
            throw new DatabaseException("Failed retrieving tickets list: " + e.getMessage(), e);
        }
        return list;
    }

    public void updateStatusAndNotes(int ticketId, String status, String adminNotes) {
        String sql = "UPDATE customer_tickets SET status = ?, admin_notes = ?, updated_at = CURRENT_TIMESTAMP WHERE ticket_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminNotes);
            ps.setInt(3, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed updating ticket status and notes: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getTicketStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT "
                   + "COUNT(*) AS total_tickets, "
                   + "SUM(CASE WHEN status != 'Resolved' AND status != 'Rejected' THEN 1 ELSE 0 END) AS active_tickets, "
                   + "SUM(CASE WHEN ticket_type = 'Complaint' AND status != 'Resolved' AND status != 'Rejected' THEN 1 ELSE 0 END) AS open_complaints, "
                   + "SUM(CASE WHEN ticket_type = 'Missing_Item' AND status != 'Resolved' AND status != 'Rejected' THEN 1 ELSE 0 END) AS missing_items, "
                   + "SUM(CASE WHEN ticket_type = 'Refund_Request' AND status != 'Resolved' AND status != 'Rejected' THEN 1 ELSE 0 END) AS pending_refunds, "
                   + "COALESCE(SUM(CASE WHEN ticket_type = 'Refund_Request' AND status != 'Rejected' THEN amount_claimed ELSE 0 END), 0) AS total_refund_amount "
                   + "FROM customer_tickets";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("totalTickets", rs.getInt("total_tickets"));
                stats.put("activeTickets", rs.getInt("active_tickets"));
                stats.put("openComplaints", rs.getInt("open_complaints"));
                stats.put("missingItems", rs.getInt("missing_items"));
                stats.put("pendingRefunds", rs.getInt("pending_refunds"));
                stats.put("totalRefundAmount", rs.getBigDecimal("total_refund_amount"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed calculating ticket metrics: " + e.getMessage(), e);
        }
        return stats;
    }

    private CustomerTicket mapRow(ResultSet rs) throws SQLException {
        CustomerTicket t = new CustomerTicket();
        t.setTicketId(rs.getInt("ticket_id"));
        t.setTicketNumber(rs.getString("ticket_number"));
        t.setTicketType(rs.getString("ticket_type"));
        int orderId = rs.getInt("order_id");
        if (!rs.wasNull()) {
            t.setOrderId(orderId);
        }
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) {
            t.setCustomerId(custId);
        }
        t.setCustomerName(rs.getString("customer_name"));
        t.setCustomerPhone(rs.getString("customer_phone"));
        t.setCategory(rs.getString("category"));
        t.setSubject(rs.getString("subject"));
        t.setDescription(rs.getString("description"));
        t.setItemDetails(rs.getString("item_details"));
        t.setAmountClaimed(rs.getBigDecimal("amount_claimed"));
        t.setPayoutDetails(rs.getString("payout_details"));
        t.setStatus(rs.getString("status"));
        t.setPriority(rs.getString("priority"));
        t.setAdminNotes(rs.getString("admin_notes"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) t.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) t.setUpdatedAt(uat.toLocalDateTime());
        return t;
    }

    private boolean orderExists(Connection conn, int orderId) {
        String sql = "SELECT 1 FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean customerExists(Connection conn, int customerId) {
        String sql = "SELECT 1 FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}

