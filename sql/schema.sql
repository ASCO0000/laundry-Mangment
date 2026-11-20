-- ==============================================================
-- Laundry Management System - Database Schema
-- Database: laundry_management
-- Compatible with MySQL 8.0+ and MariaDB
-- ==============================================================

CREATE DATABASE IF NOT EXISTS laundry_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE laundry_management;

-- --------------------------------------------------------------
-- 1. Table: users
-- Stores administrative and staff credentials with BCrypt hashes
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 2. Table: customers
-- Registered customer profiles and contact details
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NULL,
    address TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cust_phone (phone),
    INDEX idx_cust_name (full_name)
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 3. Table: services
-- Available laundry services, pricing units, and unit rates
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS services (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL,
    pricing_unit VARCHAR(30) NOT NULL DEFAULT 'Per Item', -- 'Per Item' or 'Per Kg'
    price DECIMAL(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_service_active (active)
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 4. Table: orders
-- Customer laundry orders header records
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_delivery_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Received',
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    special_instructions TEXT NULL,
    created_by INT NOT NULL,
    INDEX idx_order_customer (customer_id),
    INDEX idx_order_status (status),
    INDEX idx_order_date (order_date),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) 
        REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_orders_user FOREIGN KEY (created_by) 
        REFERENCES users(user_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 5. Table: order_items
-- Line items for each order, preserving historic unit price
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    service_id INT NOT NULL,
    item_description VARCHAR(200) NOT NULL,
    quantity DECIMAL(8, 2) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    INDEX idx_item_order (order_id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_items_service FOREIGN KEY (service_id) 
        REFERENCES services(service_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 6. Table: payments
-- Payment receipts and balance tracking
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('Cash', 'UPI', 'Card') NOT NULL,
    transaction_reference VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Completed',
    INDEX idx_payment_order (order_id),
    INDEX idx_payment_date (payment_date),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 7. Table: deliveries
-- Scheduled customer pickups and deliveries
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    delivery_type ENUM('Pickup', 'Delivery') NOT NULL,
    address TEXT NOT NULL,
    scheduled_datetime DATETIME NOT NULL,
    assigned_staff_id INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Scheduled',
    INDEX idx_delivery_order (order_id),
    INDEX idx_delivery_status (status),
    INDEX idx_delivery_datetime (scheduled_datetime),
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_deliveries_staff FOREIGN KEY (assigned_staff_id) 
        REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 8. Table: order_status_history
-- Full audit trail of order progress changes
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    changed_by INT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT NULL,
    INDEX idx_history_order (order_id),
    CONSTRAINT fk_history_order FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) 
        REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------------
-- 9. Table: customer_tickets
-- Complaints, missing garments, and refund claims management
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_tickets (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(30) NOT NULL UNIQUE,
    ticket_type VARCHAR(30) NOT NULL, -- 'Complaint', 'Missing_Item', 'Refund_Request'
    order_id INT NULL,
    customer_id INT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    category VARCHAR(60) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    item_details TEXT NULL,
    amount_claimed DECIMAL(10, 2) DEFAULT 0.00,
    payout_details VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'Open', -- 'Open', 'In Review', 'Resolved', 'Rejected'
    priority VARCHAR(20) NOT NULL DEFAULT 'Medium',
    admin_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ticket_num (ticket_number),
    INDEX idx_ticket_phone (customer_phone),
    INDEX idx_ticket_type (ticket_type),
    INDEX idx_ticket_status (status),
    CONSTRAINT fk_ticket_order FOREIGN KEY (order_id)
        REFERENCES orders(order_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ticket_cust FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

