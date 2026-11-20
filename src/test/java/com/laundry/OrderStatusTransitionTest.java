package com.laundry;

import com.laundry.model.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderStatusTransitionTest {

    @Test
    @DisplayName("Should allow valid forward lifecycle transitions")
    public void testValidForwardTransitions() {
        assertTrue(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.WASHING));
        assertTrue(OrderStatus.WASHING.canTransitionTo(OrderStatus.DRYING));
        assertTrue(OrderStatus.DRYING.canTransitionTo(OrderStatus.IRONING));
        assertTrue(OrderStatus.IRONING.canTransitionTo(OrderStatus.READY_FOR_PICKUP));
        assertTrue(OrderStatus.READY_FOR_PICKUP.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Should prevent delivered or cancelled orders from moving backwards")
    public void testTerminalStateTransitions() {
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.WASHING));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.RECEIVED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.WASHING));
    }

    @Test
    @DisplayName("Should allow cancellation from early active states")
    public void testCancellationAllowed() {
        assertTrue(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.WASHING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.IRONING.canTransitionTo(OrderStatus.CANCELLED));
    }
}
