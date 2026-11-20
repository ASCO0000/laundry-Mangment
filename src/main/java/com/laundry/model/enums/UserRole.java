package com.laundry.model.enums;

/**
 * User roles in the Laundry Management System.
 */
public enum UserRole {
    ADMIN("Administrator"),
    STAFF("Staff Member");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromString(String roleStr) {
        if (roleStr == null) return STAFF;
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(roleStr.trim()) || 
                role.displayName.equalsIgnoreCase(roleStr.trim())) {
                return role;
            }
        }
        return STAFF;
    }
}
