package com.laundry.model.enums;

/**
 * Supported payment methods for billing and receipts.
 */
public enum PaymentMethod {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethod fromString(String str) {
        if (str == null) return CASH;
        for (PaymentMethod m : values()) {
            if (m.name().equalsIgnoreCase(str.trim()) || 
                m.displayName.equalsIgnoreCase(str.trim())) {
                return m;
            }
        }
        return CASH;
    }
}
