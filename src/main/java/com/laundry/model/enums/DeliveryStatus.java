package com.laundry.model.enums;

import java.awt.Color;

/**
 * Status of transport scheduling for pickup and delivery.
 */
public enum DeliveryStatus {
    SCHEDULED("Scheduled", new Color(0x3B, 0x82, 0xF6), new Color(0xDB, 0xEA, 0xFE)),
    ASSIGNED("Assigned", new Color(0x63, 0x66, 0xF1), new Color(0xE0, 0xE7, 0xFF)),
    PICKED_UP("Picked Up", new Color(0x06, 0xB6, 0xD4), new Color(0xCF, 0xFA, 0xFE)),
    IN_TRANSIT("In Transit", new Color(0xF5, 0x9E, 0x0B), new Color(0xFE, 0xFC, 0xBF)),
    DELIVERED("Delivered", new Color(0x10, 0xB9, 0x81), new Color(0xD1, 0xFA, 0xE5)),
    FAILED("Failed", new Color(0xEF, 0x44, 0x44), new Color(0xFE, 0xE2, 0xE2));

    private final String displayName;
    private final Color textColor;
    private final Color bgColor;

    DeliveryStatus(String displayName, Color textColor, Color bgColor) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.bgColor = bgColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getBgColor() {
        return bgColor;
    }

    public static DeliveryStatus fromString(String str) {
        if (str == null) return SCHEDULED;
        for (DeliveryStatus s : values()) {
            if (s.name().equalsIgnoreCase(str.replace(" ", "_")) ||
                s.displayName.equalsIgnoreCase(str.trim())) {
                return s;
            }
        }
        return SCHEDULED;
    }
}
