package com.laundry.model;

import java.math.BigDecimal;

/**
 * Line item within a laundry order.
 */
public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int serviceId;
    private String serviceName; // Denormalized for display convenience
    private String itemDescription;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public OrderItem() {
        this.quantity = BigDecimal.ONE;
        this.unitPrice = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    public OrderItem(int orderItemId, int orderId, int serviceId, String serviceName, 
                     String itemDescription, BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void calculateSubtotal() {
        if (this.quantity != null && this.unitPrice != null) {
            this.subtotal = this.quantity.multiply(this.unitPrice);
        }
    }
}
