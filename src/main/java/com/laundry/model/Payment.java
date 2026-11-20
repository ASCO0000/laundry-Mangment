package com.laundry.model;

import com.laundry.model.enums.PaymentMethod;
import com.laundry.model.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment transaction record for an order.
 */
public class Payment {
    private int paymentId;
    private int orderId;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String transactionReference;
    private PaymentStatus status;

    public Payment() {
        this.paymentDate = LocalDateTime.now();
        this.paymentMethod = PaymentMethod.CASH;
        this.status = PaymentStatus.COMPLETED;
        this.amount = BigDecimal.ZERO;
    }

    public Payment(int paymentId, int orderId, LocalDateTime paymentDate, BigDecimal amount,
                   PaymentMethod paymentMethod, String transactionReference, PaymentStatus status) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.status = status;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
