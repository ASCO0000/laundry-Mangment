package com.laundry.model.enums;

/**
 * Type of transport task: Pickup from customer or Delivery to customer.
 */
public enum DeliveryType {
    PICKUP("Pickup"),
    DELIVERY("Delivery");

    private final String displayName;

    DeliveryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DeliveryType fromString(String str) {
        if (str == null) return DELIVERY;
        for (DeliveryType t : values()) {
            if (t.name().equalsIgnoreCase(str.trim()) ||
                t.displayName.equalsIgnoreCase(str.trim())) {
                return t;
            }
        }
        return DELIVERY;
    }
}
