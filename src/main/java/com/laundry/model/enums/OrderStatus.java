package com.laundry.model.enums;

import java.awt.Color;

/**
 * Standard life cycle statuses for laundry orders.
 */
public enum OrderStatus {
    RECEIVED("Received", new Color(0x3B, 0x82, 0xF6), new Color(0xDB, 0xEA, 0xFE)),       // Blue
    WASHING("Washing", new Color(0x06, 0xB6, 0xD4), new Color(0xCF, 0xFA, 0xFE)),         // Cyan
    DRYING("Drying", new Color(0x8B, 0x5C, 0xF6), new Color(0xED, 0xE9, 0xFE)),           // Purple
    IRONING("Ironing", new Color(0xF5, 0x9E, 0x0B), new Color(0xFE, 0xFC, 0xBF)),          // Amber/Gold
    READY_FOR_PICKUP("Ready for Pickup", new Color(0x10, 0xB9, 0x81), new Color(0xD1, 0xFA, 0xE5)), // Emerald
    DELIVERED("Delivered", new Color(0x05, 0x96, 0x69), new Color(0xA7, 0xF3, 0xD0)),      // Green
    CANCELLED("Cancelled", new Color(0xEF, 0x44, 0x44), new Color(0xFE, 0xE2, 0xE2));      // Red

    private final String displayName;
    private final Color badgeTextColor;
    private final Color badgeBackgroundColor;

    OrderStatus(String displayName, Color badgeTextColor, Color badgeBackgroundColor) {
        this.displayName = displayName;
        this.badgeTextColor = badgeTextColor;
        this.badgeBackgroundColor = badgeBackgroundColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getBadgeTextColor() {
        return badgeTextColor;
    }

    public Color getBadgeBackgroundColor() {
        return badgeBackgroundColor;
    }

    public static OrderStatus fromString(String statusStr) {
        if (statusStr == null) return RECEIVED;
        for (OrderStatus s : values()) {
            if (s.name().equalsIgnoreCase(statusStr.replace(" ", "_")) ||
                s.displayName.equalsIgnoreCase(statusStr.trim())) {
                return s;
            }
        }
        return RECEIVED;
    }

    /**
     * Checks if transitioning from this status to targetStatus is valid.
     * Prevents invalid backwards transitions, e.g. from Delivered or Cancelled.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) return true;
        if (this == DELIVERED || this == CANCELLED) {
            return false; // Terminal states
        }
        if (target == CANCELLED) {
            return true; // Can cancel from any active state before delivered
        }
        // Ordered workflow
        return target.ordinal() >= this.ordinal();
    }
}
