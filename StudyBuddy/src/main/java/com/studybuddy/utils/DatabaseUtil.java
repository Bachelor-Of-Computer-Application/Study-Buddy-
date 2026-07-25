package com.studybuddy.utils;

import com.studybuddy.dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseUtil {

    private static final Map<String, Boolean> TABLE_EXISTENCE_CACHE = new ConcurrentHashMap<>();

    public static Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    /**
     * Returns true when {@code tableName} exists in the current SQL Server database.
     * Uses {@code OBJECT_ID} first, then {@code INFORMATION_SCHEMA.TABLES} as fallback.
     * Results are cached for the lifetime of the JVM (schema is assumed stable at runtime).
     */
    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        if (tableName == null || tableName.isBlank()) {
            return false;
        }
        String normalized = tableName.trim();
        String cacheKey = normalized.toLowerCase(Locale.ROOT);
        Boolean cached = TABLE_EXISTENCE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean exists = false;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CASE WHEN OBJECT_ID(?, 'U') IS NOT NULL THEN 1 ELSE 0 END")) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exists = rs.getInt(1) == 1;
                }
            }
        }

        if (!exists) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
                ps.setString(1, normalized);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }
                }
            }
        }

        TABLE_EXISTENCE_CACHE.put(cacheKey, exists);
        return exists;
    }

    /** Clears cached table-existence lookups (for tests or after migrations). */
    public static void clearTableExistenceCache() {
        TABLE_EXISTENCE_CACHE.clear();
    }
}
