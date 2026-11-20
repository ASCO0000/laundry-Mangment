package com.laundry.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standard date and time formatters for display and parsing.
 */
public class DateUtil {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
    public static final DateTimeFormatter RECEIPT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE) : "-";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DISPLAY_DATETIME) : "-";
    }

    public static String formatReceiptDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(RECEIPT_DATETIME) : "-";
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
