package com.laundry.util;

import com.laundry.exception.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validates input parameters across forms and API boundaries.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    // Matches 10 to 15 digit telephone numbers, allowing optional leading +
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$"
    );

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required and cannot be empty.");
        }
    }

    public static void validatePhone(String phone) {
        requireNonEmpty(phone, "Phone number");
        String sanitized = phone.replaceAll("[\\s-()]", "");
        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            throw new ValidationException("Invalid phone number format: '" + phone + "'. Must be 10-15 digits.");
        }
    }

    public static void validateEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                throw new ValidationException("Invalid email address format: '" + email + "'.");
            }
        }
    }

    public static void validatePositivePrice(BigDecimal price, String fieldName) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void validateNonNegativeAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static void validatePositiveQuantity(BigDecimal quantity, String fieldName) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void validateDeliveryDate(LocalDate expectedDate) {
        if (expectedDate == null) {
            throw new ValidationException("Expected delivery date is required.");
        }
        if (expectedDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Expected delivery date cannot be in the past.");
        }
    }

    public static void validatePasswordStrength(String password) {
        requireNonEmpty(password, "Password");
        if (password.length() < 6) {
            throw new ValidationException("Password must contain at least 6 characters.");
        }
    }
}
