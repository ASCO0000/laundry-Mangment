package com.laundry;

import com.laundry.exception.ValidationException;
import com.laundry.util.ValidationUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidationUtilTest {

    @Test
    @DisplayName("Should pass valid required non-empty string")
    public void testRequireNonEmpty_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.requireNonEmpty("John Doe", "Full Name"));
    }

    @Test
    @DisplayName("Should throw for null or empty required string")
    public void testRequireNonEmpty_Invalid() {
        assertThrows(ValidationException.class, () -> ValidationUtil.requireNonEmpty("", "Full Name"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireNonEmpty(null, "Full Name"));
        assertThrows(ValidationException.class, () -> ValidationUtil.requireNonEmpty("   ", "Full Name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "+15551234567", "98765-43210", "+91 98765 43210"})
    @DisplayName("Should accept valid phone formats")
    public void testPhoneValidation_Valid(String phone) {
        assertDoesNotThrow(() -> ValidationUtil.validatePhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc1234567", "phone-number", ""})
    @DisplayName("Should reject invalid phone formats")
    public void testPhoneValidation_Invalid(String phone) {
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePhone(phone));
    }

    @Test
    @DisplayName("Should validate email formats")
    public void testEmailValidation() {
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("test@example.com"));
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("john.doe+service@domain.co.in"));
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("")); // Optional email allowed

        assertThrows(ValidationException.class, () -> ValidationUtil.validateEmail("invalid-email"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validateEmail("user@.com"));
    }

    @Test
    @DisplayName("Should validate positive prices and non-negative discounts")
    public void testMonetaryValidation() {
        assertDoesNotThrow(() -> ValidationUtil.validatePositivePrice(new BigDecimal("15.50"), "Price"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositivePrice(BigDecimal.ZERO, "Price"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositivePrice(new BigDecimal("-5.00"), "Price"));

        assertDoesNotThrow(() -> ValidationUtil.validateNonNegativeAmount(BigDecimal.ZERO, "Discount"));
        assertDoesNotThrow(() -> ValidationUtil.validateNonNegativeAmount(new BigDecimal("10.00"), "Discount"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validateNonNegativeAmount(new BigDecimal("-1.00"), "Discount"));
    }

    @Test
    @DisplayName("Should reject delivery dates in the past")
    public void testDeliveryDateValidation() {
        assertDoesNotThrow(() -> ValidationUtil.validateDeliveryDate(LocalDate.now()));
        assertDoesNotThrow(() -> ValidationUtil.validateDeliveryDate(LocalDate.now().plusDays(2)));
        assertThrows(ValidationException.class, () -> ValidationUtil.validateDeliveryDate(LocalDate.now().minusDays(1)));
    }
}
