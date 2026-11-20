package com.laundry;

import com.laundry.model.Order;
import com.laundry.model.OrderItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderCalculationTest {

    @Test
    @DisplayName("Should accurately calculate OrderItem subtotal (quantity * unit price)")
    public void testOrderItemSubtotal() {
        OrderItem item = new OrderItem();
        item.setQuantity(new BigDecimal("3.5")); // 3.5 Kg
        item.setUnitPrice(new BigDecimal("30.00")); // $30/Kg
        item.calculateSubtotal();

        assertEquals(new BigDecimal("105.000"), item.getSubtotal());
    }

    @Test
    @DisplayName("Should sum multiple items, apply discount, and calculate balance due accurately")
    public void testOrderTotalCalculation() {
        Order order = new Order();

        OrderItem item1 = new OrderItem();
        item1.setQuantity(new BigDecimal("2"));
        item1.setUnitPrice(new BigDecimal("50.00"));
        item1.calculateSubtotal(); // 100.00

        OrderItem item2 = new OrderItem();
        item2.setQuantity(new BigDecimal("4"));
        item2.setUnitPrice(new BigDecimal("15.00"));
        item2.calculateSubtotal(); // 60.00

        order.addItem(item1);
        order.addItem(item2);
        order.setDiscount(new BigDecimal("20.00"));

        order.recalculateTotals();

        assertEquals(new BigDecimal("160.00"), order.getSubtotal());
        assertEquals(new BigDecimal("140.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("140.00"), order.getBalanceDue());

        // Simulate partial payment
        order.setPaidAmount(new BigDecimal("50.00"));
        order.recalculateTotals();
        assertEquals(new BigDecimal("90.00"), order.getBalanceDue());
    }

    @Test
    @DisplayName("Should not allow total amount to become negative if discount exceeds subtotal")
    public void testExcessiveDiscountFlooring() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal("25.00"));
        item.calculateSubtotal();

        order.addItem(item);
        order.setDiscount(new BigDecimal("50.00")); // Exceeds 25.00 subtotal
        order.recalculateTotals();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }
}
