package com.laundry.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Security utility for hashing and verifying passwords using BCrypt.
 * Never stores or compares plain-text credentials.
 */
public class PasswordUtil {

    private static final int LOG_ROUNDS = 10;

    /**
     * Hashes a plain-text password using salt and standard BCrypt work factor.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Checks if a plain-text password matches a stored BCrypt hash.
     */
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            // Malformed hash or unsupported salt version
            return false;
        }
    }
}
