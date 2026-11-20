package com.laundry.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model representing a customer support ticket, missing garment dispute,
 * or monetary refund claim.
 */
public class CustomerTicket {

    private int ticketId;
    private String ticketNumber; // e.g. CMP-1001, MIS-1002, REF-1003
    private String ticketType;   // "Complaint", "Missing_Item", "Refund_Request"
    private Integer orderId;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String category;     // e.g. "Poor Wash Quality", "Missing Garment", "Damaged Fabric", etc.
    private String subject;
    private String description;
    private String itemDetails;  // e.g. "2 White Linen Shirts - Allen Solly"
    private BigDecimal amountClaimed; // In Indian Rupees (₹)
    private String payoutDetails;     // UPI ID or bank account details
    private String status;       // "Open", "In Review", "Resolved", "Rejected"
    private String priority;     // "Low", "Medium", "High", "Urgent"
    private String adminNotes;   // Resolution remarks or transaction reference
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerTicket() {
        this.status = "Open";
        this.priority = "Medium";
        this.amountClaimed = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public CustomerTicket(String ticketType, Integer orderId, String customerName, String customerPhone,
                          String category, String subject, String description) {
        this();
        this.ticketType = ticketType;
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.category = category;
        this.subject = subject;
        this.description = description;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItemDetails() {
        return itemDetails;
    }

    public void setItemDetails(String itemDetails) {
        this.itemDetails = itemDetails;
    }

    public BigDecimal getAmountClaimed() {
        return amountClaimed != null ? amountClaimed : BigDecimal.ZERO;
    }

    public void setAmountClaimed(BigDecimal amountClaimed) {
        this.amountClaimed = amountClaimed;
    }

    public String getPayoutDetails() {
        return payoutDetails;
    }

    public void setPayoutDetails(String payoutDetails) {
        this.payoutDetails = payoutDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTypeIcon() {
        if ("Missing_Item".equalsIgnoreCase(ticketType)) return "🔍";
        if ("Refund_Request".equalsIgnoreCase(ticketType)) return "💸";
        return "📝";
    }

    public String getStatusBadgeClass() {
        if ("Resolved".equalsIgnoreCase(status)) return "badge-success";
        if ("In Review".equalsIgnoreCase(status)) return "badge-info";
        if ("Rejected".equalsIgnoreCase(status)) return "badge-danger";
        return "badge-warning";
    }
}
