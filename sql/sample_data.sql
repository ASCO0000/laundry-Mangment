-- ==============================================================
-- Laundry Management System - Initial & Sample Seed Data
-- Database: laundry_management
-- ==============================================================

USE laundry_management;

-- --------------------------------------------------------------
-- Initial Admin and Staff Users
-- Default passwords:
--   admin  -> admin123 (Role: ADMIN)
--   staff1 -> staff123 (Role: STAFF)
-- Hashes generated via standard BCrypt (rounds: 10)
-- --------------------------------------------------------------
INSERT INTO users (user_id, username, password_hash, full_name, role, active) VALUES
(1, 'admin', '$2a$10$Z3k3x34rYhUq8Vp/lUqYIeb2o6x9mYv5dE2mJ8jD4k0F6bE8i8L5C', 'System Administrator', 'ADMIN', TRUE),
(2, 'staff1', '$2a$10$f6b3n61tZ9LhP2m5w1a0EuU3r8v1k5s9d2m4j8d1k6f8b2e1i5l7G', 'John Assistant', 'STAFF', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- --------------------------------------------------------------
-- Default Laundry Services
-- --------------------------------------------------------------
INSERT INTO services (service_id, service_name, description, pricing_unit, price, active) VALUES
(1, 'Washing', 'Normal washing for everyday clothes.', 'Per Kg', 40.00, TRUE),
(2, 'Ironing', 'Ironing shirts, trousers, uniforms, and other garments.', 'Per Item', 15.00, TRUE),
(3, 'Dry Cleaning', 'Specialized cleaning for suits, blazers, coats, and delicate garments.', 'Per Item', 180.00, TRUE),
(4, 'Drying', 'Drying clothes using a suitable dryer.', 'Per Kg', 30.00, TRUE),
(5, 'Wash, Dry and Fold', 'Complete washing, drying, and folding service.', 'Per Kg', 60.00, TRUE),
(6, 'Bedsheet and Blanket Cleaning', 'Cleaning bedsheets, blankets, curtains, and comforters.', 'Per Item', 150.00, TRUE),
(7, 'Steam Pressing', 'Professional steam pressing for formal and delicate garments.', 'Per Item', 35.00, TRUE),
(8, 'Pickup and Delivery', 'Collecting clothes from customers and returning completed orders.', 'Fixed Fee', 50.00, TRUE)
ON DUPLICATE KEY UPDATE service_name = VALUES(service_name);

-- --------------------------------------------------------------
-- Sample Customers
-- --------------------------------------------------------------
INSERT INTO customers (customer_id, full_name, phone, email, address, created_at) VALUES
(1, 'Alice Johnson', '9876543210', 'alice.j@example.com', '12 Rosewood Apartments, Main Street', '2026-09-01 10:00:00'),
(2, 'Robert Smith', '9876543211', 'robert.smith@example.com', '45 Greenfield Boulevard, Suite 3B', '2026-09-05 11:30:00'),
(3, 'Priya Sharma', '9876543212', 'priya.sharma@example.com', '78 Sunshine Heights, 4th Cross', '2026-09-10 14:15:00'),
(4, 'David Miller', '9876543213', 'david.m@example.com', '210 Lakeview Residency, Sector 5', '2026-09-15 09:45:00')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- --------------------------------------------------------------
-- Sample Orders
-- --------------------------------------------------------------
INSERT INTO orders (order_id, customer_id, order_date, expected_delivery_date, status, subtotal, discount, total_amount, special_instructions, created_by) VALUES
(101, 1, '2026-10-10 10:30:00', '2026-10-12', 'Delivered', 210.00, 10.00, 200.00, 'Use fragrance-free detergent for shirts', 1),
(102, 2, '2026-10-14 12:00:00', '2026-10-16', 'Ready for Pickup', 360.00, 0.00, 360.00, 'Sharp creases on formal trousers', 2),
(103, 3, '2026-10-17 15:45:00', '2026-10-19', 'Ironing', 150.00, 0.00, 150.00, 'Hang the silk dress, do not fold', 1),
(104, 4, '2026-10-18 09:15:00', '2026-10-20', 'Received', 240.00, 20.00, 220.00, 'Handle wool sweater with care', 2)
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount);

-- --------------------------------------------------------------
-- Sample Order Items
-- --------------------------------------------------------------
INSERT INTO order_items (order_item_id, order_id, service_id, item_description, quantity, unit_price, subtotal) VALUES
(1, 101, 1, 'Daily Wear Clothes (5 Kg)', 5.00, 30.00, 150.00),
(2, 101, 3, 'Office Cotton Shirts (4 pcs)', 4.00, 15.00, 60.00),
(3, 102, 4, '2-Piece Business Suit (3 sets)', 3.00, 120.00, 360.00),
(4, 103, 7, 'Heavy Woolen Blanket (1 pc)', 1.00, 150.00, 150.00),
(5, 104, 4, 'Formal Blazer (2 pcs)', 2.00, 120.00, 240.00)
ON DUPLICATE KEY UPDATE subtotal = VALUES(subtotal);

-- --------------------------------------------------------------
-- Sample Payments
-- --------------------------------------------------------------
INSERT INTO payments (payment_id, order_id, payment_date, amount, payment_method, transaction_reference, status) VALUES
(1, 101, '2026-10-10 10:35:00', 200.00, 'UPI', 'UPI/20261010/884920', 'Completed'),
(2, 102, '2026-10-14 12:05:00', 200.00, 'Cash', 'CASH-REC-102A', 'Completed'),
(3, 103, '2026-10-17 15:50:00', 150.00, 'Card', 'TXN-CARD-44910', 'Completed')
ON DUPLICATE KEY UPDATE amount = VALUES(amount);

-- --------------------------------------------------------------
-- Sample Deliveries
-- --------------------------------------------------------------
INSERT INTO deliveries (delivery_id, order_id, delivery_type, address, scheduled_datetime, assigned_staff_id, status) VALUES
(1, 101, 'Delivery', '12 Rosewood Apartments, Main Street', '2026-10-12 17:00:00', 2, 'Delivered'),
(2, 102, 'Delivery', '45 Greenfield Boulevard, Suite 3B', '2026-10-16 18:00:00', 2, 'Scheduled'),
(3, 104, 'Pickup', '210 Lakeview Residency, Sector 5', '2026-10-18 10:00:00', 2, 'Picked Up')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- --------------------------------------------------------------
-- Sample Order Status History
-- --------------------------------------------------------------
INSERT INTO order_status_history (history_id, order_id, old_status, new_status, changed_by, changed_at, remarks) VALUES
(1, 101, NULL, 'Received', 1, '2026-10-10 10:30:00', 'Order booked and weighted'),
(2, 101, 'Received', 'Washing', 2, '2026-10-10 14:00:00', 'Moved to wash cycle 2'),
(3, 101, 'Washing', 'Drying', 2, '2026-10-11 09:00:00', 'Tumble drying started'),
(4, 101, 'Drying', 'Ironing', 2, '2026-10-11 15:30:00', 'Steam pressed'),
(5, 101, 'Ironing', 'Ready for Pickup', 1, '2026-10-12 11:00:00', 'Packed and ready'),
(6, 101, 'Ready for Pickup', 'Delivered', 2, '2026-10-12 17:30:00', 'Delivered to customer address'),
(7, 102, NULL, 'Received', 2, '2026-10-14 12:00:00', 'Suits received for dry cleaning'),
(8, 102, 'Received', 'Ready for Pickup', 2, '2026-10-16 14:00:00', 'Dry cleaning completed'),
(9, 103, NULL, 'Received', 1, '2026-10-17 15:45:00', 'Blanket received'),
(10, 103, 'Received', 'Ironing', 1, '2026-10-18 11:00:00', 'Washing complete, in steam finishing'),
(11, 104, NULL, 'Received', 2, '2026-10-18 09:15:00', 'Picked up from customer')
ON DUPLICATE KEY UPDATE new_status = VALUES(new_status);
