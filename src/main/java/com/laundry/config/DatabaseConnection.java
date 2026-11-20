package com.laundry.config;

import com.laundry.exception.DatabaseException;
import com.laundry.util.PasswordUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages JDBC connections and automated database initialization.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static volatile boolean initialized = false;

    /**
     * Obtains an active JDBC Connection using configured settings.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DatabaseConfig.getDriver());
            return DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
            );
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC Driver not found: " + DatabaseConfig.getDriver(), e);
        } catch (SQLException e) {
            if (!DatabaseConfig.isH2Active() && DatabaseConfig.isFallbackH2Allowed()) {
                LOGGER.log(Level.WARNING, "Unable to connect to MySQL server ({0}). Falling back to embedded H2 database for demonstration/testing.", e.getMessage());
                DatabaseConfig.activateH2Fallback();
                try {
                    Class.forName(DatabaseConfig.getDriver());
                    return DriverManager.getConnection(
                            DatabaseConfig.getUrl(),
                            DatabaseConfig.getUsername(),
                            DatabaseConfig.getPassword()
                    );
                } catch (Exception ex) {
                    throw new DatabaseException("Failed connecting to fallback database", ex);
                }
            }
            throw e;
        }
    }

    /**
     * Initializes tables and default seeds if not already created.
     */
    public static synchronized void initializeDatabase() {
        if (initialized) return;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            LOGGER.info("Verifying database schema tables...");

            // 1. Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(50) NOT NULL UNIQUE, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "full_name VARCHAR(100) NOT NULL, "
                    + "role VARCHAR(20) NOT NULL DEFAULT 'STAFF', "
                    + "active BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            // 2. Customers table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers ("
                    + "customer_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "full_name VARCHAR(100) NOT NULL, "
                    + "phone VARCHAR(20) NOT NULL UNIQUE, "
                    + "email VARCHAR(100) NULL, "
                    + "address TEXT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            // 3. Services table
            stmt.execute("CREATE TABLE IF NOT EXISTS services ("
                    + "service_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "service_name VARCHAR(100) NOT NULL UNIQUE, "
                    + "description TEXT NULL, "
                    + "pricing_unit VARCHAR(30) NOT NULL DEFAULT 'Per Item', "
                    + "price DECIMAL(10,2) NOT NULL, "
                    + "active BOOLEAN NOT NULL DEFAULT TRUE"
                    + ")");

            // 4. Orders table
            stmt.execute("CREATE TABLE IF NOT EXISTS orders ("
                    + "order_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "customer_id INT NOT NULL, "
                    + "order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "expected_delivery_date DATE NOT NULL, "
                    + "status VARCHAR(30) NOT NULL DEFAULT 'Received', "
                    + "subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00, "
                    + "discount DECIMAL(10,2) NOT NULL DEFAULT 0.00, "
                    + "total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00, "
                    + "special_instructions TEXT NULL, "
                    + "created_by INT NOT NULL, "
                    + "FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT, "
                    + "FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE RESTRICT"
                    + ")");

            // 5. Order Items table
            stmt.execute("CREATE TABLE IF NOT EXISTS order_items ("
                    + "order_item_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "order_id INT NOT NULL, "
                    + "service_id INT NOT NULL, "
                    + "item_description VARCHAR(200) NOT NULL, "
                    + "quantity DECIMAL(8,2) NOT NULL, "
                    + "unit_price DECIMAL(10,2) NOT NULL, "
                    + "subtotal DECIMAL(10,2) NOT NULL, "
                    + "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (service_id) REFERENCES services(service_id) ON DELETE RESTRICT"
                    + ")");

            // 6. Payments table
            stmt.execute("CREATE TABLE IF NOT EXISTS payments ("
                    + "payment_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "order_id INT NOT NULL, "
                    + "payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "amount DECIMAL(10,2) NOT NULL, "
                    + "payment_method VARCHAR(20) NOT NULL, "
                    + "transaction_reference VARCHAR(100) NULL, "
                    + "status VARCHAR(30) NOT NULL DEFAULT 'Completed', "
                    + "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE"
                    + ")");

            // 7. Deliveries table
            stmt.execute("CREATE TABLE IF NOT EXISTS deliveries ("
                    + "delivery_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "order_id INT NOT NULL, "
                    + "delivery_type VARCHAR(20) NOT NULL, "
                    + "address TEXT NOT NULL, "
                    + "scheduled_datetime DATETIME NOT NULL, "
                    + "assigned_staff_id INT NULL, "
                    + "status VARCHAR(30) NOT NULL DEFAULT 'Scheduled', "
                    + "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (assigned_staff_id) REFERENCES users(user_id) ON DELETE SET NULL"
                    + ")");

            // 8. Order Status History table
            stmt.execute("CREATE TABLE IF NOT EXISTS order_status_history ("
                    + "history_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "order_id INT NOT NULL, "
                    + "old_status VARCHAR(30) NULL, "
                    + "new_status VARCHAR(30) NOT NULL, "
                    + "changed_by INT NULL, "
                    + "changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "remarks TEXT NULL, "
                    + "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (changed_by) REFERENCES users(user_id) ON DELETE SET NULL"
                    + ")");

            // 9. Customer Tickets table (Complaints, Missing Garments, Refund Requests)
            stmt.execute("CREATE TABLE IF NOT EXISTS customer_tickets ("
                    + "ticket_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "ticket_number VARCHAR(30) NOT NULL UNIQUE, "
                    + "ticket_type VARCHAR(30) NOT NULL, " // Complaint, Missing_Item, Refund_Request
                    + "order_id INT NULL, "
                    + "customer_id INT NULL, "
                    + "customer_name VARCHAR(100) NOT NULL, "
                    + "customer_phone VARCHAR(20) NOT NULL, "
                    + "category VARCHAR(60) NOT NULL, "
                    + "subject VARCHAR(200) NOT NULL, "
                    + "description TEXT NOT NULL, "
                    + "item_details TEXT NULL, "
                    + "amount_claimed DECIMAL(10,2) DEFAULT 0.00, "
                    + "payout_details VARCHAR(255) NULL, "
                    + "status VARCHAR(30) NOT NULL DEFAULT 'Open', " // Open, In Review, Resolved, Rejected
                    + "priority VARCHAR(20) NOT NULL DEFAULT 'Medium', "
                    + "admin_notes TEXT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL, "
                    + "FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL"
                    + ")");

            seedDefaultServices(conn);
            seedInitialAdmin(conn);
            seedSampleDataIfEmpty(conn);

            initialized = true;
            LOGGER.info("Database initialized successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database initialization failed: " + e.getMessage(), e);
            throw new DatabaseException("Failed to initialize database tables: " + e.getMessage(), e);
        }
    }

    private static void seedDefaultServices(Connection conn) throws SQLException {
        Object[][] defaultServices = {
            {"Washing", "Normal washing for everyday clothes.", "Per Kg", 40.00, true},
            {"Ironing", "Ironing shirts, trousers, uniforms, and other garments.", "Per Item", 15.00, true},
            {"Dry Cleaning", "Specialized cleaning for suits, blazers, coats, and delicate garments.", "Per Item", 180.00, true},
            {"Drying", "Drying clothes using a suitable dryer.", "Per Kg", 30.00, true},
            {"Wash, Dry and Fold", "Complete washing, drying, and folding service.", "Per Kg", 60.00, true},
            {"Bedsheet and Blanket Cleaning", "Cleaning bedsheets, blankets, curtains, and comforters.", "Per Item", 150.00, true},
            {"Steam Pressing", "Professional steam pressing for formal and delicate garments.", "Per Item", 35.00, true},
            {"Pickup and Delivery", "Collecting clothes from customers and returning completed orders.", "Fixed Fee", 50.00, true}
        };

        String checkSql = "SELECT COUNT(*) FROM services WHERE service_name = ?";
        String insertSql = "INSERT INTO services (service_name, description, pricing_unit, price, active) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql);
             PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
            for (Object[] s : defaultServices) {
                checkPs.setString(1, (String) s[0]);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        insertPs.setString(1, (String) s[0]);
                        insertPs.setString(2, (String) s[1]);
                        insertPs.setString(3, (String) s[2]);
                        insertPs.setBigDecimal(4, new java.math.BigDecimal(s[3].toString()));
                        insertPs.setBoolean(5, (Boolean) s[4]);
                        insertPs.executeUpdate();
                    }
                }
            }
        }
        LOGGER.info("Default laundry services verified.");
    }

    private static void seedInitialAdmin(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                String sql = "INSERT INTO users (username, password_hash, full_name, role, active) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "admin");
                    ps.setString(2, PasswordUtil.hashPassword("admin123"));
                    ps.setString(3, "System Administrator");
                    ps.setString(4, "ADMIN");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();

                    ps.setString(1, "staff1");
                    ps.setString(2, PasswordUtil.hashPassword("staff123"));
                    ps.setString(3, "Staff Assistant");
                    ps.setString(4, "STAFF");
                    ps.setBoolean(5, true);
                    ps.executeUpdate();
                }
                LOGGER.info("Default initial Admin (admin/admin123) and Staff (staff1/staff123) created.");
            }
        }
    }

    private static void seedSampleDataIfEmpty(Connection conn) {
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM customers")) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert 3 sample customers
                String custSql = "INSERT INTO customers (full_name, phone, email, address) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(custSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "Alice Johnson");
                    ps.setString(2, "9876543210");
                    ps.setString(3, "alice.j@example.com");
                    ps.setString(4, "12 Rosewood Apartments, Main Street");
                    ps.executeUpdate();

                    ps.setString(1, "Robert Smith");
                    ps.setString(2, "9876543211");
                    ps.setString(3, "robert.s@example.com");
                    ps.setString(4, "45 Greenfield Boulevard, Suite 3B");
                    ps.executeUpdate();

                    ps.setString(1, "Priya Sharma");
                    ps.setString(2, "9876543212");
                    ps.setString(3, "priya.s@example.com");
                    ps.setString(4, "78 Sunshine Heights, 4th Cross");
                    ps.executeUpdate();
                }
                LOGGER.info("Sample customers seeded.");
            }

            // Seed sample tickets if empty
            try (Statement checkTicket = conn.createStatement();
                 ResultSet trs = checkTicket.executeQuery("SELECT COUNT(*) FROM customer_tickets")) {
                if (trs.next() && trs.getInt(1) == 0) {
                    String ticketSql = "INSERT INTO customer_tickets (ticket_number, ticket_type, order_id, customer_name, customer_phone, category, subject, description, item_details, amount_claimed, payout_details, status, priority, admin_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement tps = conn.prepareStatement(ticketSql)) {
                        // Ticket 1: Complaint
                        tps.setString(1, "CMP-1001");
                        tps.setString(2, "Complaint");
                        tps.setObject(3, 1);
                        tps.setString(4, "Alice Johnson");
                        tps.setString(5, "9876543210");
                        tps.setString(6, "Poor Wash Quality");
                        tps.setString(7, "Collar stains not cleaned on white shirt");
                        tps.setString(8, "Received the laundry today and noticed light yellow discoloration on collar.");
                        tps.setString(9, null);
                        tps.setBigDecimal(10, java.math.BigDecimal.ZERO);
                        tps.setString(11, null);
                        tps.setString(12, "In Review");
                        tps.setString(13, "High");
                        tps.setString(14, "Assigned to head washer for re-cleaning with gentle enzyme detergent.");
                        tps.executeUpdate();

                        // Ticket 2: Missing Garment
                        tps.setString(1, "MIS-1002");
                        tps.setString(2, "Missing_Item");
                        tps.setObject(3, 2);
                        tps.setString(4, "Robert Smith");
                        tps.setString(5, "9876543211");
                        tps.setString(6, "Missing Garment");
                        tps.setString(7, "Blue striped formal shirt missing");
                        tps.setString(8, "Sent 5 shirts in order ORD-102, only 4 were returned in packet.");
                        tps.setString(9, "1 Piece - Blue Striped Formal Shirt (Raymond brand, Size 42)");
                        tps.setBigDecimal(10, new java.math.BigDecimal("1200.00"));
                        tps.setString(11, null);
                        tps.setString(12, "Open");
                        tps.setString(13, "Urgent");
                        tps.setString(14, null);
                        tps.executeUpdate();

                        // Ticket 3: Refund Request
                        tps.setString(1, "REF-1003");
                        tps.setString(2, "Refund_Request");
                        tps.setObject(3, null);
                        tps.setString(4, "Priya Sharma");
                        tps.setString(5, "9876543212");
                        tps.setString(6, "Damaged Garment");
                        tps.setString(7, "Compensation for torn dupatta hem");
                        tps.setString(8, "Hemline of georgette dupatta torn during high-speed cycle.");
                        tps.setString(9, "1 Piece - Peach Georgette Dupatta");
                        tps.setBigDecimal(10, new java.math.BigDecimal("350.00"));
                        tps.setString(11, "priya.s@okicici");
                        tps.setString(12, "Resolved");
                        tps.setString(13, "Medium");
                        tps.setString(14, "Refund of ₹350 approved and processed to UPI priya.s@okicici (Ref #UPI998822)");
                        tps.executeUpdate();
                    }
                    LOGGER.info("Sample customer tickets seeded.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Sample data check skipped: {0}", e.getMessage());
        }
    }
}
