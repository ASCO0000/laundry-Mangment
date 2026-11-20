package com.laundry.util;

import com.laundry.model.Customer;
import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import com.laundry.model.Payment;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

/**
 * Generates formatted text and HTML receipts for customer orders.
 */
public class ReceiptUtil {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("₹#,##0.00");

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Builds a clean, professional monospaced text receipt suitable for printing or preview.
     */
    public static String generateTextReceipt(Order order, Customer customer, Payment payment) {
        StringBuilder sb = new StringBuilder();
        String line = "==========================================================\n";
        String dash = "----------------------------------------------------------\n";

        sb.append(line);
        sb.append("                  BLUE WAVE LAUNDRY CARE                  \n");
        sb.append("         Premium Garment Care & Express Services          \n");
        sb.append("       Phone: (555) 019-2834 | support@bluewavelaundry.com \n");
        sb.append(line);

        sb.append(String.format("Receipt Ref   : REC-%06d\n", payment != null ? payment.getPaymentId() : order.getOrderId()));
        sb.append(String.format("Date & Time   : %s\n", DateUtil.formatReceiptDateTime(payment != null ? payment.getPaymentDate() : LocalDateTime.now())));
        sb.append(String.format("Order Number  : ORD-%05d\n", order.getOrderId()));
        sb.append(String.format("Order Status  : %s\n", order.getStatus().getDisplayName()));
        sb.append(dash);

        sb.append("CUSTOMER DETAILS:\n");
        if (customer != null) {
            sb.append(String.format("  Name   : %s (ID: #%d)\n", customer.getFullName(), customer.getCustomerId()));
            sb.append(String.format("  Phone  : %s\n", customer.getPhone()));
            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                sb.append(String.format("  Address: %s\n", customer.getAddress()));
            }
        } else {
            sb.append("  Walk-in Customer\n");
        }
        sb.append(dash);

        sb.append(String.format("%-22s %6s %11s %14s\n", "Item / Service", "Qty", "Rate", "Subtotal"));
        sb.append(dash);

        for (OrderItem item : order.getItems()) {
            String desc = item.getItemDescription();
            if (desc.length() > 22) {
                desc = desc.substring(0, 19) + "...";
            }
            sb.append(String.format("%-22s %6.1f %11s %14s\n",
                    desc,
                    item.getQuantity().doubleValue(),
                    formatCurrency(item.getUnitPrice()),
                    formatCurrency(item.getSubtotal())));
        }
        sb.append(dash);

        sb.append(String.format("%-38s : %15s\n", "Subtotal", formatCurrency(order.getSubtotal())));
        if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("%-38s : %15s\n", "Discount Applied", "-" + formatCurrency(order.getDiscount())));
        }
        sb.append(String.format("%-38s : %15s\n", "Total Order Amount", formatCurrency(order.getTotalAmount())));
        sb.append(line);

        BigDecimal paid = order.getPaidAmount();
        BigDecimal balance = order.getBalanceDue();

        if (payment != null) {
            sb.append(String.format("%-38s : %15s\n", "Payment Now (" + payment.getPaymentMethod().getDisplayName() + ")", formatCurrency(payment.getAmount())));
            if (payment.getTransactionReference() != null && !payment.getTransactionReference().isEmpty()) {
                sb.append(String.format("%-38s : %15s\n", "Transaction Ref", payment.getTransactionReference()));
            }
        }
        sb.append(String.format("%-38s : %15s\n", "Total Amount Paid", formatCurrency(paid)));
        sb.append(String.format("%-38s : %15s\n", "Outstanding Balance Due", formatCurrency(balance)));
        sb.append(line);

        if (order.getExpectedDeliveryDate() != null) {
            sb.append(String.format("Expected Ready / Delivery Date: %s\n", DateUtil.formatDate(order.getExpectedDeliveryDate())));
        }
        if (order.getSpecialInstructions() != null && !order.getSpecialInstructions().isEmpty()) {
            sb.append(String.format("Instructions: %s\n", order.getSpecialInstructions()));
        }

        sb.append("\n");
        sb.append("         Thank you for choosing Blue Wave Laundry!        \n");
        sb.append("   Please present this receipt when collecting garments.  \n");
        sb.append(line);

        return sb.toString();
    }

    /**
     * Builds an HTML receipt for formatted display in a JEditorPane or printing.
     */
    public static String generateHtmlReceipt(Order order, Customer customer, Payment payment) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #1B2A4A; padding: 15px;'>");
        sb.append("<div style='text-align: center; border-bottom: 2px solid #1B2A4A; padding-bottom: 8px;'>");
        sb.append("<h2 style='margin: 0; color: #1B2A4A;'>BLUE WAVE LAUNDRY CARE</h2>");
        sb.append("<p style='margin: 2px; font-size: 11px; color: #555;'>Premium Fabric Cleaning & Express Pressing</p>");
        sb.append("<p style='margin: 2px; font-size: 11px; color: #555;'>Phone: (555) 019-2834 | support@bluewavelaundry.com</p>");
        sb.append("</div>");

        sb.append("<div style='margin-top: 10px; font-size: 12px;'>");
        sb.append("<table width='100%'>");
        sb.append("<tr><td><b>Order ID:</b> ORD-").append(order.getOrderId()).append("</td>");
        sb.append("<td align='right'><b>Date:</b> ").append(DateUtil.formatReceiptDateTime(payment != null ? payment.getPaymentDate() : LocalDateTime.now())).append("</td></tr>");
        sb.append("<tr><td><b>Customer:</b> ").append(customer != null ? customer.getFullName() : "Walk-in").append("</td>");
        sb.append("<td align='right'><b>Status:</b> ").append(order.getStatus().getDisplayName()).append("</td></tr>");
        if (customer != null && customer.getPhone() != null) {
            sb.append("<tr><td><b>Phone:</b> ").append(customer.getPhone()).append("</td>");
            sb.append("<td align='right'><b>Expected:</b> ").append(DateUtil.formatDate(order.getExpectedDeliveryDate())).append("</td></tr>");
        }
        sb.append("</table></div>");

        sb.append("<table width='100%' style='border-collapse: collapse; margin-top: 12px; font-size: 12px;'>");
        sb.append("<tr style='background-color: #E2E8F0; text-align: left;'>");
        sb.append("<th style='padding: 6px; border-bottom: 1px solid #CBD5E1;'>Service / Description</th>");
        sb.append("<th style='padding: 6px; border-bottom: 1px solid #CBD5E1; text-align: center;'>Qty</th>");
        sb.append("<th style='padding: 6px; border-bottom: 1px solid #CBD5E1; text-align: right;'>Rate</th>");
        sb.append("<th style='padding: 6px; border-bottom: 1px solid #CBD5E1; text-align: right;'>Total</th></tr>");

        for (OrderItem item : order.getItems()) {
            sb.append("<tr>");
            sb.append("<td style='padding: 5px; border-bottom: 1px solid #F1F5F9;'>").append(item.getItemDescription()).append("</td>");
            sb.append("<td style='padding: 5px; border-bottom: 1px solid #F1F5F9; text-align: center;'>").append(item.getQuantity()).append("</td>");
            sb.append("<td style='padding: 5px; border-bottom: 1px solid #F1F5F9; text-align: right;'>").append(formatCurrency(item.getUnitPrice())).append("</td>");
            sb.append("<td style='padding: 5px; border-bottom: 1px solid #F1F5F9; text-align: right;'>").append(formatCurrency(item.getSubtotal())).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("<div style='margin-top: 10px; font-size: 12px; border-top: 1px solid #1B2A4A; padding-top: 5px;'>");
        sb.append("<table width='100%'>");
        sb.append("<tr><td>Subtotal:</td><td align='right'>").append(formatCurrency(order.getSubtotal())).append("</td></tr>");
        if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<tr><td style='color: #059669;'>Discount:</td><td align='right' style='color: #059669;'>-").append(formatCurrency(order.getDiscount())).append("</td></tr>");
        }
        sb.append("<tr style='font-weight: bold; font-size: 13px;'><td>Total Amount:</td><td align='right'>").append(formatCurrency(order.getTotalAmount())).append("</td></tr>");
        sb.append("<tr><td>Amount Paid:</td><td align='right'>").append(formatCurrency(order.getPaidAmount())).append("</td></tr>");
        sb.append("<tr style='font-weight: bold; color: #DC2626;'><td>Balance Due:</td><td align='right'>").append(formatCurrency(order.getBalanceDue())).append("</td></tr>");
        if (payment != null) {
            sb.append("<tr><td>Payment Method:</td><td align='right'>").append(payment.getPaymentMethod().getDisplayName()).append("</td></tr>");
        }
        sb.append("</table></div>");

        sb.append("<div style='text-align: center; margin-top: 15px; font-size: 11px; color: #64748B;'>");
        sb.append("<p>Thank you for your business!<br>Please retain this slip for claiming your garments.</p>");
        sb.append("</div></body></html>");
        return sb.toString();
    }
}
