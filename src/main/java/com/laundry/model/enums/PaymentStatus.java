package com.laundry.model.enums;

/**
 * Payment statuses for order transactions.
 */
public enum PaymentStatus {
    COMPLETED("Completed"),
    PENDING("Pending"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentStatus fromString(String str) {
        if (str == null) return COMPLETED;
        for (PaymentStatus s : values()) {
            if (s.name().equalsIgnoreCase(str.trim()) ||
                s.displayName.equalsIgnoreCase(str.trim())) {
                return s;
            }
        }
        return COMPLETED;
    }
}
