package com.laundry.service;

import com.laundry.dao.CustomerDAO;
import com.laundry.dao.OrderDAO;
import com.laundry.dao.PaymentDAO;
import com.laundry.exception.ValidationException;
import com.laundry.model.Customer;
import com.laundry.model.Order;
import com.laundry.model.Payment;
import com.laundry.model.enums.PaymentMethod;
import com.laundry.model.enums.PaymentStatus;
import com.laundry.util.ReceiptUtil;
import com.laundry.util.ValidationUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing billing, balance calculations, payments, and receipts.
 */
public class BillingService {

    private final OrderDAO orderDAO;
    private final CustomerDAO customerDAO;
    private final PaymentDAO paymentDAO;

    public BillingService() {
        this.orderDAO = new OrderDAO();
        this.customerDAO = new CustomerDAO();
        this.paymentDAO = new PaymentDAO();
    }

    public BillingService(OrderDAO orderDAO, CustomerDAO customerDAO, PaymentDAO paymentDAO) {
        this.orderDAO = orderDAO;
        this.customerDAO = customerDAO;
        this.paymentDAO = paymentDAO;
    }

    public Order getOrderBillingDetails(int orderId) {
        Order order = orderDAO.findById(orderId);
        if (order != null) {
            BigDecimal totalPaid = paymentDAO.getTotalPaidForOrder(orderId);
            order.setPaidAmount(totalPaid);
            order.setBalanceDue(order.getTotalAmount().subtract(totalPaid).max(BigDecimal.ZERO));
        }
        return order;
    }

    /**
     * Records a payment against an order, preventing negative or overpayments.
     */
    public Payment recordPayment(int orderId, BigDecimal amount, PaymentMethod method, String reference) {
        Order order = getOrderBillingDetails(orderId);
        if (order == null) {
            throw new ValidationException("Order not found with ID: " + orderId);
        }

        ValidationUtil.validatePositivePrice(amount, "Payment amount");

        BigDecimal balanceDue = order.getBalanceDue();
        if (amount.compareTo(balanceDue) > 0) {
            throw new ValidationException(String.format(
                    "Payment amount (₹%s) cannot exceed the outstanding balance (₹%s).",
                    amount, balanceDue));
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod(method != null ? method : PaymentMethod.CASH);
        payment.setTransactionReference(reference);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.COMPLETED);

        return paymentDAO.save(payment);
    }

    public List<Payment> getPaymentsForOrder(int orderId) {
        return paymentDAO.findByOrderId(orderId);
    }

    public List<Payment> getAllPayments() {
        return paymentDAO.findAllPayments();
    }

    public String generateReceiptText(int orderId, Payment payment) {
        Order order = getOrderBillingDetails(orderId);
        if (order == null) return "Order not found.";
        Customer customer = customerDAO.findById(order.getCustomerId());
        return ReceiptUtil.generateTextReceipt(order, customer, payment);
    }

    public String generateReceiptHtml(int orderId, Payment payment) {
        Order order = getOrderBillingDetails(orderId);
        if (order == null) return "<html><body>Order not found.</body></html>";
        Customer customer = customerDAO.findById(order.getCustomerId());
        return ReceiptUtil.generateHtmlReceipt(order, customer, payment);
    }
}
