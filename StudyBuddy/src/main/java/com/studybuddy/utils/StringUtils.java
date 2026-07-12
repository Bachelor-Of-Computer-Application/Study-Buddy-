package com.studybuddy.utils;

/**
 * Utility class for common string operations.
 * Provides helper methods to handle null-safe string operations.
 */
public class StringUtils {

    /**
     * Returns the string if not null, otherwise returns an empty string.
     * This is a null-safe helper to avoid NullPointerException when working with strings.
     *
     * @param s the string to check
     * @return the string if not null, otherwise an empty string
     */
    public static String nullSafe(String s) {
        return s != null ? s : "";
    }

    /**
     * Returns the string if not null and not blank, otherwise returns the fallback.
     *
     * @param s the string to check
     * @param fallback the fallback string to return if s is null or blank
     * @return the string if not null and not blank, otherwise the fallback
     */
    public static String nullSafeOr(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    /**
     * Truncates a string to a maximum length, appending "..." if truncated.
     * Null-safe.
     *
     * @param s the string to truncate
     * @param maxLength the maximum length
     * @return the truncated string or original if within limit
     */
    public static String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength) + "...";
    }

    /**
     * Capitalizes the first letter of a string.
     * Null-safe.
     *
     * @param s the string to capitalize
     * @return the capitalized string
     */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
