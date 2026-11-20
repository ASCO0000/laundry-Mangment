package com.laundry.dao;

import com.laundry.config.DatabaseConnection;
import com.laundry.exception.DatabaseException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data aggregation operations for dashboard metrics, KPI counters, and analytical reports.
 */
public class ReportDAO {

    /**
     * Aggregates real-time high-level KPIs for the Dashboard Cards.
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Defaults
        metrics.put("totalCustomers", 0);
        metrics.put("totalOrders", 0);
        metrics.put("pendingOrders", 0);
        metrics.put("completedOrders", 0);
        metrics.put("todayRevenue", BigDecimal.ZERO);
        metrics.put("totalRevenue", BigDecimal.ZERO);
        metrics.put("outstandingPayments", BigDecimal.ZERO);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Total Customers
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM customers")) {
                if (rs.next()) metrics.put("totalCustomers", rs.getInt(1));
            }

            // 2. Total Orders
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {
                if (rs.next()) metrics.put("totalOrders", rs.getInt(1));
            }

            // 3. Pending Orders (Not Delivered and Not Cancelled)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders WHERE status NOT IN ('Delivered', 'Cancelled')")) {
                if (rs.next()) metrics.put("pendingOrders", rs.getInt(1));
            }

            // 4. Completed Orders
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders WHERE status = 'Delivered'")) {
                if (rs.next()) metrics.put("completedOrders", rs.getInt(1));
            }

            // 5. Today's Revenue (Actual successful payments collected today)
            try (ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE DATE(payment_date) = CURRENT_DATE AND status = 'Completed'")) {
                if (rs.next()) metrics.put("todayRevenue", rs.getBigDecimal(1));
            }

            // 6. Total Revenue collected to date
            try (ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'Completed'")) {
                if (rs.next()) metrics.put("totalRevenue", rs.getBigDecimal(1));
            }

            // 7. Outstanding Payments (Total of orders excluding cancelled minus all completed payments)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT (SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status != 'Cancelled') - "
                  + "(SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'Completed')")) {
                if (rs.next()) {
                    BigDecimal outstanding = rs.getBigDecimal(1);
                    metrics.put("outstandingPayments", outstanding != null ? outstanding.max(BigDecimal.ZERO) : BigDecimal.ZERO);
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed loading dashboard metrics: " + e.getMessage(), e);
        }

        return metrics;
    }

    /**
     * Aggregates revenue collected by month (Year-Month) for JFreeChart trend plotting.
     */
    public Map<String, BigDecimal> getMonthlyRevenueTrend() {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(payment_date, '%b %Y') AS month_label, "
                   + "COALESCE(SUM(amount), 0) AS revenue "
                   + "FROM payments WHERE status = 'Completed' "
                   + "GROUP BY DATE_FORMAT(payment_date, '%Y-%m'), DATE_FORMAT(payment_date, '%b %Y') "
                   + "ORDER BY DATE_FORMAT(payment_date, '%Y-%m') ASC LIMIT 12";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                trend.put(rs.getString("month_label"), rs.getBigDecimal("revenue"));
            }
        } catch (SQLException e) {
            // H2 fallback syntax adaptation if DATE_FORMAT is not recognized
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT FORMATDATETIME(payment_date, 'MMM yyyy') AS month_label, "
                         + "COALESCE(SUM(amount), 0) AS revenue FROM payments WHERE status = 'Completed' "
                         + "GROUP BY FORMATDATETIME(payment_date, 'yyyy-MM'), FORMATDATETIME(payment_date, 'MMM yyyy') "
                         + "ORDER BY FORMATDATETIME(payment_date, 'yyyy-MM') ASC LIMIT 12")) {
                while (rs.next()) {
                    trend.put(rs.getString("month_label"), rs.getBigDecimal("revenue"));
                }
            } catch (Exception ex) {
                // If query fails, provide default empty trend
            }
        }
        return trend;
    }

    /**
     * Returns distribution counts by order status.
     */
    public Map<String, Integer> getOrderStatusCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS count FROM orders GROUP BY status ORDER BY count DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed loading order status distribution", e);
        }
        return counts;
    }

    /**
     * Returns top services by total quantity and revenue.
     */
    public List<Map<String, Object>> getPopularServices() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT s.service_name, SUM(oi.quantity) AS total_qty, SUM(oi.subtotal) AS total_sales "
                   + "FROM order_items oi "
                   + "JOIN services s ON oi.service_id = s.service_id "
                   + "GROUP BY s.service_id, s.service_name "
                   + "ORDER BY total_sales DESC LIMIT 5";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("serviceName", rs.getString("service_name"));
                item.put("totalQty", rs.getBigDecimal("total_qty"));
                item.put("totalSales", rs.getBigDecimal("total_sales"));
                list.add(item);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed querying popular services", e);
        }
        return list;
    }

    /**
     * Queries payments between date ranges for financial reporting.
     */
    public List<Map<String, Object>> getPaymentCollectionReport(LocalDate start, LocalDate end) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.payment_date, p.order_id, c.full_name AS customer_name, "
                   + "p.amount, p.payment_method, p.transaction_reference, p.status "
                   + "FROM payments p "
                   + "JOIN orders o ON p.order_id = o.order_id "
                   + "JOIN customers c ON o.customer_id = c.customer_id "
                   + "WHERE DATE(p.payment_date) BETWEEN ? AND ? "
                   + "ORDER BY p.payment_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("paymentId", rs.getInt("payment_id"));
                    row.put("paymentDate", rs.getTimestamp("payment_date").toLocalDateTime());
                    row.put("orderId", rs.getInt("order_id"));
                    row.put("customerName", rs.getString("customer_name"));
                    row.put("amount", rs.getBigDecimal("amount"));
                    row.put("paymentMethod", rs.getString("payment_method"));
                    row.put("reference", rs.getString("transaction_reference"));
                    row.put("status", rs.getString("status"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed loading payment collection report", e);
        }
        return list;
    }
}
