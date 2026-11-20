package com.laundry.model;

import java.time.LocalDateTime;

/**
 * Historical log tracking order status progression.
 */
public class OrderStatusHistory {
    private int historyId;
    private int orderId;
    private String oldStatus;
    private String newStatus;
    private Integer changedBy;
    private String changedByName;
    private LocalDateTime changedAt;
    private String remarks;

    public OrderStatusHistory() {
        this.changedAt = LocalDateTime.now();
    }

    public OrderStatusHistory(int historyId, int orderId, String oldStatus, String newStatus,
                              Integer changedBy, LocalDateTime changedAt, String remarks) {
        this.historyId = historyId;
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.remarks = remarks;
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
