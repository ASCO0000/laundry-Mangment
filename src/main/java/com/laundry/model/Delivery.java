package com.laundry.model;

import com.laundry.model.enums.DeliveryStatus;
import com.laundry.model.enums.DeliveryType;
import java.time.LocalDateTime;

/**
 * Transport schedule for laundry pickup or delivery.
 */
public class Delivery {
    private int deliveryId;
    private int orderId;
    private String customerName; // Helpful display property
    private String customerPhone;
    private DeliveryType deliveryType;
    private String address;
    private LocalDateTime scheduledDatetime;
    private Integer assignedStaffId;
    private String assignedStaffName;
    private DeliveryStatus status;

    public Delivery() {
        this.deliveryType = DeliveryType.DELIVERY;
        this.status = DeliveryStatus.SCHEDULED;
        this.scheduledDatetime = LocalDateTime.now().plusDays(1);
    }

    public Delivery(int deliveryId, int orderId, DeliveryType deliveryType, String address,
                    LocalDateTime scheduledDatetime, Integer assignedStaffId, DeliveryStatus status) {
        this.deliveryId = deliveryId;
        this.orderId = orderId;
        this.deliveryType = deliveryType;
        this.address = address;
        this.scheduledDatetime = scheduledDatetime;
        this.assignedStaffId = assignedStaffId;
        this.status = status;
    }

    public int getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(int deliveryId) {
        this.deliveryId = deliveryId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
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

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getScheduledDatetime() {
        return scheduledDatetime;
    }

    public void setScheduledDatetime(LocalDateTime scheduledDatetime) {
        this.scheduledDatetime = scheduledDatetime;
    }

    public Integer getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(Integer assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public String getAssignedStaffName() {
        return assignedStaffName;
    }

    public void setAssignedStaffName(String assignedStaffName) {
        this.assignedStaffName = assignedStaffName;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }
}
