package com.laundry;

import com.laundry.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordUtilTest {

    @Test
    @DisplayName("Should hash password securely and verify matching plaintext")
    public void testPasswordHashingAndVerification() {
        String plain = "SecretStaff123!";
        String hash = PasswordUtil.hashPassword(plain);

        assertNotNull(hash);
        assertNotEquals(plain, hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));

        assertTrue(PasswordUtil.checkPassword(plain, hash));
        assertFalse(PasswordUtil.checkPassword("WrongPassword", hash));
        assertFalse(PasswordUtil.checkPassword("", hash));
    }

    @Test
    @DisplayName("Should produce different salt hashes for identical passwords")
    public void testSaltUniqueness() {
        String plain = "same_password";
        String hash1 = PasswordUtil.hashPassword(plain);
        String hash2 = PasswordUtil.hashPassword(plain);

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtil.checkPassword(plain, hash1));
        assertTrue(PasswordUtil.checkPassword(plain, hash2));
    }

    @Test
    @DisplayName("Should throw exception when hashing null or empty password")
    public void testEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
    }
}
