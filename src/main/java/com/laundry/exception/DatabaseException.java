package com.laundry.exception;

/**
 * Thrown when persistence or SQL queries encounter an error.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
