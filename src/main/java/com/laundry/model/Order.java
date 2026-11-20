package com.laundry.model;

import com.laundry.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Laundry order header containing status, customer reference, items, and totals.
 */
public class Order {
    private int orderId;
    private int customerId;
    private String customerName; // Denormalized for convenient table rendering
    private String customerPhone;
    private LocalDateTime orderDate;
    private LocalDate expectedDeliveryDate;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String specialInstructions;
    private int createdBy;
    private String createdByName;
    private List<OrderItem> items;

    // Derived payment details
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;

    public Order() {
        this.status = OrderStatus.RECEIVED;
        this.subtotal = BigDecimal.ZERO;
        this.discount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.balanceDue = BigDecimal.ZERO;
        this.items = new ArrayList<>();
        this.orderDate = LocalDateTime.now();
        this.expectedDeliveryDate = LocalDate.now().plusDays(2);
    }

    public Order(int orderId, int customerId, LocalDateTime orderDate, LocalDate expectedDeliveryDate,
                 OrderStatus status, BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount,
                 String specialInstructions, int createdBy) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.status = status;
        this.subtotal = subtotal;
        this.discount = discount;
        this.totalAmount = totalAmount;
        this.specialInstructions = specialInstructions;
        this.createdBy = createdBy;
        this.items = new ArrayList<>();
        this.paidAmount = BigDecimal.ZERO;
        this.balanceDue = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    public BigDecimal getPaidAmount() {
        return paidAmount != null ? paidAmount : BigDecimal.ZERO;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue != null ? balanceDue : BigDecimal.ZERO;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
    }

    /**
     * Recalculates subtotal from items and applies discount to calculate totalAmount.
     */
    public void recalculateTotals() {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (item.getSubtotal() != null) {
                calculatedSubtotal = calculatedSubtotal.add(item.getSubtotal());
            }
        }
        this.subtotal = calculatedSubtotal;
        BigDecimal appliedDiscount = discount != null ? discount : BigDecimal.ZERO;
        this.totalAmount = calculatedSubtotal.subtract(appliedDiscount).max(BigDecimal.ZERO);
        if (this.paidAmount != null) {
            this.balanceDue = this.totalAmount.subtract(this.paidAmount).max(BigDecimal.ZERO);
        } else {
            this.balanceDue = this.totalAmount;
        }
    }
}
