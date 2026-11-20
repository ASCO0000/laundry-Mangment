package com.laundry.service;

import com.laundry.dao.CustomerDAO;
import com.laundry.dao.OrderDAO;
import com.laundry.dao.OrderStatusHistoryDAO;
import com.laundry.exception.ValidationException;
import com.laundry.model.Customer;
import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import com.laundry.model.OrderStatusHistory;
import com.laundry.model.enums.OrderStatus;
import com.laundry.util.ValidationUtil;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service managing order creation, calculation, lifecycle state transitions, and history.
 */
public class OrderService {

    private final OrderDAO orderDAO;
    private final CustomerDAO customerDAO;
    private final OrderStatusHistoryDAO historyDAO;

    public OrderService() {
        this.orderDAO = new OrderDAO();
        this.customerDAO = new CustomerDAO();
        this.historyDAO = new OrderStatusHistoryDAO();
    }

    public OrderService(OrderDAO orderDAO, CustomerDAO customerDAO, OrderStatusHistoryDAO historyDAO) {
        this.orderDAO = orderDAO;
        this.customerDAO = customerDAO;
        this.historyDAO = historyDAO;
    }

    /**
     * Creates and persists a customer order with items and calculations.
     */
    public Order createOrder(Order order) {
        validateOrder(order);
        order.recalculateTotals();
        return orderDAO.saveOrderWithItems(order);
    }

    public Order getOrderById(int orderId) {
        return orderDAO.findById(orderId);
    }

    public List<Order> getAllOrders(String statusFilter, String searchKeyword) {
        return orderDAO.findAll(statusFilter, searchKeyword);
    }

    public List<OrderStatusHistory> getOrderStatusHistory(int orderId) {
        return historyDAO.findByOrderId(orderId);
    }

    public List<Order> getDelayedOrders() {
        return orderDAO.findDelayedOrders();
    }

    /**
     * Updates order progress and validates allowed transitions.
     */
    public void updateOrderStatus(int orderId, OrderStatus newStatus, int changedByUserId, String remarks) {
        Order current = orderDAO.findById(orderId);
        if (current == null) {
            throw new ValidationException("Order not found with ID: " + orderId);
        }

        if (!current.getStatus().canTransitionTo(newStatus)) {
            throw new ValidationException("Invalid status transition: Cannot move order from '" 
                    + current.getStatus().getDisplayName() + "' to '" + newStatus.getDisplayName() + "'.");
        }

        orderDAO.updateStatus(orderId, newStatus, changedByUserId, remarks);
    }

    private void validateOrder(Order order) {
        if (order == null) {
            throw new ValidationException("Order cannot be null.");
        }
        if (order.getCustomerId() <= 0) {
            throw new ValidationException("An existing customer must be selected for the order.");
        }
        Customer customer = customerDAO.findById(order.getCustomerId());
        if (customer == null) {
            throw new ValidationException("Selected customer does not exist.");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ValidationException("At least one laundry item or service must be added to the order.");
        }

        for (OrderItem item : order.getItems()) {
            ValidationUtil.requireNonEmpty(item.getItemDescription(), "Item description");
            ValidationUtil.validatePositiveQuantity(item.getQuantity(), "Item quantity");
            ValidationUtil.validatePositivePrice(item.getUnitPrice(), "Item unit rate");
        }

        ValidationUtil.validateDeliveryDate(order.getExpectedDeliveryDate());

        if (order.getDiscount() != null) {
            ValidationUtil.validateNonNegativeAmount(order.getDiscount(), "Discount amount");
        }
    }
}
